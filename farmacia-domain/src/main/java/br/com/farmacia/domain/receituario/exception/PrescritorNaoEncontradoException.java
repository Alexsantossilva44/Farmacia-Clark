package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um prescritor referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class PrescritorNaoEncontradoException extends DomainException {

    public PrescritorNaoEncontradoException(UUID id) {
        super("Prescritor não encontrado: " + id);
    }
}
