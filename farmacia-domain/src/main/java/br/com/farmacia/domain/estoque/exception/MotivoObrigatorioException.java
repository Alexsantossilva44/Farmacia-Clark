package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o motivo do ajuste de estoque não foi informado.
 *
 * @author Alex Silva e Claude
 */
public class MotivoObrigatorioException extends DomainException {

    public MotivoObrigatorioException() {
        super("Motivo do ajuste é obrigatório");
    }
}
