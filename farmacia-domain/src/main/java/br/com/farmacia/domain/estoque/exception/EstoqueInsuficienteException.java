package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando a quantidade solicitada excede o saldo disponível.
 *
 * @author Alex Silva e Claude
 */
public class EstoqueInsuficienteException extends DomainException {

    private final String nomeMedicamento;
    private final int solicitado;
    private final int disponivel;

    public EstoqueInsuficienteException(String nomeMedicamento, int solicitado, int disponivel) {
        super("Estoque insuficiente para '%s'. Solicitado: %d, disponível: %d"
            .formatted(nomeMedicamento, solicitado, disponivel));
        this.nomeMedicamento = nomeMedicamento;
        this.solicitado = solicitado;
        this.disponivel = disponivel;
    }

    public String getUserMessage() {
        if (disponivel <= 0) {
            return "'%s' não possui unidades disponíveis em estoque para dispensação."
                .formatted(nomeMedicamento);
        }
        return "'%s': você pediu %d un., mas só há %d disponível(is) em estoque."
            .formatted(nomeMedicamento, solicitado, disponivel);
    }
}
