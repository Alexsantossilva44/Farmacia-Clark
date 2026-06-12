package br.com.farmacia.domain.medicamento.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.math.BigDecimal;

/**
 * Lançada quando o preço de venda excede o PMC (Preço Máximo ao Consumidor).
 *
 * @author Alex Silva e Claude
 */
public class PrecoAcimaPMCException extends DomainException {

    public PrecoAcimaPMCException(String nome, BigDecimal preco, BigDecimal pmc) {
        super("Preço R$ %s de '%s' excede o PMC de R$ %s".formatted(preco, nome, pmc));
    }
}
