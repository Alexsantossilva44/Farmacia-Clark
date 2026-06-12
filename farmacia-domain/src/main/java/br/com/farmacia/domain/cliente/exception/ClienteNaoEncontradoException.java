package br.com.farmacia.domain.cliente.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um cliente referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class ClienteNaoEncontradoException extends DomainException {

    public ClienteNaoEncontradoException(UUID id) {
        super("Cliente não encontrado: " + id);
    }

    public ClienteNaoEncontradoException(String cpf) {
        super("Cliente não encontrado com CPF: " + cpf);
    }
}
