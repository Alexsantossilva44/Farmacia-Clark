package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando a chave de acesso da NF-e é inválida.
 *
 * @author Alex Silva e Claude
 */
public class ChaveInvalidaException extends DomainException {

    public ChaveInvalidaException() {
        super("Chave de acesso NF-e inválida");
    }
}
