package br.com.farmacia.qa.bdd.steps;

import br.com.farmacia.domain.estoque.entity.AlertaEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import br.com.farmacia.domain.estoque.repository.AlertaEstoqueRepository;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.infrastructure.scheduler.AlertaVencimentoScheduler;
import br.com.farmacia.qa.bdd.BddTestContext;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.*;
import br.com.farmacia.qa.seed.IntegracaoTestSeed;
import io.cucumber.java.Before;
import io.cucumber.java.pt.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Step Definitions para alertas automáticos de estoque.
 *
 * <p><b>Heurística Júlio de Lima</b>: steps de schedulers invocam o método
 * diretamente (não esperam o cron). O seed {@link IntegracaoTestSeed} garante
 * FKs consistentes entre medicamento, lote e item de estoque.</p>
 */
public class AlertasEstoqueVencimentoSteps {

    @Autowired private AlertaVencimentoScheduler scheduler;
    @Autowired private LoteRepository            loteRepository;
    @Autowired private AlertaEstoqueRepository   alertaRepository;
    @Autowired private MedicamentoRepository     medicamentoRepository;
    @Autowired private EstoqueRepository         estoqueRepository;
    @Autowired private IntegracaoTestSeed        testSeed;
    @Autowired private BddTestContext            bddContext;

    private Lote loteAtual() {
        return bddContext.getLoteAtual();
    }

    private Medicamento medicamentoAtual() {
        return bddContext.getMedicamentoAtual();
    }

    private String numeroLoteAtual() {
        return bddContext.getNumeroLoteAtual();
    }

    @Before("@alertas")
    public void prepararCatalogo() {
        bddContext.limpar();
        testSeed.semearCatalogoBase();
    }

    // ══════════════════════════════════════════════════════════════════════
    // GIVEN
    // ══════════════════════════════════════════════════════════════════════

    @Dado("que existe o lote {string} do medicamento {string}")
    public void queExisteOLote(String numeroLote, String nomeMed) {
        bddContext.setNumeroLoteAtual(numeroLote);
        bddContext.setMedicamentoAtual(testSeed.garantirMedicamento(nomeMed, b -> {
            if ("Amoxicilina 500mg".equals(nomeMed)) {
                return MedicamentoBuilder.umMedicamentoComReceitaSimples();
            }
            return b;
        }));
    }

    @Dado("que esse lote tinha validade para ontem e tinha {int} unidades disponíveis")
    public void queLoteTinhaValidadeParaOntem(int quantidade) {
        bddContext.setLoteAtual(testSeed.garantirLote(
            numeroLoteAtual(),
            medicamentoAtual(),
            LocalDate.now().minusDays(1),
            quantidade,
            StatusLote.ATIVO
        ));
    }

    @Dado("que o lote {string} vence em {int} dias")
    public void queLoteVenceEmXDiasSemMedicamento(String numeroLote, int dias) {
        bddContext.setNumeroLoteAtual(numeroLote);
        bddContext.setMedicamentoAtual(testSeed.garantirMedicamento("Rivotril 2mg",
            b -> MedicamentoBuilder.umMedicamentoControlado()));

        bddContext.setLoteAtual(testSeed.garantirLote(
            numeroLote,
            medicamentoAtual(),
            LocalDate.now().plusDays(dias),
            30,
            StatusLote.ATIVO
        ));
    }

    @Dado("que não existe alerta aberto para esse lote")
    public void queNaoExisteAlertaAberto() {
        alertaRepository.deleteAllByLoteId(loteAtual().getId());
    }

    @Dado("que o lote {string} já tem um alerta {string} em aberto")
    public void queLoteJaTemAlertaEmAberto(String numeroLote, String tipoAlerta) {
        bddContext.setNumeroLoteAtual(numeroLote);
        Lote lote = loteRepository.findByNumeroLote(numeroLote).orElseGet(() ->
            testSeed.garantirLote(
                numeroLote,
                testSeed.garantirMedicamento("Rivotril 2mg", b -> MedicamentoBuilder.umMedicamentoControlado()),
                LocalDate.now().plusDays(15),
                30,
                StatusLote.ATIVO
            ));
        bddContext.setLoteAtual(lote);
        bddContext.setMedicamentoAtual(lote.getMedicamento());

        alertaRepository.deleteAllByLoteId(lote.getId());

        AlertaEstoque alertaExistente = AlertaEstoque.builder()
            .lote(lote)
            .medicamento(lote.getMedicamento())
            .tipo(TipoAlerta.valueOf(tipoAlerta))
            .mensagem("Alerta existente de teste")
            .status(StatusAlerta.ABERTO)
            .lido(false)
            .dataGeracao(java.time.LocalDateTime.now())
            .build();
        alertaRepository.save(alertaExistente);
    }

    @Dado("que {string} tem estoque mínimo configurado como {int} unidades")
    public void queMedicamentoTemEstoqueMinimoConfigurado(String nomeMed, int minimo) {
        temEstoqueMinimoConfigurado(nomeMed, minimo);
    }

    @Dado("{string} tem estoque mínimo configurado como {int} unidades")
    public void temEstoqueMinimoConfigurado(String nomeMed, int minimo) {
        bddContext.setMedicamentoAtual(testSeed.garantirMedicamento(nomeMed, b ->
            "Dipirona 500mg".equals(nomeMed)
                ? b.comNomeComercial("Dipirona 500mg")
                : b));
        testSeed.garantirItemEstoque(bddContext.getMedicamentoAtual().getId(), 0, minimo);
    }

    @Dado("que o estoque atual é de {int} unidades")
    public void queOEstoqueAtualEDe(int quantidade) {
        testSeed.garantirItemEstoque(
            medicamentoAtual().getId(),
            quantidade,
            estoqueRepository.findByMedicamentoId(medicamentoAtual().getId())
                .map(item -> item.getQuantidadeMinima())
                .orElse(5)
        );
    }

    @Dado("que não existe alerta de estoque mínimo aberto para {string}")
    public void queNaoExisteAlertaDeEstoqueMinimoAberto(String nomeMed) {
        alertaRepository.deleteAllByMedicamentoNomeAndTipo(nomeMed, TipoAlerta.ESTOQUE_MINIMO);
    }

    @Dado("que o estoque de {string} está zerado")
    public void queOEstoqueEstaZerado(String nomeMed) {
        bddContext.setMedicamentoAtual(testSeed.garantirMedicamento(nomeMed, b ->
            "Morfina 10mg".equals(nomeMed)
                ? MedicamentoBuilder.umMedicamentoEntorpecente()
                : b));
        testSeed.garantirItemEstoque(bddContext.getMedicamentoAtual().getId(), 0, 5);
    }

    @Dado("que não existe alerta de estoque zerado aberto")
    public void queNaoExisteAlertaDeEstoqueZeradoAberto() {
        alertaRepository.deleteAllByMedicamentoId(medicamentoAtual().getId());
    }

    // ══════════════════════════════════════════════════════════════════════
    // WHEN
    // ══════════════════════════════════════════════════════════════════════

    @Quando("o scheduler de verificação de vencimentos é executado")
    public void schedulerVerificacaoVencimentosExecutado() {
        scheduler.verificarLotesVencidos();
    }

    @Quando("o scheduler de alertas de vencimento é executado")
    public void schedulerAlertasVencimentoExecutado() {
        scheduler.alertarVencimentoProximo();
    }

    @Quando("o scheduler de alertas de vencimento é executado novamente")
    public void schedulerAlertasVencimentoExecutadoNovamente() {
        scheduler.alertarVencimentoProximo();
    }

    @Quando("o scheduler de estoque mínimo é executado")
    public void schedulerEstoqueMinimoExecutado() {
        scheduler.alertarEstoqueMinimo();
    }

    @Quando("o scheduler de estoque zerado é executado")
    public void schedulerEstoqueZeradoExecutado() {
        scheduler.alertarEstoqueZerado();
    }

    // ══════════════════════════════════════════════════════════════════════
    // THEN
    // ══════════════════════════════════════════════════════════════════════

    @Entao("o lote {string} deve ter status alterado para {string}")
    public void loteDeverTerStatusAlterado(String numeroLote, String statusEsperado) {
        Lote loteVerificado = loteRepository.findByNumeroLote(numeroLote).orElseThrow();
        assertThat(loteVerificado.getStatus().name())
            .as("Status do lote deve ser " + statusEsperado)
            .isEqualTo(statusEsperado);
    }

    @Entao("as {int} unidades devem ser removidas do saldo consolidado")
    public void unidadesDevemSerRemovidasDoSaldo(int qtd) {
        Lote loteVerificado = loteRepository.findById(loteAtual().getId()).orElseThrow();
        assertThat(loteVerificado.getQuantidadeAtual())
            .as("Saldo do lote deve ser zerado após bloqueio por vencimento")
            .isEqualTo(0);
    }

    @Entao("um alerta do tipo {string} deve ser gerado para o gerente")
    public void umAlertaDoTipoDeveSerGerado(String tipoAlerta) {
        List<AlertaEstoque> alertas = alertaRepository
            .findByLoteIdAndTipo(loteAtual().getId(), TipoAlerta.valueOf(tipoAlerta));

        assertThat(alertas)
            .as("Deve existir pelo menos um alerta do tipo " + tipoAlerta)
            .isNotEmpty();
    }

    @Entao("o lote não deve mais aparecer como disponível para dispensação")
    public void loteNaoDeveAparecerComoDisponivel() {
        List<Lote> lotesDisponiveis = loteRepository
            .findLotesDisponivelFefo(medicamentoAtual().getId());

        assertThat(lotesDisponiveis)
            .as("Lote vencido não deve aparecer na lista de disponíveis")
            .noneMatch(l -> l.getId().equals(loteAtual().getId()));
    }

    @Entao("deve ser gerado um alerta com urgência {string} para {string}")
    public void deveSerGeradoAlertaComUrgencia(String urgencia, String numeroLote) {
        assertThat(deveSerGeradoAlertaComUrgenciaInterno(urgencia)).isTrue();
    }

    @Entao("deve ser gerado um alerta com urgência {string}")
    public void deveSerGeradoAlertaComUrgenciaSimples(String urgencia) {
        assertThat(deveSerGeradoAlertaComUrgenciaInterno(urgencia)).isTrue();
    }

    private boolean deveSerGeradoAlertaComUrgenciaInterno(String urgencia) {
        List<AlertaEstoque> alertas = alertaRepository
            .findByLoteIdAndTipo(loteAtual().getId(), TipoAlerta.VENCIMENTO_PROXIMO);

        assertThat(alertas)
            .as("Deve ter alerta de vencimento próximo")
            .isNotEmpty();

        assertThat(alertas.get(0).getMensagem()).containsIgnoringCase(urgencia);
        return true;
    }

    @Entao("o alerta deve estar com status {string}")
    public void alertaDeveEstarComStatus(String status) {
        List<AlertaEstoque> alertas = alertaRepository
            .findByMedicamentoId(medicamentoAtual().getId());

        assertThat(alertas)
            .allMatch(a -> a.getStatus().name().equals(status));
    }

    @Entao("o alerta deve estar não lido")
    public void alertaDeveEstarNaoLido() {
        List<AlertaEstoque> alertas = alertaRepository
            .findByMedicamentoId(medicamentoAtual().getId());

        assertThat(alertas)
            .allMatch(a -> Boolean.FALSE.equals(a.getLido()));
    }

    @Entao("nenhum novo alerta deve ser criado para {string}")
    public void nenhumNovoAlertaDeveSer(String numeroLote) {
        long totalAlertas = alertaRepository
            .countByLoteIdAndTipo(loteAtual().getId(), TipoAlerta.VENCIMENTO_PROXIMO);

        assertThat(totalAlertas)
            .as("Deve haver apenas 1 alerta (o pré-existente), sem duplicidade")
            .isEqualTo(1L);
    }

    @Entao("deve ser gerado um alerta do tipo {string}")
    public void deveSerGeradoAlertaDoTipo(String tipoAlerta) {
        List<AlertaEstoque> alertas = alertaRepository
            .findByMedicamentoIdAndTipo(medicamentoAtual().getId(), TipoAlerta.valueOf(tipoAlerta));

        assertThat(alertas).isNotEmpty();
    }

    @Entao("a mensagem do alerta deve informar {string}")
    public void aMensagemDoAlertaDeveInformar(String trecho) {
        List<AlertaEstoque> alertas = alertaRepository
            .findByMedicamentoId(medicamentoAtual().getId());

        assertThat(alertas)
            .isNotEmpty()
            .anyMatch(a -> a.getMensagem().contains(trecho));
    }

    @Entao("a mensagem deve conter {string}")
    public void aMensagemDeveConter(String texto) {
        List<AlertaEstoque> alertas = alertaRepository
            .findByMedicamentoId(medicamentoAtual().getId());

        assertThat(alertas)
            .anyMatch(a -> a.getMensagem().toUpperCase().contains(texto.toUpperCase()));
    }
}
