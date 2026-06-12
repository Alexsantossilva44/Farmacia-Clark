package br.com.farmacia.application.cliente;

/**
 * Resultado da verificação de disponibilidade de contato para um cliente.
 *
 * <p>{@code true} = disponível para uso; {@code false} = já pertence a outro cliente.
 * Campos em branco/nulos não são consultados no banco e retornam {@code true} como sentinela
 * de "não verificado" — veja {@link ClienteContatoService#verificarDisponibilidade}.</p>
 *
 * <p>Extraído de {@link ClienteContatoService} para que outras classes importem apenas
 * este value object sem depender do {@code @Service} inteiro.</p>
 */
public record DisponibilidadeContato(boolean telefoneDisponivel, boolean emailDisponivel) {}
