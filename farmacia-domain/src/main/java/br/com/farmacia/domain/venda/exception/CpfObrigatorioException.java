package br.com.farmacia.domain.venda.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

/**
 * Lançada quando o CPF do comprador é obrigatório (controlados/antimicrobianos).
 *
 * @author Alex Silva e Claude
 */
public class CpfObrigatorioException extends DomainException {

    public CpfObrigatorioException(String motivo) {
        super("CPF do comprador é obrigatório para " + motivo);
    }
}
