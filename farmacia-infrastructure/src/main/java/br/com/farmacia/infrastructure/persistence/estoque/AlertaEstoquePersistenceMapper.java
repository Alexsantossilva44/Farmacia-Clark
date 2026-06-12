package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.AlertaEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.medicamento.entity.Medicamento;

import java.time.LocalDateTime;

/**
 * Mapeamento manual entre {@link AlertaEstoque} (domínio) e
 * {@link AlertaEstoqueJpaEntity}.
 *
 * <p>Medicamento e lote são referenciados pelo id (FK).</p>
 *
 * @author Alex Silva e Claude
 */
public final class AlertaEstoquePersistenceMapper {

    private AlertaEstoquePersistenceMapper() {
    }

    public static AlertaEstoqueJpaEntity toJpa(AlertaEstoque a) {
        return AlertaEstoqueJpaEntity.builder()
            .id(a.getId())
            .medicamentoId(a.getMedicamento() != null ? a.getMedicamento().getId() : null)
            .loteId(a.getLote() != null ? a.getLote().getId() : null)
            .tipo(a.getTipo())
            .mensagem(a.getMensagem())
            .dataGeracao(a.getDataGeracao() != null ? a.getDataGeracao() : LocalDateTime.now())
            .lido(a.getLido() != null ? a.getLido() : Boolean.FALSE)
            .status(a.getStatus() != null ? a.getStatus() : StatusAlerta.ABERTO)
            .build();
    }

    public static AlertaEstoque toDomain(AlertaEstoqueJpaEntity e) {
        return AlertaEstoque.builder()
            .id(e.getId())
            .medicamento(e.getMedicamentoId() != null
                ? Medicamento.builder().id(e.getMedicamentoId()).build() : null)
            .lote(e.getLoteId() != null
                ? Lote.builder().id(e.getLoteId()).build() : null)
            .tipo(e.getTipo())
            .mensagem(e.getMensagem())
            .dataGeracao(e.getDataGeracao())
            .lido(e.getLido())
            .status(e.getStatus())
            .build();
    }
}
