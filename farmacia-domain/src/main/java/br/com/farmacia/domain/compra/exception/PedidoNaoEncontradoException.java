package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um pedido de compra referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class PedidoNaoEncontradoException extends DomainException {

    public PedidoNaoEncontradoException(UUID id) {
        super("Pedido não encontrado: " + id);
    }
}
