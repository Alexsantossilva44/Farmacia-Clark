package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o saldo consolidado de estoque é insuficiente para a operação.
 *
 * @author Alex Silva e Claude
 */
public class SaldoConsolidadoInsuficienteException extends DomainException {

    public SaldoConsolidadoInsuficienteException(int disponivel) {
        super("Saldo consolidado insuficiente (disponível: %d)".formatted(disponivel));
    }
}
