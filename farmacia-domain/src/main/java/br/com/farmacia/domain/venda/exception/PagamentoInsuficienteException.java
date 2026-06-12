package br.com.farmacia.domain.venda.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.math.BigDecimal;

/**
 * Lançada quando o total pago não cobre o valor da venda.
 *
 * @author Alex Silva e Claude
 */
public class PagamentoInsuficienteException extends DomainException {

    public PagamentoInsuficienteException(BigDecimal total, BigDecimal pago) {
        super("Pagamento insuficiente. Total: R$ %s | Pago: R$ %s".formatted(total, pago));
    }
}
