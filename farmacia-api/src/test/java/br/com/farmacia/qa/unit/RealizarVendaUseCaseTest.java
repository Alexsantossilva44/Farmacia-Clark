package br.com.farmacia.application.venda.usecase;

import br.com.farmacia.application.sngpc.usecase.EnviarRegistroSNGPCUseCase;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import br.com.farmacia.domain.estoque.exception.EstoqueInsuficienteException;
import br.com.farmacia.domain.financeiro.exception.CaixaFechadoException;
import br.com.farmacia.domain.medicamento.exception.PrecoAcimaPMCException;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoAprovadaException;
import br.com.farmacia.domain.receituario.exception.ReceitaObrigatoriaException;
import br.com.farmacia.domain.venda.exception.CpfObrigatorioException;
import br.com.farmacia.domain.venda.entity.Venda;
import br.com.farmacia.domain.venda.enums.FormaPagamento;
import br.com.farmacia.domain.venda.repository.VendaRepository;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link RealizarVendaUseCase}.
 *
 * <p><b>Heurística Júlio de Lima</b>: cobertura por comportamento esperado,
 * não por linha de código. Cada bloco @Nested representa um estado ou
 * condição de entrada diferente, com o objetivo de documentar
 * o que o sistema faz em cada situação.</p>
 *
 * <p>Cenários cobertos (heurística SFDIPOT):</p>
 * <ul>
 *   <li><b>Structure</b>  — objetos nulos, relacionamentos ausentes</li>
 *   <li><b>Function</b>   — regras PMC, FEFO, receita obrigatória, SNGPC</li>
 *   <li><b>Data</b>       — valores-limite (quantidade 0, preço = PMC)</li>
 *   <li><b>Integration</b>— chamadas a repositórios e SNGPC verificadas</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RealizarVendaUseCase — Fluxo Completo de Venda Farmacêutica")
class RealizarVendaUseCaseTest {

    @Mock private VendaRepository            vendaRepository;
    @Mock private MedicamentoRepository      medicamentoRepository;
    @Mock private LoteRepository             loteRepository;
    @Mock private EstoqueRepository          estoqueRepository;
    @Mock private ReceitaRepository          receitaRepository;
    @Mock private ClienteRepository          clienteRepository;
    @Mock private CaixaRepository            caixaRepository;
    @Mock private EnviarRegistroSNGPCUseCase enviarSNGPC;

    @InjectMocks
    private RealizarVendaUseCase sut;

    private UUID pdvId;
    private UUID funcionarioId;
    private Caixa caixaAberta;

    @BeforeEach
    void setUp() {
        pdvId        = UUID.randomUUID();
        funcionarioId = UUID.randomUUID();
        caixaAberta  = criarCaixaAberta();

        when(caixaRepository.findCaixaAbertoPorPdv(pdvId))
            .thenReturn(Optional.of(caixaAberta));
        when(vendaRepository.save(any())).thenAnswer(inv -> {
            Venda v = inv.getArgument(0);
            if (v.getId() == null) {
                v.atribuirId(UUID.randomUUID());
            }
            return v;
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VENDA SIMPLES — CAMINHO FELIZ
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que todos os dados da venda estão corretos")
    class DadoQueTodosOsDadosEstaoCorretos {

        @Test
        @DisplayName("deve finalizar venda com sucesso para medicamento livre")
        void deve_finalizar_venda_com_sucesso_para_medicamento_livre() {
            // ARRANGE
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre().build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();

            configurarMocksParaVendaSimples(med, lote);

            var input = montarInputVendaSimples(med, lote, BigDecimal.TEN, new BigDecimal("10.00"));

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output).isNotNull();
            assertThat(output.vendaId()).isNotNull();
            assertThat(output.total()).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(output.numeroCupom()).startsWith("CUP-");

            verify(vendaRepository).save(any(Venda.class));
            verify(loteRepository).save(any(Lote.class));
            verify(estoqueRepository).decrementarSaldo(med.getId(), 1);
        }

        @Test
        @DisplayName("deve reduzir o estoque do lote após a venda")
        void deve_reduzir_estoque_do_lote_apos_venda() {
            // ARRANGE
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre().build();
            Lote lote = LoteBuilder.umLoteDisponivel()
                .comQuantidadeAtual(50)
                .comMedicamento(med)
                .build();

            configurarMocksParaVendaSimples(med, lote);

            var input = montarInputVendaSimples(med, lote, BigDecimal.TEN, new BigDecimal("10.00"));

            // ACT
            sut.executar(input);

            // ASSERT — captura o lote salvo e verifica o saldo
            ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
            verify(loteRepository).save(loteCaptor.capture());

            assertThat(loteCaptor.getValue().getQuantidadeAtual())
                .as("Estoque deve ser decrementado em 1")
                .isEqualTo(49);
        }

        @Test
        @DisplayName("deve marcar lote como ESGOTADO quando última unidade é vendida")
        void deve_marcar_lote_esgotado_quando_ultima_unidade_e_vendida() {
            // ARRANGE — lote com exatamente 1 unidade
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre().build();
            Lote lote = LoteBuilder.umLoteComUmaUnidade().comMedicamento(med).build();

            configurarMocksParaVendaSimples(med, lote);

            var input = montarInputVendaSimples(med, lote, BigDecimal.TEN, new BigDecimal("10.00"));

            // ACT
            sut.executar(input);

            // ASSERT
            ArgumentCaptor<Lote> captor = ArgumentCaptor.forClass(Lote.class);
            verify(loteRepository).save(captor.capture());

            assertThat(captor.getValue().getStatus().name())
                .as("Lote com 0 unidades deve ter status ESGOTADO")
                .isEqualTo("ESGOTADO");
        }

        @Test
        @DisplayName("deve aceitar preço exatamente igual ao PMC (valor de borda)")
        void deve_aceitar_preco_exatamente_igual_ao_PMC() {
            // ARRANGE — preço = PMC exato (R$ 12,50 = R$ 12,50)
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre()
                .comPmc(new BigDecimal("12.50")).build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();

            configurarMocksParaVendaSimples(med, lote);

            var input = montarInputVendaSimples(med, lote,
                new BigDecimal("12.50"),   // preço = PMC exato
                new BigDecimal("12.50"));

            // ACT + ASSERT — não deve lançar exceção
            assertThatCode(() -> sut.executar(input)).doesNotThrowAnyException();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGRA PMC
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que o preço viola o PMC (ANVISA)")
    class DadoQueOPrecoViolaOPMC {

        @Test
        @DisplayName("deve lançar PrecoAcimaPMCException quando preço excede o PMC")
        void deve_lancar_exception_quando_preco_excede_pmc() {
            // ARRANGE — PMC = 12,50 / Preço cobrado = 15,00
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre()
                .comPmc(new BigDecimal("12.50")).build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();

            configurarMocksParaVendaSimples(med, lote);

            var input = montarInputVendaSimples(med, lote,
                new BigDecimal("15.00"),   // preço acima do PMC
                new BigDecimal("15.00"));

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(PrecoAcimaPMCException.class)
                .hasMessageContaining("PMC")
                .hasMessageContaining("12.50");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGRA RECEITA OBRIGATÓRIA
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que o medicamento requer receita")
    class DadoQueOMedicamentoRequerReceita {

        @Test
        @DisplayName("deve lançar ReceitaObrigatoriaException quando não há receita")
        void deve_lancar_exception_quando_nao_ha_receita() {
            // ARRANGE
            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();

            when(medicamentoRepository.findById(med.getId()))
                .thenReturn(Optional.of(med));
            when(loteRepository.findLotesDisponivelFefo(med.getId()))
                .thenReturn(List.of(lote));

            var input = new RealizarVendaUseCase.Input(
                pdvId, funcionarioId,
                null,      // sem cliente
                null,      // SEM receita
                List.of(new RealizarVendaUseCase.Input.ItemInput(
                    med.getId(), 1, BigDecimal.TEN, BigDecimal.ZERO)),
                List.of(new RealizarVendaUseCase.Input.PagamentoInput(
                    FormaPagamento.DINHEIRO, BigDecimal.TEN)),
                null, null
            );

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(ReceitaObrigatoriaException.class)
                .hasMessageContaining(med.getNomeComercial());
        }

        @Test
        @DisplayName("deve lançar ReceitaNaoAprovadaException quando receita está pendente")
        void deve_lancar_exception_quando_receita_nao_esta_aprovada() {
            // ARRANGE
            UUID receitaId = UUID.randomUUID();
            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();
            Receita receitaPendente = ReceitaBuilder.umaReceita().comId(receitaId).build(); // PENDENTE

            when(medicamentoRepository.findById(med.getId())).thenReturn(Optional.of(med));
            when(loteRepository.findLotesDisponivelFefo(med.getId())).thenReturn(List.of(lote));
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receitaPendente));

            var input = new RealizarVendaUseCase.Input(
                pdvId, funcionarioId, null, receitaId,
                List.of(new RealizarVendaUseCase.Input.ItemInput(
                    med.getId(), 1, BigDecimal.TEN, BigDecimal.ZERO)),
                List.of(new RealizarVendaUseCase.Input.PagamentoInput(
                    FormaPagamento.DINHEIRO, BigDecimal.TEN)),
                null, null
            );

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(ReceitaNaoAprovadaException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGRA CPF OBRIGATÓRIO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que o CPF é obrigatório para o medicamento")
    class DadoQueOCpfEObrigatorio {

        @Test
        @DisplayName("deve lançar CpfObrigatorioException para medicamento controlado sem CPF")
        void deve_lancar_exception_para_controlado_sem_cpf() {
            // ARRANGE
            UUID receitaId = UUID.randomUUID();
            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();
            Receita receita = ReceitaBuilder.umaReceitaAzulAprovada().comId(receitaId).build();

            when(medicamentoRepository.findById(med.getId())).thenReturn(Optional.of(med));
            when(loteRepository.findLotesDisponivelFefo(med.getId())).thenReturn(List.of(lote));
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receita));

            var input = new RealizarVendaUseCase.Input(
                pdvId, funcionarioId, null, receitaId,
                List.of(new RealizarVendaUseCase.Input.ItemInput(
                    med.getId(), 1, BigDecimal.TEN, BigDecimal.ZERO)),
                List.of(new RealizarVendaUseCase.Input.PagamentoInput(
                    FormaPagamento.DINHEIRO, BigDecimal.TEN)),
                null, null  // CPF nulo
            );

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(CpfObrigatorioException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGRA ESTOQUE FEFO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que há múltiplos lotes disponíveis")
    class DadoQueHaMultiplosLotes {

        @Test
        @DisplayName("deve selecionar o lote com validade mais próxima (FEFO)")
        void deve_selecionar_lote_com_validade_mais_proxima() {
            // ARRANGE — dois lotes: um vence em 60 dias, outro em 180
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre().build();

            Lote loteMaisAntigo = LoteBuilder.umLote()
                .comNumeroLote("LOT-ANTIGO")
                .comDataValidade(java.time.LocalDate.now().plusDays(60))  // vence primeiro
                .comQuantidadeAtual(10)
                .comMedicamento(med)
                .build();

            Lote loteMaisNovo = LoteBuilder.umLote()
                .comNumeroLote("LOT-NOVO")
                .comDataValidade(java.time.LocalDate.now().plusDays(180))
                .comQuantidadeAtual(10)
                .comMedicamento(med)
                .build();

            when(medicamentoRepository.findById(med.getId())).thenReturn(Optional.of(med));
            // FEFO: retorna na ordem correta (mais próximo primeiro)
            when(loteRepository.findLotesDisponivelFefo(med.getId()))
                .thenReturn(List.of(loteMaisAntigo, loteMaisNovo));

            var input = montarInputVendaSimples(med, loteMaisAntigo,
                BigDecimal.TEN, BigDecimal.TEN);

            // ACT
            sut.executar(input);

            // ASSERT — o lote mais antigo (que vence primeiro) deve ter sido salvo (decrementado)
            ArgumentCaptor<Lote> captor = ArgumentCaptor.forClass(Lote.class);
            verify(loteRepository).save(captor.capture());

            assertThat(captor.getValue().getNumeroLote())
                .as("Deve selecionar o lote LOT-ANTIGO (FEFO)")
                .isEqualTo("LOT-ANTIGO");
        }

        @Test
        @DisplayName("deve lançar EstoqueInsuficienteException quando não há lotes disponíveis")
        void deve_lancar_exception_quando_nao_ha_lotes_disponiveis() {
            // ARRANGE
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre().build();
            when(medicamentoRepository.findById(med.getId())).thenReturn(Optional.of(med));
            when(loteRepository.findLotesDisponivelFefo(med.getId()))
                .thenReturn(List.of()); // nenhum lote

            var input = montarInputVendaSimples(med, null, BigDecimal.TEN, BigDecimal.TEN);

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(EstoqueInsuficienteException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGRA SNGPC ASSÍNCRONO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que a venda contém medicamento controlado")
    class DadoQueAVendaContemControlado {

        @Test
        @DisplayName("deve publicar registro SNGPC após venda bem-sucedida")
        void deve_publicar_sngpc_apos_venda_de_controlado() {
            // ARRANGE
            UUID receitaId = UUID.randomUUID();
            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();
            Receita receita = ReceitaBuilder.umaReceitaAzulAprovada().comId(receitaId).build();

            when(medicamentoRepository.findById(med.getId())).thenReturn(Optional.of(med));
            when(loteRepository.findLotesDisponivelFefo(med.getId())).thenReturn(List.of(lote));
            when(receitaRepository.findById(receitaId)).thenReturn(Optional.of(receita));
            when(receitaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(estoqueRepository).decrementarSaldo(any(), anyInt());
            doNothing().when(estoqueRepository).salvarMovimentacao(any());

            var input = new RealizarVendaUseCase.Input(
                pdvId, funcionarioId, null, receitaId,
                List.of(new RealizarVendaUseCase.Input.ItemInput(
                    med.getId(), 1, BigDecimal.TEN, BigDecimal.ZERO)),
                List.of(new RealizarVendaUseCase.Input.PagamentoInput(
                    FormaPagamento.DINHEIRO, BigDecimal.TEN)),
                "João da Silva", "12345678901"
            );

            // ACT
            sut.executar(input);

            // ASSERT — SNGPC deve ter sido chamado exatamente 1 vez
            verify(enviarSNGPC, times(1))
                .publicarNaFila(any(EnviarRegistroSNGPCUseCase.Input.class));
        }

        @Test
        @DisplayName("não deve publicar SNGPC para medicamento livre")
        void nao_deve_publicar_sngpc_para_medicamento_livre() {
            // ARRANGE
            Medicamento med = MedicamentoBuilder.umMedicamentoLivre().build();
            Lote lote = LoteBuilder.umLoteDisponivel().comMedicamento(med).build();

            configurarMocksParaVendaSimples(med, lote);

            var input = montarInputVendaSimples(med, lote, BigDecimal.TEN, BigDecimal.TEN);

            // ACT
            sut.executar(input);

            // ASSERT
            verifyNoInteractions(enviarSNGPC);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGRA CAIXA FECHADO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que o caixa está fechado")
    class DadoQueOCaixaEstaFechado {

        @Test
        @DisplayName("deve lançar CaixaFechadoException quando não há caixa aberto")
        void deve_lancar_exception_quando_nao_ha_caixa_aberto() {
            // ARRANGE
            when(caixaRepository.findCaixaAbertoPorPdv(pdvId))
                .thenReturn(Optional.empty()); // sem caixa aberto

            var input = new RealizarVendaUseCase.Input(
                pdvId, funcionarioId, null, null,
                List.of(), List.of(), null, null
            );

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(CaixaFechadoException.class)
                .hasMessageContaining(pdvId.toString());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarMocksParaVendaSimples(Medicamento med, Lote lote) {
        when(medicamentoRepository.findById(med.getId())).thenReturn(Optional.of(med));
        if (lote != null) {
            when(loteRepository.findLotesDisponivelFefo(med.getId()))
                .thenReturn(List.of(lote));
        }
        doNothing().when(estoqueRepository).decrementarSaldo(any(), anyInt());
        doNothing().when(estoqueRepository).salvarMovimentacao(any());
        when(loteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private RealizarVendaUseCase.Input montarInputVendaSimples(
            Medicamento med, Lote lote, BigDecimal preco, BigDecimal pagamento) {

        return new RealizarVendaUseCase.Input(
            pdvId, funcionarioId,
            null, null,
            List.of(new RealizarVendaUseCase.Input.ItemInput(
                med.getId(), 1, preco, BigDecimal.ZERO)),
            List.of(new RealizarVendaUseCase.Input.PagamentoInput(
                FormaPagamento.DINHEIRO, pagamento)),
            null, null
        );
    }

    private Caixa criarCaixaAberta() {
        PDV pdv = PDV.builder().id(pdvId).numero("PDV-01").build();
        return Caixa.builder()
            .id(UUID.randomUUID())
            .pdv(pdv)
            .saldoAbertura(BigDecimal.ZERO)
            .totalVendas(BigDecimal.ZERO)
            .build();
    }
}
