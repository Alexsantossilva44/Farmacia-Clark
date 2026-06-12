package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o status do pedido de compra é inválido para a operação.
 *
 * @author Alex Silva e Claude
 */
public class StatusInvalidoException extends DomainException {

    public StatusInvalidoException(String mensagem) {
        super(mensagem);
    }
}
