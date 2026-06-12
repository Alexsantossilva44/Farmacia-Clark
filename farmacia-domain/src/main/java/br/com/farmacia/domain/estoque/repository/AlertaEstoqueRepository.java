package br.com.farmacia.domain.estoque.repository;

import br.com.farmacia.domain.estoque.entity.AlertaEstoque;
import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import java.util.List;
import java.util.UUID;

/**
 * Porta de saída para alertas de estoque.
 *
 * @author Alex Silva e Claude
 */
public interface AlertaEstoqueRepository {
    AlertaEstoque save(AlertaEstoque alerta);
    boolean existeAlertaAberto(UUID loteId, TipoAlerta tipo);
    boolean existeAlertaAbertoPorMedicamento(UUID medicamentoId, TipoAlerta tipo);
    List<AlertaEstoque> findByLoteIdAndTipo(UUID loteId, TipoAlerta tipo);
    List<AlertaEstoque> findByMedicamentoId(UUID medicamentoId);
    List<AlertaEstoque> findByMedicamentoIdAndTipo(UUID medicamentoId, TipoAlerta tipo);
    List<AlertaEstoque> findByStatus(StatusAlerta status);
    long countByLoteIdAndTipo(UUID loteId, TipoAlerta tipo);
    void deleteAllByLoteId(UUID loteId);
    void deleteAllByMedicamentoNomeAndTipo(String nome, TipoAlerta tipo);
    void deleteAllByMedicamentoId(UUID medicamentoId);
}
