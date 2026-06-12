package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando um pedido de compra é criado sem itens.
 *
 * @author Alex Silva e Claude
 */
public class ItensObrigatoriosException extends DomainException {

    public ItensObrigatoriosException() {
        super("Pedido deve conter ao menos um item");
    }
}
