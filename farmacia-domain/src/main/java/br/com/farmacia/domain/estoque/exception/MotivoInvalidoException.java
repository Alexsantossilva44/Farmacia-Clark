package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o motivo do ajuste não atinge o tamanho mínimo exigido.
 *
 * @author Alex Silva e Claude
 */
public class MotivoInvalidoException extends DomainException {

    public MotivoInvalidoException(int minimo) {
        super("Motivo deve ter pelo menos " + minimo + " caracteres");
    }
}
