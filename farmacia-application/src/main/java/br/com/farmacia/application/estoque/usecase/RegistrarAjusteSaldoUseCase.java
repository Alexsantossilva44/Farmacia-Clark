package br.com.farmacia.application.estoque.usecase;

import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import br.com.farmacia.domain.estoque.exception.ItemEstoqueNaoEncontradoException;
import br.com.farmacia.domain.estoque.exception.LoteMedicamentoIncompativelException;
import br.com.farmacia.domain.estoque.exception.LoteNaoEncontradoException;
import br.com.farmacia.domain.estoque.exception.LoteVencidoException;
import br.com.farmacia.domain.estoque.exception.MotivoInvalidoException;
import br.com.farmacia.domain.estoque.exception.MotivoObrigatorioException;
import br.com.farmacia.domain.estoque.exception.QuantidadeInvalidaException;
import br.com.farmacia.domain.estoque.exception.SaldoConsolidadoInsuficienteException;
import br.com.farmacia.domain.estoque.exception.SaldoLoteInsuficienteException;
import br.com.farmacia.domain.estoque.exception.TipoAjusteInvalidoException;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra ajuste manual de saldo (inventário, perda, correção) com trilha de auditoria.
 * Toda alteração exige lote identificado e motivo documentado.
 */
@Service
@RequiredArgsConstructor
public class RegistrarAjusteSaldoUseCase {

    private static final int MOTIVO_MINIMO = 10;

    private final MedicamentoRepository medicamentoRepository;
    private final LoteRepository loteRepository;
    private final EstoqueRepository estoqueRepository;

    @Transactional
    public Output executar(Input input) {
        validarInput(input);

        Medicamento medicamento = medicamentoRepository.findById(input.medicamentoId())
            .orElseThrow(() -> new MedicamentoNaoEncontradoException(input.medicamentoId()));

        Lote lote = loteRepository.findById(input.loteId())
            .orElseThrow(() -> new LoteNaoEncontradoException(input.loteId()));

        if (lote.getMedicamento() == null
                || !medicamento.getId().equals(lote.getMedicamento().getId())) {
            throw new LoteMedicamentoIncompativelException();
        }

        TipoMovimentacao tipo = input.tipo();
        if (tipo == TipoMovimentacao.AJUSTE_POSITIVO && lote.estaVencido()) {
            throw new LoteVencidoException();
        }

        ItemEstoque item = estoqueRepository.findByMedicamentoId(medicamento.getId()).orElse(null);

        if (tipo == TipoMovimentacao.AJUSTE_NEGATIVO) {
            if (item == null) {
                throw new ItemEstoqueNaoEncontradoException(medicamento.getId());
            }
            if (lote.getQuantidadeAtual() < input.quantidade()) {
                throw new SaldoLoteInsuficienteException(lote.getNumeroLote(), lote.getQuantidadeAtual());
            }
            if (item.getQuantidadeAtual() < input.quantidade()) {
                throw new SaldoConsolidadoInsuficienteException(item.getQuantidadeAtual());
            }
        }

        int saldoConsolidadoAnterior = item != null ? item.getQuantidadeAtual() : 0;

        if (tipo == TipoMovimentacao.AJUSTE_POSITIVO) {
            lote.incrementarSaldo(input.quantidade());
            loteRepository.save(lote);
            item = item != null
                ? incrementarItem(item, input.quantidade())
                : criarItem(medicamento, input.quantidade());
        } else {
            lote.decrementarSaldo(input.quantidade());
            loteRepository.save(lote);
            estoqueRepository.decrementarSaldo(medicamento.getId(), input.quantidade());
            item = estoqueRepository.findByMedicamentoId(medicamento.getId())
                .orElseThrow(() -> new ItemEstoqueNaoEncontradoException(medicamento.getId()));
        }

        estoqueRepository.salvarMovimentacao(MovimentacaoEstoque.builder()
            .medicamento(medicamento)
            .lote(lote)
            .tipo(tipo)
            .quantidade(input.quantidade())
            .saldoAnterior(saldoConsolidadoAnterior)
            .saldoPosterior(item.getQuantidadeAtual())
            .motivoAjuste(input.motivo().trim())
            .dataHora(LocalDateTime.now())
            .build());

        return new Output(item, lote);
    }

    private void validarInput(Input input) {
        if (input.quantidade() <= 0) {
            throw new QuantidadeInvalidaException();
        }
        if (input.motivo() == null || input.motivo().isBlank()) {
            throw new MotivoObrigatorioException();
        }
        if (input.motivo().trim().length() < MOTIVO_MINIMO) {
            throw new MotivoInvalidoException(MOTIVO_MINIMO);
        }
        if (input.tipo() != TipoMovimentacao.AJUSTE_POSITIVO
                && input.tipo() != TipoMovimentacao.AJUSTE_NEGATIVO) {
            throw new TipoAjusteInvalidoException();
        }
    }

    private ItemEstoque criarItem(Medicamento medicamento, int quantidade) {
        return estoqueRepository.salvar(ItemEstoque.builder()
            .medicamento(medicamento)
            .quantidadeAtual(quantidade)
            .quantidadeMinima(5)
            .quantidadeMaxima(500)
            .ultimaMovimentacao(LocalDateTime.now())
            .build());
    }

    private ItemEstoque incrementarItem(ItemEstoque item, int quantidade) {
        item.incrementarSaldo(quantidade);
        return estoqueRepository.salvar(item);
    }

    public record Input(
        UUID medicamentoId,
        UUID loteId,
        TipoMovimentacao tipo,
        int quantidade,
        String motivo
    ) {}

    public record Output(ItemEstoque itemEstoque, Lote lote) {}
}
