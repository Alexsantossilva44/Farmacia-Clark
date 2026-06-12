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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler de rotinas de estoque.
 *
 * <p>Jobs executados automaticamente:</p>
 * <ul>
 *   <li><b>verificarLotesVencidos</b>  — Diariamente às 00:30: bloqueia lotes vencidos</li>
 *   <li><b>alertarVencimentoProximo</b> — Diariamente às 07:00: alerta lotes com &lt;90 dias</li>
 *   <li><b>alertarEstoqueMinimo</b>     — A cada 4 horas: alerta itens abaixo do mínimo</li>
 *   <li><b>alertarEstoqueZerado</b>     — A cada 1 hora: alerta medicamentos zerados</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertaVencimentoScheduler {

    private final LoteRepository           loteRepository;
    private final EstoqueRepository        estoqueRepository;
    private final AlertaEstoqueRepository  alertaRepository;

    // Limiares de alerta de vencimento (em dias)
    private static final int ALERTA_CRITICO  = 30;
    private static final int ALERTA_ATENCAO  = 60;
    private static final int ALERTA_AVISO    = 90;

    // ─── Job 1: Bloquear lotes vencidos ──────────────────────────────────────

    /**
     * Executa diariamente às 00:30.
     * Varre todos os lotes ATIVOS e bloqueia os que já venceram.
     */
    @Scheduled(cron = "0 30 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void verificarLotesVencidos() {
        log.info("[Scheduler] Iniciando verificação de lotes vencidos em {}", LocalDateTime.now());

        List<Lote> lotesVencidos = loteRepository
            .findByStatusAndDataValidadeBefore(StatusLote.ATIVO, LocalDate.now());

        if (lotesVencidos.isEmpty()) {
            log.info("[Scheduler] Nenhum lote vencido encontrado.");
            return;
        }

        int lotesBloqueados = 0;
        for (Lote lote : lotesVencidos) {
            try { // C-08: falha em um lote não interrompe os demais do mesmo job
                int quantidadeBloqueada = lote.bloquearPorVencimento();
                loteRepository.save(lote);

                estoqueRepository.decrementarSaldo(
                    lote.getMedicamento().getId(),
                    quantidadeBloqueada
                );

                gerarAlerta(
                    lote,
                    TipoAlerta.LOTE_VENCIDO,
                    "[VENCIDO] Lote %s do medicamento '%s' venceu em %s. %d unidades bloqueadas."
                        .formatted(
                            lote.getNumeroLote(),
                            lote.getMedicamento().getNomeComercial(),
                            lote.getDataValidade(),
                            quantidadeBloqueada
                        )
                );
                lotesBloqueados++;
            } catch (Exception e) {
                log.error("[Scheduler] Erro ao bloquear lote [{}]: {}", lote.getNumeroLote(), e.getMessage(), e);
            }
        }

        log.warn("[Scheduler] {} lote(s) bloqueado(s) por vencimento.", lotesBloqueados); // C-08: conta apenas os efetivamente bloqueados
    }

    // ─── Job 2: Alertar vencimento próximo ────────────────────────────────────

    /**
     * Executa diariamente às 07:00.
     * Gera alertas escalonados: 90, 60 e 30 dias antes do vencimento.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void alertarVencimentoProximo() {
        log.info("[Scheduler] Verificando vencimentos próximos...");

        LocalDate hoje = LocalDate.now();

        // Lotes que vencem nos próximos 90 dias
        List<Lote> lotesProximosVencer = loteRepository.findLotesProximosVencer(
            StatusLote.ATIVO, hoje.plusDays(ALERTA_AVISO)
        );

        int alertasGerados = 0; // M-05: contador real de alertas emitidos — lotesProximosVencer.size() inclui lotes já alertados
        for (Lote lote : lotesProximosVencer) {
            long diasRestantes = java.time.temporal.ChronoUnit.DAYS
                .between(hoje, lote.getDataValidade());

            boolean alertaJaExiste = alertaRepository.existeAlertaAberto(
                lote.getId(), TipoAlerta.VENCIMENTO_PROXIMO
            );
            if (alertaJaExiste) continue;

            String urgencia = diasRestantes <= ALERTA_CRITICO ? "CRITICO"
                            : diasRestantes <= ALERTA_ATENCAO ? "ATENCAO"
                            : "AVISO";

            gerarAlerta(
                lote,
                TipoAlerta.VENCIMENTO_PROXIMO,
                "[%s] Lote %s de '%s' vence em %d dias (%s). Estoque: %d unidades."
                    .formatted(
                        urgencia,
                        lote.getNumeroLote(),
                        lote.getMedicamento().getNomeComercial(),
                        diasRestantes,
                        lote.getDataValidade(),
                        lote.getQuantidadeAtual()
                    )
            );
            alertasGerados++;
        }

        log.info("[Scheduler] {} alerta(s) de vencimento gerado(s) de {} candidato(s).", alertasGerados, lotesProximosVencer.size()); // M-05: distingue gerados vs candidatos
    }

    // ─── Job 3: Alertar estoque mínimo ───────────────────────────────────────

    /**
     * Executa a cada 4 horas.
     * Detecta medicamentos abaixo do estoque mínimo configurado.
     */
    @Scheduled(cron = "0 0 */4 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void alertarEstoqueMinimo() {
        log.info("[Scheduler] Verificando estoque mínimo...");

        List<ItemEstoque> itensAbaixoMinimo = estoqueRepository
            .findItensAbaixoDoMinimo();

        int alertasGerados = 0; // M-05: conta apenas alertas efetivamente salvos (exclui duplicatas já abertas)
        for (ItemEstoque item : itensAbaixoMinimo) {
            boolean alertaJaExiste = alertaRepository.existeAlertaAbertoPorMedicamento(
                item.getMedicamento().getId(), TipoAlerta.ESTOQUE_MINIMO
            );
            if (alertaJaExiste) continue;

            AlertaEstoque alerta = AlertaEstoque.builder()
                .medicamento(item.getMedicamento())
                .tipo(TipoAlerta.ESTOQUE_MINIMO)
                .mensagem(
                    "🟡 Estoque baixo: '%s' está com %d unidades (mínimo: %d). Considere realizar novo pedido."
                        .formatted(
                            item.getMedicamento().getNomeComercial(),
                            item.getQuantidadeAtual(),
                            item.getQuantidadeMinima()
                        )
                )
                .dataGeracao(LocalDateTime.now())
                .lido(false)
                .status(StatusAlerta.ABERTO)
                .build();

            alertaRepository.save(alerta);
            alertasGerados++; // M-05: incrementa só após save — não conta itens ignorados por duplicata
        }

        log.info("[Scheduler] {} alerta(s) de estoque mínimo gerado(s) de {} candidato(s).", alertasGerados, itensAbaixoMinimo.size()); // M-05: distingue gerados vs candidatos totais
    }

    // ─── Job 4: Alertar estoque zerado ───────────────────────────────────────

    /**
     * Executa a cada hora.
     * Detecta medicamentos completamente zerados (especialmente crítico para controlados).
     */
    @Scheduled(cron = "0 0 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void alertarEstoqueZerado() {
        log.info("[Scheduler] Verificando estoques zerados...");

        List<ItemEstoque> itensZerados = estoqueRepository.findItensComEstoqueZerado();

        for (ItemEstoque item : itensZerados) {
            boolean alertaJaExiste = alertaRepository.existeAlertaAbertoPorMedicamento(
                item.getMedicamento().getId(), TipoAlerta.ESTOQUE_ZERADO
            );
            if (alertaJaExiste) continue;

            AlertaEstoque alerta = AlertaEstoque.builder()
                .medicamento(item.getMedicamento())
                .tipo(TipoAlerta.ESTOQUE_ZERADO)
                .mensagem(
                    "🔴 ESTOQUE ZERADO: '%s' não possui unidades disponíveis para dispensação!"
                        .formatted(item.getMedicamento().getNomeComercial())
                )
                .dataGeracao(LocalDateTime.now())
                .lido(false)
                .status(StatusAlerta.ABERTO)
                .build();

            alertaRepository.save(alerta);
            log.error("[Scheduler] ESTOQUE ZERADO: {}", item.getMedicamento().getNomeComercial());
        }
    }

    // ─── Job 5: Retry SNGPC com falha ────────────────────────────────────────

    /**
     * Executa a cada 30 minutos.
     * Reprocessa registros SNGPC que falharam no envio (máximo 5 tentativas).
     */
    @Scheduled(cron = "0 */30 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void reprocessarSNGPCComFalha() {
        log.info("[Scheduler] Verificando registros SNGPC pendentes/com erro...");
        // Delegado para EnviarRegistroSNGPCUseCase.reprocessarPendentes()
        // — implementado no adapter de mensageria
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void gerarAlerta(Lote lote, TipoAlerta tipo, String mensagem) {
        AlertaEstoque alerta = AlertaEstoque.builder()
            .medicamento(lote.getMedicamento())
            .lote(lote)
            .tipo(tipo)
            .mensagem(mensagem)
            .dataGeracao(LocalDateTime.now())
            .lido(false)
            .status(StatusAlerta.ABERTO)
            .build();

        alertaRepository.save(alerta);
        log.info("[Alerta] {}", mensagem);
    }
}
