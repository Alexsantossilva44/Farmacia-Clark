package br.com.farmacia.application.sngpc.usecase;

import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.domain.receituario.entity.RegistroSNGPC;
import br.com.farmacia.domain.receituario.enums.StatusEnvio;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import br.com.farmacia.domain.receituario.repository.RegistroSNGPCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caso de Uso: Registrar dispensação no SNGPC.
 *
 * <p>Persiste o registro com status PENDENTE e delega o envio
 * ao {@link SngpcEventPublisher} (implementado na infra com RabbitMQ).</p>
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnviarRegistroSNGPCUseCase {

    private final RegistroSNGPCRepository registroRepository;
    private final ReceitaRepository       receitaRepository;
    private final MedicamentoRepository   medicamentoRepository;
    private final LoteRepository          loteRepository;
    private final SngpcEventPublisher     sngpcEventPublisher;

    public record Input(
        UUID   receitaId,
        UUID   medicamentoId,
        UUID   loteId,
        String compradorNome,
        String compradorCpf,
        int    quantidade
    ) {}

    @Transactional
    public void publicarNaFila(Input input) {
        RegistroSNGPC registro = RegistroSNGPC.builder()
            .receita(receitaRepository.getReferenceById(input.receitaId()))
            .medicamento(medicamentoRepository.getReferenceById(input.medicamentoId()))
            .lote(loteRepository.getReferenceById(input.loteId()))
            .compradorNome(input.compradorNome())
            .compradorCpf(input.compradorCpf())
            .quantidade(input.quantidade())
            .statusEnvio(StatusEnvio.PENDENTE)
            .dataRegistro(LocalDateTime.now())
            .tentativasEnvio(0)
            .build();

        RegistroSNGPC salvo = registroRepository.save(registro);

        // H-12: publica na fila somente após commit — evita mensagem órfã se a transação fizer rollback
        UUID idSalvo = salvo.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sngpcEventPublisher.publicar(idSalvo, input);
                log.info("Registro SNGPC [{}] publicado na fila após commit.", idSalvo);
            }
        });
    }
}
