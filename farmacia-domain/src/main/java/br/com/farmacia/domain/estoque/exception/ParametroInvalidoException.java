package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando parâmetros de estoque informados são inválidos.
 *
 * @author Alex Silva e Claude
 */
public class ParametroInvalidoException extends DomainException {

    public ParametroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
