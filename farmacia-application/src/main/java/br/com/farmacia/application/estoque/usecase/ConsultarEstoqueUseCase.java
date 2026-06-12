package br.com.farmacia.application.estoque.usecase;

import br.com.farmacia.domain.estoque.entity.AlertaEstoque;
import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import br.com.farmacia.domain.estoque.exception.ItemEstoqueNaoEncontradoException;
import br.com.farmacia.domain.estoque.repository.AlertaEstoqueRepository;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consultas de saldo, lotes FEFO e alertas de estoque.
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ConsultarEstoqueUseCase {

    private final EstoqueRepository estoqueRepository;
    private final LoteRepository loteRepository;
    private final AlertaEstoqueRepository alertaEstoqueRepository;

    @Transactional(readOnly = true)
    public ItemEstoque buscarSaldoPorMedicamento(UUID medicamentoId) {
        return estoqueRepository.findByMedicamentoId(medicamentoId)
            .orElseThrow(() -> new ItemEstoqueNaoEncontradoException(medicamentoId));
    }

    @Transactional(readOnly = true)
    public List<Lote> listarLotesFefo(UUID medicamentoId) {
        return loteRepository.findLotesDisponivelFefo(medicamentoId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Integer> mapaDisponivelVenda() {
        return loteRepository.somarDisponivelVendaPorMedicamento();
    }

    @Transactional(readOnly = true)
    public int calcularDisponivelVenda(UUID medicamentoId) {
        return mapaDisponivelVenda().getOrDefault(medicamentoId, 0);
    }

    @Transactional(readOnly = true)
    public List<Lote> listarLotesParaAjuste(UUID medicamentoId) {
        return loteRepository.findByMedicamentoIdOrderByDataValidadeAsc(medicamentoId);
    }

    @Transactional(readOnly = true)
    public List<ItemEstoque> listarAbaixoDoMinimo() {
        return estoqueRepository.findItensAbaixoDoMinimo();
    }

    @Transactional(readOnly = true)
    public List<ItemEstoque> listarEstoqueZerado() {
        return estoqueRepository.findItensComEstoqueZerado();
    }

    @Transactional(readOnly = true)
    public List<AlertaEstoque> listarAlertasAbertos(TipoAlerta tipo) {
        return alertaEstoqueRepository.findByStatus(StatusAlerta.ABERTO).stream()
            .filter(a -> tipo == null || a.getTipo() == tipo)
            .toList();
    }
}
