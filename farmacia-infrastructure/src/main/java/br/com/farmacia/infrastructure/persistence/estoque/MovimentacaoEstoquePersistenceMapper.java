package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;

import java.time.LocalDateTime;

/**
 * Mapeamento manual entre {@link MovimentacaoEstoque} (domínio) e
 * {@link MovimentacaoEstoqueJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class MovimentacaoEstoquePersistenceMapper {

    private MovimentacaoEstoquePersistenceMapper() {
    }

    public static MovimentacaoEstoqueJpaEntity toJpa(MovimentacaoEstoque m) {
        return MovimentacaoEstoqueJpaEntity.builder()
            .id(m.getId())
            .loteId(m.getLote() != null ? m.getLote().getId() : null)
            .medicamentoId(m.getMedicamento() != null ? m.getMedicamento().getId() : null)
            .tipo(m.getTipo())
            .quantidade(m.getQuantidade())
            .saldoAnterior(m.getSaldoAnterior())
            .saldoPosterior(m.getSaldoPosterior())
            .referenciaId(m.getReferenciaId())
            .motivoAjuste(m.getMotivoAjuste())
            .dataHora(m.getDataHora() != null ? m.getDataHora() : LocalDateTime.now())
            .build();
    }

    public static MovimentacaoEstoque toDomain(
            MovimentacaoEstoqueJpaEntity e,
            br.com.farmacia.domain.medicamento.entity.Medicamento medicamento,
            br.com.farmacia.domain.estoque.entity.Lote lote) {
        return MovimentacaoEstoque.builder()
            .id(e.getId())
            .medicamento(medicamento)
            .lote(lote)
            .tipo(e.getTipo())
            .quantidade(e.getQuantidade())
            .saldoAnterior(e.getSaldoAnterior())
            .saldoPosterior(e.getSaldoPosterior())
            .referenciaId(e.getReferenciaId())
            .motivoAjuste(e.getMotivoAjuste())
            .dataHora(e.getDataHora())
            .build();
    }
}
