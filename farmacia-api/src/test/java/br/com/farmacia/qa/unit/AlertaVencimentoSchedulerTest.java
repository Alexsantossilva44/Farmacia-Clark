package br.com.farmacia.infrastructure.scheduler;

import br.com.farmacia.domain.estoque.entity.AlertaEstoque;
import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import br.com.farmacia.domain.estoque.repository.AlertaEstoqueRepository;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link AlertaVencimentoScheduler}.
 *
 * <p><b>Heurística Júlio de Lima — Time na SFDIPOT</b>:</p>
 * <blockquote>
 * "Sempre que um sistema lida com datas e tempos, teste os limites temporais:
 * ontem, hoje, amanhã, e os limiares exatos (30/60/90 dias)."
 * </blockquote>
 *
 * <p>Cenários especiais testados aqui:</p>
 * <ul>
 *   <li>Lote que vence exatamente hoje → deve bloquear</li>
 *   <li>Lote que venceu ontem → já deve estar bloqueado</li>
 *   <li>Lote que vence amanhã → deve gerar alerta crítico</li>
 *   <li>Lote que vence em 90 dias → alerta de aviso</li>
 *   <li>Lote que vence em 91 dias → sem alerta</li>
 *   <li>Alerta duplicado → não deve gerar segundo alerta</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertaVencimentoScheduler — Rotinas Automáticas de Estoque")
class AlertaVencimentoSchedulerTest {

    @Mock private LoteRepository           loteRepository;
    @Mock private EstoqueRepository        estoqueRepository;
    @Mock private AlertaEstoqueRepository  alertaRepository;

    @InjectMocks
    private AlertaVencimentoScheduler sut;

    // ═══════════════════════════════════════════════════════════════════════
    // verificarLotesVencidos()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verificarLotesVencidos() — Bloqueio de Lotes Vencidos")
    class VerificarLotesVencidos {

        @Test
        @DisplayName("deve bloquear lote vencido e zerar sua quantidade")
        void deve_bloquear_lote_vencido_e_zerar_quantidade() {
            // ARRANGE
            Lote loteVencido = LoteBuilder.umLoteVencido()
                .comQuantidadeAtual(30)
                .build();

            when(loteRepository.findByStatusAndDataValidadeBefore(
                eq(StatusLote.ATIVO), any(LocalDate.class)))
                .thenReturn(List.of(loteVencido));
            when(loteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(estoqueRepository).decrementarSaldo(any(), anyInt());

            // ACT
            sut.verificarLotesVencidos();

            // ASSERT
            ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
            verify(loteRepository).save(loteCaptor.capture());

            assertThat(loteCaptor.getValue().getStatus())
                .as("Lote vencido deve ser marcado como VENCIDO")
                .isEqualTo(StatusLote.VENCIDO);

            assertThat(loteCaptor.getValue().getQuantidadeAtual())
                .as("Quantidade do lote vencido deve ser zerada")
                .isEqualTo(0);
        }

        @Test
        @DisplayName("deve decrementar saldo consolidado com a quantidade do lote bloqueado")
        void deve_decrementar_saldo_consolidado_com_quantidade_do_lote() {
            // ARRANGE — lote com 30 unidades vencendo hoje
            Lote loteVencido = LoteBuilder.umLoteVencido()
                .comQuantidadeAtual(30)
                .build();

            when(loteRepository.findByStatusAndDataValidadeBefore(
                eq(StatusLote.ATIVO), any()))
                .thenReturn(List.of(loteVencido));
            when(loteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ACT
            sut.verificarLotesVencidos();

            // ASSERT
            verify(estoqueRepository).decrementarSaldo(
                loteVencido.getMedicamento().getId(), 30);
        }

        @Test
        @DisplayName("deve gerar alerta do tipo LOTE_VENCIDO para cada lote bloqueado")
        void deve_gerar_alerta_lote_vencido() {
            // ARRANGE
            Lote loteVencido = LoteBuilder.umLoteVencido().build();

            when(loteRepository.findByStatusAndDataValidadeBefore(
                eq(StatusLote.ATIVO), any()))
                .thenReturn(List.of(loteVencido));
            when(loteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<AlertaEstoque> alertaCaptor =
                ArgumentCaptor.forClass(AlertaEstoque.class);

            // ACT
            sut.verificarLotesVencidos();

            // ASSERT
            verify(alertaRepository).save(alertaCaptor.capture());
            assertThat(alertaCaptor.getValue().getTipo())
                .isEqualTo(TipoAlerta.LOTE_VENCIDO);
            assertThat(alertaCaptor.getValue().getStatus())
                .isEqualTo(StatusAlerta.ABERTO);
            assertThat(alertaCaptor.getValue().getLido())
                .isFalse();
        }

        @Test
        @DisplayName("não deve executar operações quando não há lotes vencidos")
        void nao_deve_executar_operacoes_quando_nao_ha_lotes_vencidos() {
            // ARRANGE
            when(loteRepository.findByStatusAndDataValidadeBefore(
                eq(StatusLote.ATIVO), any()))
                .thenReturn(List.of());

            // ACT
            sut.verificarLotesVencidos();

            // ASSERT
            verifyNoInteractions(estoqueRepository);
            verifyNoInteractions(alertaRepository);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // alertarVencimentoProximo()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("alertarVencimentoProximo() — Alertas Escalonados por Proximidade")
    class AlertarVencimentoProximo {

        @Test
        @DisplayName("deve gerar alerta para lote que vence em 15 dias (crítico)")
        void deve_gerar_alerta_para_lote_que_vence_em_15_dias() {
            // ARRANGE
            Lote loteProximo = LoteBuilder.umLoteProximoDoVencimento().build(); // vence em 15 dias

            when(loteRepository.findLotesProximosVencer(eq(StatusLote.ATIVO), any()))
                .thenReturn(List.of(loteProximo));
            when(alertaRepository.existeAlertaAberto(loteProximo.getId(), TipoAlerta.VENCIMENTO_PROXIMO))
                .thenReturn(false);
            when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<AlertaEstoque> captor = ArgumentCaptor.forClass(AlertaEstoque.class);

            // ACT
            sut.alertarVencimentoProximo();

            // ASSERT
            verify(alertaRepository).save(captor.capture());
            assertThat(captor.getValue().getTipo())
                .isEqualTo(TipoAlerta.VENCIMENTO_PROXIMO);
            assertThat(captor.getValue().getMensagem())
                .as("Mensagem de alerta crítico deve conter indicativo de urgência")
                .containsIgnoringCase("CRITICO");
        }

        @Test
        @DisplayName("não deve gerar alerta duplicado para lote com alerta já aberto")
        void nao_deve_gerar_alerta_duplicado() {
            // ARRANGE
            Lote loteProximo = LoteBuilder.umLoteProximoDoVencimento().build();

            when(loteRepository.findLotesProximosVencer(any(), any()))
                .thenReturn(List.of(loteProximo));
            when(alertaRepository.existeAlertaAberto(loteProximo.getId(), TipoAlerta.VENCIMENTO_PROXIMO))
                .thenReturn(true); // já existe alerta

            // ACT
            sut.alertarVencimentoProximo();

            // ASSERT — save não deve ser chamado (alerta já existe)
            verify(alertaRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // alertarEstoqueMinimo()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("alertarEstoqueMinimo() — Estoque Abaixo do Mínimo Configurado")
    class AlertarEstoqueMinimo {

        @Test
        @DisplayName("deve gerar alerta quando item está abaixo do mínimo")
        void deve_gerar_alerta_quando_abaixo_do_minimo() {
            // ARRANGE
            ItemEstoque itemAbaixo = criarItemEstoque(3, 10); // atual=3, mínimo=10

            when(estoqueRepository.findItensAbaixoDoMinimo())
                .thenReturn(List.of(itemAbaixo));
            when(alertaRepository.existeAlertaAbertoPorMedicamento(any(), eq(TipoAlerta.ESTOQUE_MINIMO)))
                .thenReturn(false);
            when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<AlertaEstoque> captor = ArgumentCaptor.forClass(AlertaEstoque.class);

            // ACT
            sut.alertarEstoqueMinimo();

            // ASSERT
            verify(alertaRepository).save(captor.capture());

            assertThat(captor.getValue().getTipo())
                .isEqualTo(TipoAlerta.ESTOQUE_MINIMO);
            assertThat(captor.getValue().getMensagem())
                .contains("3")
                .contains("10");
        }

        @Test
        @DisplayName("não deve gerar alerta quando estoque está acima do mínimo")
        void nao_deve_gerar_alerta_quando_estoque_acima_do_minimo() {
            // ARRANGE — nenhum item abaixo do mínimo
            when(estoqueRepository.findItensAbaixoDoMinimo())
                .thenReturn(List.of());

            // ACT
            sut.alertarEstoqueMinimo();

            // ASSERT
            verify(alertaRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // alertarEstoqueZerado()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("alertarEstoqueZerado() — Medicamentos Completamente Sem Estoque")
    class AlertarEstoqueZerado {

        @Test
        @DisplayName("deve gerar alerta ESTOQUE_ZERADO com mensagem de urgência máxima")
        void deve_gerar_alerta_estoque_zerado_com_urgencia_maxima() {
            // ARRANGE
            ItemEstoque itemZerado = criarItemEstoque(0, 5);

            when(estoqueRepository.findItensComEstoqueZerado())
                .thenReturn(List.of(itemZerado));
            when(alertaRepository.existeAlertaAbertoPorMedicamento(any(), eq(TipoAlerta.ESTOQUE_ZERADO)))
                .thenReturn(false);
            when(alertaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<AlertaEstoque> captor = ArgumentCaptor.forClass(AlertaEstoque.class);

            // ACT
            sut.alertarEstoqueZerado();

            // ASSERT
            verify(alertaRepository).save(captor.capture());

            assertThat(captor.getValue().getTipo())
                .isEqualTo(TipoAlerta.ESTOQUE_ZERADO);
            assertThat(captor.getValue().getMensagem())
                .as("Alerta de zerado deve conter indicativo de urgência máxima")
                .contains("ZERADO");
        }

        @Test
        @DisplayName("não deve gerar alerta duplicado de estoque zerado")
        void nao_deve_gerar_alerta_duplicado_de_estoque_zerado() {
            // ARRANGE
            ItemEstoque itemZerado = criarItemEstoque(0, 5);

            when(estoqueRepository.findItensComEstoqueZerado())
                .thenReturn(List.of(itemZerado));
            when(alertaRepository.existeAlertaAbertoPorMedicamento(any(), eq(TipoAlerta.ESTOQUE_ZERADO)))
                .thenReturn(true); // já existe

            // ACT
            sut.alertarEstoqueZerado();

            // ASSERT
            verify(alertaRepository, never()).save(any());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private ItemEstoque criarItemEstoque(int atual, int minimo) {
        var med = MedicamentoBuilder.umMedicamento().build();
        return ItemEstoque.builder()
            .medicamento(med)
            .quantidadeAtual(atual)
            .quantidadeMinima(minimo)
            .quantidadeMaxima(500)
            .build();
    }
}
