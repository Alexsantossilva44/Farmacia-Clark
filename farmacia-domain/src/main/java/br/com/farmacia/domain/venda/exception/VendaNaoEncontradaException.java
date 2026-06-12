package br.com.farmacia.domain.venda.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando uma venda referenciada não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class VendaNaoEncontradaException extends DomainException {

    public VendaNaoEncontradaException(UUID id) {
        super("Venda não encontrada: " + id);
    }
}
