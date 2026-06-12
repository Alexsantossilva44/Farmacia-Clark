package br.com.farmacia.domain.cliente.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe cliente cadastrado com o mesmo telefone.
 *
 * <p>Mensagem genérica — não inclui o número (PII / LGPD).</p>
 */
public class TelefoneClienteDuplicadoException extends DomainException {

    public TelefoneClienteDuplicadoException() {
        super("Telefone já está em uso.");
    }
}
