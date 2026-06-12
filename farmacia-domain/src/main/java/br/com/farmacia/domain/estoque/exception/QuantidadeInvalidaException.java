package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando a quantidade informada não atende às regras de negócio.
 *
 * @author Alex Silva e Claude
 */
public class QuantidadeInvalidaException extends DomainException {

    public QuantidadeInvalidaException() {
        super("Quantidade deve ser maior que zero");
    }
}
