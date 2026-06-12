package br.com.farmacia.domain.estoque.repository;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para persistência de lotes.
 *
 * @author Alex Silva e Claude
 */
public interface LoteRepository {
    Lote save(Lote lote);
    Optional<Lote> findById(UUID id);
    Optional<Lote> findByNumeroLote(String numeroLote);
    Optional<Lote> findByMedicamentoIdAndNumeroLote(UUID medicamentoId, String numeroLote);
    /** Retorna lotes disponíveis ordenados por validade ASC (FEFO). */
    List<Lote> findLotesDisponivelFefo(UUID medicamentoId);
    /** Todos os lotes do medicamento, para seleção em ajustes de inventário. */
    List<Lote> findByMedicamentoIdOrderByDataValidadeAsc(UUID medicamentoId);
    List<Lote> findByStatusAndDataValidadeBefore(StatusLote status, LocalDate data);
    List<Lote> findLotesProximosVencer(StatusLote status, LocalDate dataLimite);
    /** Soma de saldo em lotes ATIVO, não vencidos, por medicamento (dispensação FEFO). */
    Map<UUID, Integer> somarDisponivelVendaPorMedicamento();
    Lote getReferenceById(UUID id);
}
