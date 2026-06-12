package br.com.farmacia.domain.medicamento.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe medicamento cadastrado com o mesmo código EAN.
 *
 * @author Alex Silva e Claude
 */
public class MedicamentoDuplicadoException extends DomainException {

    public MedicamentoDuplicadoException(String codigoEan) {
        super("Medicamento já cadastrado com EAN: " + codigoEan);
    }
}
