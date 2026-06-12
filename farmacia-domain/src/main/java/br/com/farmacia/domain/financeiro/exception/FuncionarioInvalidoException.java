package br.com.farmacia.domain.financeiro.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando o funcionário informado é inválido ou inativo.
 *
 * @author Alex Silva e Claude
 */
public class FuncionarioInvalidoException extends DomainException {

    public FuncionarioInvalidoException(UUID id) {
        super("Funcionário inválido ou inativo: " + id);
    }
}
