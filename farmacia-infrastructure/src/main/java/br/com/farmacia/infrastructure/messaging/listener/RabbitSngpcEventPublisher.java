package br.com.farmacia.infrastructure.messaging.listener;

import br.com.farmacia.application.sngpc.usecase.EnviarRegistroSNGPCUseCase;
import br.com.farmacia.application.sngpc.usecase.SNGPCGateway;
import br.com.farmacia.application.sngpc.usecase.SngpcEventPublisher;
import br.com.farmacia.domain.receituario.entity.RegistroSNGPC;
import br.com.farmacia.domain.receituario.enums.StatusEnvio;
import br.com.farmacia.domain.receituario.repository.RegistroSNGPCRepository;
import br.com.farmacia.infrastructure.messaging.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador RabbitMQ para publicação e consumo de eventos SNGPC.
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitSngpcEventPublisher implements SngpcEventPublisher {

    private final RabbitTemplate             rabbitTemplate;
    private final RegistroSNGPCRepository    registroRepository;
    private final SNGPCGateway               sngpcGateway; // C-01: porta para o gateway real do SNGPC/ANVISA

    private static final int MAX_TENTATIVAS = 5;

    // ── Publicar na fila ──────────────────────────────────────────────────────

    @Override
    public void publicar(UUID registroId, EnviarRegistroSNGPCUseCase.Input dados) {
        SngpcMessage msg = new SngpcMessage(registroId, dados);
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.SNGPC_EXCHANGE,
            RabbitMQConfig.SNGPC_ROUTING_KEY,
            msg
        );
        log.info("Mensagem SNGPC publicada na fila para registro [{}]", registroId);
    }

    // ── Consumir fila ─────────────────────────────────────────────────────────

    @RabbitListener(queues = RabbitMQConfig.SNGPC_QUEUE, ackMode = "MANUAL") // H-13: ACK manual evita perda silenciosa de mensagens
    @Transactional
    public void processarMensagem(SngpcMessage msg,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        log.info("Processando SNGPC [{}]", msg.registroId());

        RegistroSNGPC registro = registroRepository.findById(msg.registroId())
            .orElseThrow(() -> new RuntimeException("Registro SNGPC não encontrado: " + msg.registroId()));

        if (registro.getStatusEnvio() == StatusEnvio.CONFIRMADO) {
            log.info("Registro [{}] já confirmado. Ignorando.", msg.registroId());
            ackSilently(channel, tag);
            return;
        }

        registro.iniciarEnvio();

        try {
            // C-01: chama o gateway real do SNGPC/ANVISA em vez de gerar protocolo fictício
            SNGPCGateway.Response response = sngpcGateway.enviar(
                SNGPCGateway.Request.builder()
                    .receita(registro.getReceita())
                    .medicamento(registro.getMedicamento())
                    .lote(registro.getLote())
                    .compradorNome(registro.getCompradorNome())
                    .compradorCpf(registro.getCompradorCpf())
                    .quantidade(registro.getQuantidade())
                    .build()
            );
            registro.confirmarEnvio(response.protocolo()); // protocolo real devolvido pelo gateway
            log.info("Registro SNGPC [{}] confirmado. Protocolo: {}", registro.getId(), response.protocolo());
            registroRepository.save(registro);
            ackSilently(channel, tag); // mensagem processada com sucesso
        } catch (Exception e) {
            if (registro.getTentativasEnvio() >= MAX_TENTATIVAS) {
                registro.marcarErroDefinitivo();
                log.error("Registro [{}] excedeu {} tentativas.", registro.getId(), MAX_TENTATIVAS);
                registroRepository.save(registro);
                ackSilently(channel, tag); // descarta após N falhas; alertas devem monitorar ERRO_DEFINITIVO
            } else {
                registro.reagendarEnvio();
                log.warn("Falha SNGPC [{}]: {}. Será reenviado.", registro.getId(), e.getMessage());
                registroRepository.save(registro);
                nackSilently(channel, tag); // devolve à fila para reprocessamento
            }
        }
    }

    private void ackSilently(Channel channel, long tag) {
        try { channel.basicAck(tag, false); }
        catch (IOException e) { log.error("Erro ao confirmar ACK SNGPC tag={}", tag, e); }
    }

    private void nackSilently(Channel channel, long tag) {
        try { channel.basicNack(tag, false, true); }
        catch (IOException e) { log.error("Erro ao enviar NACK SNGPC tag={}", tag, e); }
    }

    // ── Retry scheduler ───────────────────────────────────────────────────────

    @Scheduled(cron = "0 */30 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void reprocessarPendentes() {
        List<RegistroSNGPC> pendentes = registroRepository
            .findPendentesParaReprocessamento(
                LocalDateTime.now().minusMinutes(10), MAX_TENTATIVAS);

        if (pendentes.isEmpty()) return;

        log.info("[Scheduler SNGPC] {} registro(s) para reprocessamento.", pendentes.size());
        pendentes.forEach(r -> {
            r.iniciarEnvio(); // M-07: sinaliza status ENVIANDO antes de publicar para evitar duplo processamento
            registroRepository.save(r);
            publicar(r.getId(),
                new EnviarRegistroSNGPCUseCase.Input(
                    r.getReceita().getId(),
                    r.getMedicamento().getId(),
                    r.getLote().getId(),
                    r.getCompradorNome(),
                    r.getCompradorCpf(),
                    r.getQuantidade()
                ));
        });
    }

    public record SngpcMessage(UUID registroId, EnviarRegistroSNGPCUseCase.Input dados) {}
}
