package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando uma receita referenciada não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class ReceitaNaoEncontradaException extends DomainException {

    public ReceitaNaoEncontradaException(UUID id) {
        super("Receita não encontrada: " + id);
    }
}
