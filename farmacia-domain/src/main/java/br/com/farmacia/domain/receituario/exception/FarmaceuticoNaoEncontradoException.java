package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um farmacêutico referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class FarmaceuticoNaoEncontradoException extends DomainException {

    public FarmaceuticoNaoEncontradoException(UUID id) {
        super("Farmacêutico não encontrado: " + id);
    }
}
