package br.com.farmacia.domain.medicamento.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um medicamento referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class MedicamentoNaoEncontradoException extends DomainException {

    public MedicamentoNaoEncontradoException(UUID id) {
        super("Medicamento não encontrado: " + id);
    }
}
