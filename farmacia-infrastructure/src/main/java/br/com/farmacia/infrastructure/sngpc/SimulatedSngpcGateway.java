package br.com.farmacia.infrastructure.sngpc;

import br.com.farmacia.application.sngpc.usecase.SNGPCGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simula o envio ao SNGPC/ANVISA em desenvolvimento e testes.
 *
 * <p>Substitui a geração fictícia de protocolo que existia no consumer RabbitMQ antes
 * da extração da porta {@link SNGPCGateway}. Em produção, registrar um bean real
 * (integração ANVISA) com {@code @Profile("prod")}.</p>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class SimulatedSngpcGateway implements SNGPCGateway {

    @Override
    public Response enviar(Request request) {
        String protocolo = "SNGPC-DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info(
            "[dev/test] SNGPC simulado — protocolo {} | medicamento {} | lote {} | qtd {}",
            protocolo,
            request.medicamento() != null ? request.medicamento().getId() : null,
            request.lote() != null ? request.lote().getId() : null,
            request.quantidade()
        );
        return new Response(protocolo, "Envio simulado (ambiente dev/test)", true);
    }
}
