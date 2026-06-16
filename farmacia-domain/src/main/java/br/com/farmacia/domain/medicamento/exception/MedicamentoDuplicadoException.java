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

    private MedicamentoDuplicadoException(String mensagem, @SuppressWarnings("unused") boolean unused) {
        super(mensagem);
    }

    public static MedicamentoDuplicadoException porNome(String nomeComercial) {
        return new MedicamentoDuplicadoException(
            "Já existe um medicamento cadastrado com o nome: " + nomeComercial, true);
    }
}
