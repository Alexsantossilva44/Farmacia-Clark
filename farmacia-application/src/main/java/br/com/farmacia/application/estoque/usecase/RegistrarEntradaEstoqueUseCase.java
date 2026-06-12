package br.com.farmacia.application.estoque.usecase;

import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import br.com.farmacia.domain.estoque.exception.LoteVencidoException;
import br.com.farmacia.domain.estoque.exception.QuantidadeInvalidaException;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra entrada de mercadoria: cria ou incrementa lote e atualiza saldo consolidado.
 */
@Service
@RequiredArgsConstructor
public class RegistrarEntradaEstoqueUseCase {

    private final MedicamentoRepository medicamentoRepository;
    private final EstoqueRepository estoqueRepository;
    private final LoteRepository loteRepository;

    @Transactional
    public Output executar(Input input) {
        if (input.quantidade() <= 0) {
            throw new QuantidadeInvalidaException();
        }
        if (input.dataValidade().isBefore(LocalDate.now())) {
            throw new LoteVencidoException();
        }

        Medicamento medicamento = medicamentoRepository.findById(input.medicamentoId())
            .orElseThrow(() -> new MedicamentoNaoEncontradoException(input.medicamentoId()));

        String numeroLote = input.numeroLote().trim();
        Lote lote = loteRepository.findByMedicamentoIdAndNumeroLote(input.medicamentoId(), numeroLote)
            .map(existente -> incrementarLote(existente, input))
            .orElseGet(() -> criarLote(medicamento, input, numeroLote));

        ItemEstoque item = estoqueRepository.findByMedicamentoId(medicamento.getId())
            .orElse(null);

        int saldoAnterior = item != null ? item.getQuantidadeAtual() : 0;

        item = item != null
            ? atualizarSaldo(item, input.quantidade())
            : criarItemEstoque(medicamento, input);

        estoqueRepository.salvarMovimentacao(MovimentacaoEstoque.builder()
            .medicamento(medicamento)
            .lote(lote)
            .tipo(TipoMovimentacao.ENTRADA_COMPRA)
            .quantidade(input.quantidade())
            .saldoAnterior(saldoAnterior)
            .saldoPosterior(item.getQuantidadeAtual())
            .referenciaId(input.referenciaId())
            .motivoAjuste(input.observacao())
            .dataHora(LocalDateTime.now())
            .build());

        return new Output(item, lote);
    }

    private Lote criarLote(Medicamento medicamento, Input input, String numeroLote) {
        LocalDate fabricacao = input.dataFabricacao() != null
            ? input.dataFabricacao()
            : LocalDate.now().minusMonths(1);

        return loteRepository.save(Lote.builder()
            .medicamento(medicamento)
            .notaFiscalId(input.notaFiscalId())
            .numeroLote(numeroLote)
            .dataFabricacao(fabricacao)
            .dataValidade(input.dataValidade())
            .quantidadeRecebida(input.quantidade())
            .quantidadeAtual(input.quantidade())
            .precoCusto(input.precoCusto())
            .status(StatusLote.ATIVO)
            .build());
    }

    private Lote incrementarLote(Lote lote, Input input) {
        lote.registrarEntradaAdicional(
            input.quantidade(),
            input.notaFiscalId(),
            input.dataValidade(),
            input.precoCusto()
        );
        return loteRepository.save(lote);
    }

    private ItemEstoque criarItemEstoque(Medicamento medicamento, Input input) {
        return estoqueRepository.salvar(ItemEstoque.builder()
            .medicamento(medicamento)
            .quantidadeAtual(input.quantidade())
            .quantidadeMinima(input.quantidadeMinima() != null ? input.quantidadeMinima() : 5)
            .quantidadeMaxima(input.quantidadeMaxima() != null ? input.quantidadeMaxima() : 500)
            .ultimaMovimentacao(LocalDateTime.now())
            .build());
    }

    private ItemEstoque atualizarSaldo(ItemEstoque item, int quantidade) {
        item.incrementarSaldo(quantidade);
        return estoqueRepository.salvar(item);
    }

    public record Input(
        UUID medicamentoId,
        String numeroLote,
        LocalDate dataValidade,
        LocalDate dataFabricacao,
        int quantidade,
        BigDecimal precoCusto,
        Integer quantidadeMinima,
        Integer quantidadeMaxima,
        String observacao,
        UUID notaFiscalId,
        UUID referenciaId
    ) {}

    public record Output(ItemEstoque itemEstoque, Lote lote) {}
}
