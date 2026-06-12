package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando o funcionário informado não possui registro de farmacêutico.
 *
 * @author Alex Silva e Claude
 */
public class FarmaceuticoNaoVinculadoException extends DomainException {

    public FarmaceuticoNaoVinculadoException(UUID funcionarioId) {
        super("Funcionário não possui registro de farmacêutico: " + funcionarioId);
    }
}
