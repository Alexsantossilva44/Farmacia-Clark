package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.medicamento.entity.Medicamento;

/**
 * Mapeamento manual entre {@link ItemEstoque} (domínio) e
 * {@link ItemEstoqueJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class ItemEstoquePersistenceMapper {

    private ItemEstoquePersistenceMapper() {
    }

    public static ItemEstoqueJpaEntity toJpa(ItemEstoque i) {
        return ItemEstoqueJpaEntity.builder()
            .id(i.getId())
            .medicamentoId(i.getMedicamento() != null ? i.getMedicamento().getId() : null)
            .quantidadeAtual(i.getQuantidadeAtual() != null ? i.getQuantidadeAtual() : 0)
            .quantidadeMinima(i.getQuantidadeMinima() != null ? i.getQuantidadeMinima() : 5)
            .quantidadeMaxima(i.getQuantidadeMaxima() != null ? i.getQuantidadeMaxima() : 500)
            .ultimaMovimentacao(i.getUltimaMovimentacao())
            .build();
    }

    public static ItemEstoque toDomain(ItemEstoqueJpaEntity e, Medicamento medicamento) {
        return ItemEstoque.builder()
            .id(e.getId())
            .medicamento(medicamento)
            .quantidadeAtual(e.getQuantidadeAtual())
            .quantidadeMinima(e.getQuantidadeMinima())
            .quantidadeMaxima(e.getQuantidadeMaxima())
            .ultimaMovimentacao(e.getUltimaMovimentacao())
            .build();
    }
}
