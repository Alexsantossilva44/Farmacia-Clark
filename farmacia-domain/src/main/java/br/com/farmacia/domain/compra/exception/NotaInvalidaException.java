package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando a nota fiscal de entrada não atende às regras de negócio.
 *
 * @author Alex Silva e Claude
 */
public class NotaInvalidaException extends DomainException {

    public NotaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
