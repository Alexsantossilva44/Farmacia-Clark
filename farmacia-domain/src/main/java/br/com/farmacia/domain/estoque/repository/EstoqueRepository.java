package br.com.farmacia.domain.estoque.repository;

import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para controle de estoque.
 *
 * @author Alex Silva e Claude
 */
public interface EstoqueRepository {
    Optional<ItemEstoque> findByMedicamentoId(UUID medicamentoId);

    Map<UUID, ItemEstoque> findByMedicamentoIds(Collection<UUID> medicamentoIds);

    List<ItemEstoque> findAll();
    void decrementarSaldo(UUID medicamentoId, int quantidade);
    void incrementarSaldo(UUID medicamentoId, int quantidade);
    List<ItemEstoque> findItensAbaixoDoMinimo();
    List<ItemEstoque> findItensComEstoqueZerado();
    void salvarMovimentacao(MovimentacaoEstoque movimentacao);
    ItemEstoque salvar(ItemEstoque item);
}
