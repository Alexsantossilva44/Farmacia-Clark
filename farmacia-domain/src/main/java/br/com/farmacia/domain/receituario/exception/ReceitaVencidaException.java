package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando a receita está fora do prazo de validade.
 *
 * @author Alex Silva e Claude
 */
public class ReceitaVencidaException extends DomainException {

    public ReceitaVencidaException(UUID id) {
        super("Receita [%s] está vencida.".formatted(id));
    }
}
