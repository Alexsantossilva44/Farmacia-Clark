package br.com.farmacia.domain.estoque.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando a data de validade do lote viola regras de negócio.
 *
 * @author Alex Silva e Claude
 */
public class LoteVencidoException extends DomainException {

    public LoteVencidoException() {
        super("Data de validade não pode ser anterior a hoje");
    }
}
