package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando a receita não está com status APROVADA para dispensação.
 *
 * @author Alex Silva e Claude
 */
public class ReceitaNaoAprovadaException extends DomainException {

    public ReceitaNaoAprovadaException(UUID id, StatusReceita status) {
        super("Receita [%s] não aprovada. Status: %s".formatted(id, status));
    }
}
