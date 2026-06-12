package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o lote informado não pertence ao medicamento selecionado.
 *
 * @author Alex Silva e Claude
 */
public class LoteMedicamentoIncompativelException extends DomainException {

    public LoteMedicamentoIncompativelException() {
        super("O lote informado não pertence ao medicamento selecionado");
    }
}
