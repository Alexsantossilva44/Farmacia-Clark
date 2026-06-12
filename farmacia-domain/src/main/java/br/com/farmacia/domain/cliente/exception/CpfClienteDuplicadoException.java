package br.com.farmacia.domain.cliente.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe cliente cadastrado com o mesmo CPF.
 *
 * <p>Mensagem genérica — não inclui o CPF na resposta (PII / LGPD).</p>
 */
public class CpfClienteDuplicadoException extends DomainException {

    public CpfClienteDuplicadoException() {
        super("CPF já está em uso.");
    }
}
