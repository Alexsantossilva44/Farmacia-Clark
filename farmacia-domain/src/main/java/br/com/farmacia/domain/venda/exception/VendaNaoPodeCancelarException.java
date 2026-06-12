package br.com.farmacia.domain.venda.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando uma venda não pode ser cancelada no status atual.
 *
 * @author Alex Silva e Claude
 */
public class VendaNaoPodeCancelarException extends DomainException {

    public VendaNaoPodeCancelarException(UUID id) {
        super("Venda não pode ser cancelada: " + id);
    }
}
