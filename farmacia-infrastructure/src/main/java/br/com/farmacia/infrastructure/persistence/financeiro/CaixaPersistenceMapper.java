package br.com.farmacia.infrastructure.persistence.financeiro;

import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.enums.StatusCaixa;
import br.com.farmacia.domain.funcionario.entity.Funcionario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapeamento manual entre {@link Caixa} (domínio) e {@link CaixaJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class CaixaPersistenceMapper {

    private CaixaPersistenceMapper() {
    }

    public static CaixaJpaEntity toJpa(Caixa c) {
        return CaixaJpaEntity.builder()
            .id(c.getId())
            .pdvId(c.getPdv() != null ? c.getPdv().getId() : null)
            .funcionarioId(c.getFuncionario() != null ? c.getFuncionario().getId() : null)
            .abertura(c.getAbertura() != null ? c.getAbertura() : LocalDateTime.now())
            .fechamento(c.getFechamento())
            .saldoAbertura(c.getSaldoAbertura() != null ? c.getSaldoAbertura() : BigDecimal.ZERO)
            .saldoFechamento(c.getSaldoFechamento())
            .totalVendas(c.getTotalVendas() != null ? c.getTotalVendas() : BigDecimal.ZERO)
            .totalEntradas(c.getTotalEntradas() != null ? c.getTotalEntradas() : BigDecimal.ZERO)
            .totalSaidas(c.getTotalSaidas() != null ? c.getTotalSaidas() : BigDecimal.ZERO)
            .status(c.getStatus() != null ? c.getStatus() : StatusCaixa.ABERTO)
            .observacao(c.getObservacao())
            .build();
    }

    public static Caixa toDomain(CaixaJpaEntity e, PDV pdv) {
        return Caixa.builder()
            .id(e.getId())
            .pdv(pdv)
            .funcionario(e.getFuncionarioId() != null
                ? Funcionario.builder().id(e.getFuncionarioId()).build() : null)
            .abertura(e.getAbertura())
            .fechamento(e.getFechamento())
            .saldoAbertura(e.getSaldoAbertura())
            .saldoFechamento(e.getSaldoFechamento())
            .totalVendas(e.getTotalVendas())
            .totalEntradas(e.getTotalEntradas())
            .totalSaidas(e.getTotalSaidas())
            .status(e.getStatus())
            .observacao(e.getObservacao())
            .build();
    }
}
