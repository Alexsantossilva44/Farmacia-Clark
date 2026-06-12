package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;

/**
 * Mapeamento manual entre {@link Lote} (domínio) e {@link LoteJpaEntity}.
 *
 * <p>O medicamento é referenciado pelo id (FK); o adapter hidrata a entidade
 * completa quando necessário.</p>
 *
 * @author Alex Silva e Claude
 */
public final class LotePersistenceMapper {

    private LotePersistenceMapper() {
    }

    public static LoteJpaEntity toJpa(Lote l) {
        return LoteJpaEntity.builder()
            .id(l.getId())
            .medicamentoId(l.getMedicamento() != null ? l.getMedicamento().getId() : null)
            .notaFiscalId(l.getNotaFiscalId())
            .numeroLote(l.getNumeroLote())
            .dataFabricacao(l.getDataFabricacao())
            .dataValidade(l.getDataValidade())
            .quantidadeRecebida(l.getQuantidadeRecebida())
            .quantidadeAtual(l.getQuantidadeAtual())
            .precoCusto(l.getPrecoCusto())
            .status(l.getStatus() != null ? l.getStatus() : StatusLote.ATIVO)
            .build();
    }

    public static Lote toDomain(LoteJpaEntity e, Medicamento medicamento) {
        return Lote.builder()
            .id(e.getId())
            .notaFiscalId(e.getNotaFiscalId())
            .numeroLote(e.getNumeroLote())
            .dataFabricacao(e.getDataFabricacao())
            .dataValidade(e.getDataValidade())
            .quantidadeRecebida(e.getQuantidadeRecebida())
            .quantidadeAtual(e.getQuantidadeAtual())
            .precoCusto(e.getPrecoCusto())
            .status(e.getStatus())
            .medicamento(medicamento)
            .build();
    }
}
