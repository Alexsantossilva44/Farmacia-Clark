package br.com.farmacia.application.venda.usecase;

import br.com.farmacia.application.sngpc.usecase.EnviarRegistroSNGPCUseCase;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import br.com.farmacia.domain.venda.entity.ItemVenda;
import br.com.farmacia.domain.venda.entity.Pagamento;
import br.com.farmacia.domain.venda.entity.Venda;
import br.com.farmacia.domain.venda.enums.FormaPagamento;
import br.com.farmacia.domain.venda.enums.StatusPagamento;
import br.com.farmacia.domain.cliente.exception.ClienteNaoEncontradoException;
import br.com.farmacia.domain.estoque.exception.EstoqueInsuficienteException;
import br.com.farmacia.domain.financeiro.exception.CaixaFechadoException;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.exception.PrecoAcimaPMCException;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoAprovadaException;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;
import br.com.farmacia.domain.receituario.exception.ReceitaObrigatoriaException;
import br.com.farmacia.domain.receituario.exception.ReceitaVencidaException;
import br.com.farmacia.domain.venda.exception.CpfObrigatorioException;
import br.com.farmacia.domain.venda.exception.PagamentoInsuficienteException;
import br.com.farmacia.domain.venda.enums.StatusVenda;
import br.com.farmacia.domain.venda.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caso de Uso: Realizar Venda.
 *
 * <p>Orquestra o fluxo completo:</p>
 * <ol>
 *   <li>Valida caixa aberto</li>
 *   <li>Seleciona lotes por FEFO (First Expired First Out)</li>
 *   <li>Verifica PMC (Preço Máximo ao Consumidor)</li>
 *   <li>Baixa estoque atomicamente</li>
 *   <li>Registra pagamento e finaliza venda</li>
 *   <li>Publica registro SNGPC para controlados (assíncrono)</li>
 * </ol>
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealizarVendaUseCase {

    private final VendaRepository         vendaRepository;
    private final MedicamentoRepository   medicamentoRepository;
    private final LoteRepository          loteRepository;
    private final EstoqueRepository       estoqueRepository;
    private final ReceitaRepository       receitaRepository;
    private final ClienteRepository       clienteRepository;
    private final CaixaRepository         caixaRepository;
    private final EnviarRegistroSNGPCUseCase enviarSNGPC;

    // ─── Input ────────────────────────────────────────────────────────────────

    public record Input(
        UUID pdvId,
        UUID funcionarioId,
        UUID clienteId,
        UUID receitaId,
        List<ItemInput> itens,
        List<PagamentoInput> pagamentos,
        String compradorCpf,
        String compradorNome
    ) {
        public record ItemInput(
            UUID medicamentoId,
            int quantidade,
            BigDecimal precoUnitario,
            BigDecimal desconto
        ) {}

        public record PagamentoInput(
            FormaPagamento forma,
            BigDecimal valor
        ) {}
    }

    // ─── Output ───────────────────────────────────────────────────────────────

    public record Output(
        UUID vendaId,
        String numeroCupom,
        BigDecimal total,
        List<String> avisos
    ) {}

    // ─── Execução ─────────────────────────────────────────────────────────────

    @Transactional
    public Output executar(Input input) {
        log.info("Iniciando venda [pdvId={}, funcionario={}]",
                 input.pdvId(), input.funcionarioId());

        List<String> avisos = new ArrayList<>();

        // 1. Validar caixa aberto
        Caixa caixa = caixaRepository
            .findCaixaAbertoPorPdv(input.pdvId())
            .orElseThrow(() -> new CaixaFechadoException(input.pdvId()));

        // 2. Montar venda
        Venda venda = Venda.builder()
            .pdv(caixa.getPdv())
            .caixa(caixa)
            .dataHora(LocalDateTime.now())
            .status(StatusVenda.ABERTA)
            .desconto(BigDecimal.ZERO)
            .itens(new ArrayList<>())
            .pagamentos(new ArrayList<>())
            .build();

        if (input.clienteId() != null) {
            venda.associarCliente(
                clienteRepository.findById(input.clienteId())
                    .orElseThrow(() -> new ClienteNaoEncontradoException(input.clienteId()))
            );
        }

        // H-03: pré-valida receita e CPF antes de qualquer decremento de estoque
        {
            boolean precisaCpf = false;
            boolean temControlado = false;
            for (Input.ItemInput pre : input.itens()) {
                Medicamento preMed = medicamentoRepository.findById(pre.medicamentoId())
                    .orElseThrow(() -> new MedicamentoNaoEncontradoException(pre.medicamentoId()));
                NivelControle nivel = preMed.getNivelControle();
                if (isControlado(nivel)) { precisaCpf = true; temControlado = true; }
                if (nivel == NivelControle.ANTIMICROBIANO) precisaCpf = true;
                if (preMed.isRequerReceita()) {
                    validarReceita(input.receitaId(), preMed, pre.quantidade());
                }
            }
            if (precisaCpf && (input.compradorCpf() == null || input.compradorCpf().isBlank())) {
                throw new CpfObrigatorioException(temControlado ? "medicamento controlado" : "antimicrobiano");
            }
        }

        // 3. Processar itens
        boolean possuiControlado     = false;
        boolean possuiAntimicrobiano = false;

        for (Input.ItemInput itemInput : input.itens()) {
            Medicamento med = medicamentoRepository.findById(itemInput.medicamentoId())
                .orElseThrow(() -> new MedicamentoNaoEncontradoException(itemInput.medicamentoId()));

            // Valida receita quando necessário
            if (med.isRequerReceita()) {
                validarReceita(input.receitaId(), med, itemInput.quantidade());
            }

            if (isControlado(med.getNivelControle()))           possuiControlado     = true;
            if (med.getNivelControle() == NivelControle.ANTIMICROBIANO) possuiAntimicrobiano = true;

            // Valida PMC
            if (itemInput.precoUnitario().compareTo(med.getPrecoMaximoConsumidor()) > 0) {
                throw new PrecoAcimaPMCException(
                    med.getNomeComercial(), itemInput.precoUnitario(), med.getPrecoMaximoConsumidor());
            }

            List<AlocacaoLote> alocacoes = alocarLotesFefo(med, itemInput.quantidade());
            BigDecimal descTotal = itemInput.desconto() != null ? itemInput.desconto() : BigDecimal.ZERO;
            // M-01: desconto não pode exceder o valor total do item (precoUnitario × quantidade)
            BigDecimal valorMaximoDesconto = itemInput.precoUnitario()
                .multiply(BigDecimal.valueOf(itemInput.quantidade()));
            if (descTotal.compareTo(valorMaximoDesconto) > 0) {
                throw new IllegalArgumentException(
                    "Desconto R$" + descTotal + " excede o valor total do item '"
                    + med.getNomeComercial() + "' (R$" + valorMaximoDesconto + ").");
            }
            int qtdTotal = itemInput.quantidade();
            BigDecimal descRestante = descTotal;

            for (int i = 0; i < alocacoes.size(); i++) {
                AlocacaoLote aloc = alocacoes.get(i);
                Lote lote = aloc.lote();
                int qtdLote = aloc.quantidade();

                if (lote.venceEm(30)) {
                    avisos.add("Atenção: lote %s de '%s' vence em %d dias."
                        .formatted(lote.getNumeroLote(), med.getNomeComercial(), lote.diasParaVencer()));
                }

                int saldoAnterior = lote.getQuantidadeAtual();
                lote.consumir(qtdLote);
                loteRepository.save(lote);

                estoqueRepository.salvarMovimentacao(MovimentacaoEstoque.builder()
                    .lote(lote)
                    .medicamento(med)
                    .tipo(TipoMovimentacao.SAIDA_VENDA)
                    .quantidade(qtdLote)
                    .saldoAnterior(saldoAnterior)
                    .saldoPosterior(lote.getQuantidadeAtual())
                    .dataHora(LocalDateTime.now())
                    .build());

                BigDecimal descPart;
                if (i == alocacoes.size() - 1) {
                    descPart = descRestante;
                } else {
                    descPart = descTotal.multiply(BigDecimal.valueOf(qtdLote))
                        .divide(BigDecimal.valueOf(qtdTotal), 2, RoundingMode.HALF_UP);
                    descRestante = descRestante.subtract(descPart);
                }

                BigDecimal subtotal = itemInput.precoUnitario()
                    .multiply(BigDecimal.valueOf(qtdLote))
                    .subtract(descPart);

                venda.getItens().add(ItemVenda.builder()
                    .venda(venda)
                    .medicamento(med)
                    .lote(lote)
                    .quantidade(qtdLote)
                    .precoUnitario(itemInput.precoUnitario())
                    .desconto(descPart)
                    .subtotal(subtotal)
                    .build());
            }

            estoqueRepository.decrementarSaldo(med.getId(), itemInput.quantidade());
        }

        // 4. CPF — validação antecipada já realizada no bloco H-03 acima; nada a fazer aqui.

        // 5. Calcula totais
        venda.recalcularTotais();

        // 6. Valida pagamentos
        BigDecimal totalPago = input.pagamentos().stream()
            .map(Input.PagamentoInput::valor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPago.compareTo(venda.getTotal()) < 0) {
            throw new PagamentoInsuficienteException(venda.getTotal(), totalPago);
        }

        // 7. Registra pagamentos e calcula troco (H-01)
        List<Pagamento> pagamentosRegistrados = new ArrayList<>();
        for (Input.PagamentoInput pgto : input.pagamentos()) {
            pagamentosRegistrados.add(Pagamento.builder()
                .venda(venda)
                .forma(pgto.forma())
                .valor(pgto.valor())
                .dataHora(LocalDateTime.now())
                .status(StatusPagamento.APROVADO)
                .build());
        }
        // H-01: troco = diferença entre total pago e total da venda; aplica no último pagamento
        BigDecimal troco = totalPago.subtract(venda.getTotal()).max(BigDecimal.ZERO);
        if (troco.compareTo(BigDecimal.ZERO) > 0 && !pagamentosRegistrados.isEmpty()) {
            pagamentosRegistrados.get(pagamentosRegistrados.size() - 1).registrarTroco(troco);
        }
        venda.getPagamentos().addAll(pagamentosRegistrados);

        // 8. Associa receita e marca como utilizada
        if (input.receitaId() != null) {
            Receita receita = receitaRepository.findById(input.receitaId())
                .orElseThrow(() -> new ReceitaNaoEncontradaException(input.receitaId()));
            receita.marcarComoUtilizada();
            receitaRepository.save(receita);
            venda.associarReceita(receita);
        }

        // 9. Finaliza venda
        venda.finalizar("CUP-%d-%s"
            .formatted(System.currentTimeMillis(),
                       UUID.randomUUID().toString().substring(0, 6).toUpperCase()));

        Venda vendaSalva = vendaRepository.save(venda);
        log.info("Venda [{}] finalizada. Total: R$ {}", vendaSalva.getId(), vendaSalva.getTotal());

        // H-02: atualiza totalizador do caixa após venda confirmada
        caixa.incrementarTotalVendas(vendaSalva.getTotal());
        caixaRepository.save(caixa);

        // 10. SNGPC assíncrono para controlados
        if (possuiControlado) {
            // C-05: usa receitaId do input como fallback caso getReceita() não esteja hidratado
            UUID receitaIdSngpc = vendaSalva.getReceita() != null
                ? vendaSalva.getReceita().getId()
                : input.receitaId();

            vendaSalva.getItens().stream()
                .filter(i -> isControlado(i.getMedicamento().getNivelControle()))
                .forEach(i -> {
                    try {
                        enviarSNGPC.publicarNaFila(new EnviarRegistroSNGPCUseCase.Input(
                            receitaIdSngpc, // C-05: sem NPE quando receita não está hidratada
                            i.getMedicamento().getId(),
                            i.getLote().getId(),
                            input.compradorNome(),
                            input.compradorCpf(),
                            i.getQuantidade()
                        ));
                    } catch (Exception e) {
                        // mantém venda finalizada mesmo com falha SNGPC; retry pelo scheduler
                        log.error("Falha ao publicar SNGPC para venda [{}]: ", vendaSalva.getId(), e);
                    }
                });
        }

        return new Output(vendaSalva.getId(), vendaSalva.getNumeroCupom(),
                          vendaSalva.getTotal(), avisos);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private record AlocacaoLote(Lote lote, int quantidade) {}

    private List<AlocacaoLote> alocarLotesFefo(Medicamento med, int qtd) {
        int restante = qtd;
        List<AlocacaoLote> alocacoes = new ArrayList<>();
        for (Lote lote : loteRepository.findLotesDisponivelFefo(med.getId())) {
            if (restante <= 0) {
                break;
            }
            if (lote.estaVencido()) {
                continue;
            }
            int saldo = lote.getQuantidadeAtual() != null ? lote.getQuantidadeAtual() : 0;
            if (saldo <= 0) {
                continue;
            }
            int take = Math.min(restante, saldo);
            alocacoes.add(new AlocacaoLote(lote, take));
            restante -= take;
        }
        if (restante > 0) {
            int disponivel = qtd - restante;
            throw new EstoqueInsuficienteException(med.getNomeComercial(), qtd, disponivel);
        }
        return alocacoes;
    }

    private void validarReceita(UUID receitaId, Medicamento med, int quantidade) {
        if (receitaId == null) throw new ReceitaObrigatoriaException(med.getNomeComercial());
        Receita receita = receitaRepository.findById(receitaId)
            .orElseThrow(() -> new ReceitaNaoEncontradaException(receitaId));
        if (receita.getStatus() != StatusReceita.APROVADA)
            throw new ReceitaNaoAprovadaException(receitaId, receita.getStatus());
        if (receita.estaVencida())
            throw new ReceitaVencidaException(receitaId);
    }

    private boolean isControlado(NivelControle nivel) {
        return nivel == NivelControle.CONTROLADO_B1
            || nivel == NivelControle.CONTROLADO_B2
            || nivel == NivelControle.CONTROLADO_C1
            || nivel == NivelControle.CONTROLADO_C2;
    }
}
