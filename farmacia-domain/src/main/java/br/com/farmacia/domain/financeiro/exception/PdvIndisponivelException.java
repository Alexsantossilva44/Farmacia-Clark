package br.com.farmacia.domain.financeiro.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o PDV informado está indisponível para operação.
 *
 * @author Alex Silva e Claude
 */
public class PdvIndisponivelException extends DomainException {

    public PdvIndisponivelException(String numero) {
        super("PDV indisponível: " + numero);
    }
}
