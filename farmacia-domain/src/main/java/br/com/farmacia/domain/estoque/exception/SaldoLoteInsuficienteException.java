package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o saldo do lote é insuficiente para a operação solicitada.
 *
 * @author Alex Silva e Claude
 */
public class SaldoLoteInsuficienteException extends DomainException {

    public SaldoLoteInsuficienteException(String numeroLote, int disponivel) {
        super("Saldo insuficiente no lote %s (disponível: %d)".formatted(numeroLote, disponivel));
    }
}
