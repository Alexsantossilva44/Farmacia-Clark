package br.com.farmacia.domain.estoque.repository;

import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoEstoqueRepository {

    List<MovimentacaoEstoque> buscar(
        UUID medicamentoId,
        TipoMovimentacao tipo,
        int offset,
        int limit
    );

    long contar(UUID medicamentoId, TipoMovimentacao tipo);
}
