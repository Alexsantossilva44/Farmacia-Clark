package br.com.farmacia.domain.cliente.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Dados cadastrais de cliente fora das regras de negócio.
 */
public class ClienteDadosInvalidosException extends DomainException {

    public ClienteDadosInvalidosException(String message) {
        super(message);
    }
}
