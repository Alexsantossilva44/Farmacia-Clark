package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o tipo de ajuste de estoque informado é inválido.
 *
 * @author Alex Silva e Claude
 */
public class TipoAjusteInvalidoException extends DomainException {

    public TipoAjusteInvalidoException() {
        super("Tipo de ajuste inválido");
    }
}
