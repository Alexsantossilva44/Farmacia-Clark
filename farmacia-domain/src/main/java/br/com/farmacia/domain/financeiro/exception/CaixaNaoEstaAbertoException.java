package br.com.farmacia.domain.financeiro.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando o caixa referenciado não está em status aberto.
 *
 * @author Alex Silva e Claude
 */
public class CaixaNaoEstaAbertoException extends DomainException {

    public CaixaNaoEstaAbertoException(UUID caixaId) {
        super("Caixa não está aberto: " + caixaId);
    }
}
