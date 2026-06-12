package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe receita cadastrada com o mesmo número.
 *
 * @author Alex Silva e Claude
 */
public class NumeroReceitaDuplicadoException extends DomainException {

    public NumeroReceitaDuplicadoException(String numero) {
        super("Número de receita já cadastrado: " + numero);
    }
}
