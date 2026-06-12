package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando uma receita referenciada por número não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class ReceitaPorNumeroNaoEncontradaException extends DomainException {

    public ReceitaPorNumeroNaoEncontradaException(String numero) {
        super("Receita não encontrada com número: " + numero);
    }
}
