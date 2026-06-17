package br.com.farmacia.domain.medicamento.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando já existe fabricante cadastrado com a mesma razão social (case-insensitive).
 *
 * @author Alex Silva e Claude
 */
public class FabricanteDuplicadoException extends DomainException {

    public FabricanteDuplicadoException(String razaoSocial) {
        super("Já existe um fabricante cadastrado com a razão social: " + razaoSocial);
    }
}
