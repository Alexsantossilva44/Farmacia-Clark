package br.com.farmacia.domain.financeiro.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando não há caixa aberto para o PDV informado.
 *
 * @author Alex Silva e Claude
 */
public class CaixaAbertoNaoEncontradoException extends DomainException {

    public CaixaAbertoNaoEncontradoException(UUID pdvId) {
        super("Não há caixa aberto para o PDV: " + pdvId);
    }
}
