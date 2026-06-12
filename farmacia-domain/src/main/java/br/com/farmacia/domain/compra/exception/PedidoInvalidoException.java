package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o pedido de compra não atende às regras de negócio.
 *
 * @author Alex Silva e Claude
 */
public class PedidoInvalidoException extends DomainException {

    public PedidoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
