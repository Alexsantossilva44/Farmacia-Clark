package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um lote referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class LoteNaoEncontradoException extends DomainException {

    public LoteNaoEncontradoException(UUID id) {
        super("Lote não encontrado: " + id);
    }
}
