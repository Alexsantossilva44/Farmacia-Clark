package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe fornecedor cadastrado com o mesmo CNPJ.
 *
 * @author Alex Silva e Claude
 */
public class CnpjDuplicadoException extends DomainException {

    public CnpjDuplicadoException(String cnpj) {
        super("CNPJ já cadastrado: " + cnpj);
    }
}
