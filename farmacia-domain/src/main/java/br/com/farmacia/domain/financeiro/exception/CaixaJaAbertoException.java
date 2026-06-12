package br.com.farmacia.domain.financeiro.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe caixa aberto no PDV informado.
 *
 * @author Alex Silva e Claude
 */
public class CaixaJaAbertoException extends DomainException {

    public CaixaJaAbertoException(String pdvNumero) {
        super("Já existe caixa aberto no PDV: " + pdvNumero);
    }
}
