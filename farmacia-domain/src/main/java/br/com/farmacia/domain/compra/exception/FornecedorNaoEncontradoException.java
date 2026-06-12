package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um fornecedor referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class FornecedorNaoEncontradoException extends DomainException {

    public FornecedorNaoEncontradoException(UUID id) {
        super("Fornecedor não encontrado: " + id);
    }
}
