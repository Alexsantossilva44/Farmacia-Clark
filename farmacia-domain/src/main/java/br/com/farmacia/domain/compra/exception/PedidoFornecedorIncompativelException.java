package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o pedido de compra não pertence ao fornecedor da NF-e.
 *
 * @author Alex Silva e Claude
 */
public class PedidoFornecedorIncompativelException extends DomainException {

    public PedidoFornecedorIncompativelException() {
        super("Pedido não pertence ao fornecedor da NF-e");
    }
}
