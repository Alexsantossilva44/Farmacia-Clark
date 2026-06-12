package br.com.farmacia.domain.receituario.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando a dispensação exige receita médica e ela não foi informada.
 *
 * @author Alex Silva e Claude
 */
public class ReceitaObrigatoriaException extends DomainException {

    public ReceitaObrigatoriaException(String medicamento) {
        super("Receita obrigatória para: " + medicamento);
    }
}
