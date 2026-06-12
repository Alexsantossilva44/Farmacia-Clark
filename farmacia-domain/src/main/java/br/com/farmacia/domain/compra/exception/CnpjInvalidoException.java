package br.com.farmacia.domain.compra.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o CNPJ informado é inválido.
 *
 * @author Alex Silva e Claude
 */
public class CnpjInvalidoException extends DomainException {

    public CnpjInvalidoException() {
        super("CNPJ inválido");
    }
}
