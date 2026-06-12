package br.com.farmacia.domain.cliente.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe cliente cadastrado com o mesmo e-mail.
 *
 * <p>Mensagem genérica — não inclui o endereço (PII / LGPD).</p>
 */
public class EmailClienteDuplicadoException extends DomainException {

    public EmailClienteDuplicadoException() {
        super("E-mail já está em uso.");
    }
}
