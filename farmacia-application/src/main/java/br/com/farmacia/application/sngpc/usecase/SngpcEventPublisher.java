package br.com.farmacia.application.sngpc.usecase;

import java.util.UUID;

/**
 * Porta de saída: publicação de eventos SNGPC.
 * Implementada na infra com RabbitMQ.
 *
 * @author Alex Silva e Claude
 */
public interface SngpcEventPublisher {
    void publicar(UUID registroId, EnviarRegistroSNGPCUseCase.Input dados);
}
