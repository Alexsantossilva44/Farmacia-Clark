package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando não há item de estoque para o medicamento informado.
 *
 * @author Alex Silva e Claude
 */
public class ItemEstoqueNaoEncontradoException extends DomainException {

    public ItemEstoqueNaoEncontradoException(UUID medicamentoId) {
        super("Item de estoque não encontrado para medicamento: " + medicamentoId);
    }
}
