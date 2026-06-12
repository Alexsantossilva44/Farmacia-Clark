package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe NF-e registrada com a mesma chave de acesso.
 *
 * @author Alex Silva e Claude
 */
public class ChaveDuplicadaException extends DomainException {

    public ChaveDuplicadaException(String chave) {
        super("Chave de acesso já cadastrada: " + chave);
    }
}
