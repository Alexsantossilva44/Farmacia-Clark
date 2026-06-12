package br.com.farmacia.qa.bdd.steps;

import br.com.farmacia.application.receituario.usecase.ValidarReceitaUseCase;
import br.com.farmacia.application.sngpc.usecase.EnviarRegistroSNGPCUseCase;
import br.com.farmacia.application.sngpc.usecase.SngpcEventPublisher;
import br.com.farmacia.application.venda.usecase.RealizarVendaUseCase;
import br.com.farmacia.qa.bdd.BddTestContext;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.domain.receituario.entity.Prescritor;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.enums.TipoReceita;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.*;
import br.com.farmacia.qa.seed.IntegracaoTestSeed;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.pt.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.com.farmacia.qa.seed.IntegracaoSeedReferencia.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Step Definitions para venda de medicamento controlado no PDV.
 *
 * <p>Usa {@link IntegracaoTestSeed} para FKs estáveis (PDV, caixa, farmacêutico,
 * prescritor, medicamento controlado e lotes).</p>
 */
public class VendaMedicamentoControladoSteps {

    @Autowired private RealizarVendaUseCase    realizarVendaUseCase;
    @Autowired private ValidarReceitaUseCase  validarReceitaUseCase;
    @Autowired private MedicamentoRepository  medicamentoRepository;
    @Autowired private ReceitaRepository      receitaRepository;
    @Autowired private LoteRepository          loteRepository;
    @Autowired private IntegracaoTestSeed      testSeed;
    @Autowired private SngpcEventPublisher     sngpcEventPublisher;
    @Autowired private BddTestContext          bddContext;

    private UUID pdvId;
    private UUID farmaceuticoId;
    private UUID balconistaId;
    private Medicamento medicamentoAtual;
    private Lote loteAtual;
    private Receita receitaAtual;
    private UUID receitaId;

    private RealizarVendaUseCase.Output vendaOutput;
    private ValidarReceitaUseCase.Output validacaoOutput;
    private Exception excecaoCapturada;

    private BigDecimal precoUnitario;
    private int quantidadeVenda = 1;
    private int quantidadeValidacao = 1;
    private String compradorCpf;
    private BigDecimal valorPagamento;

    private final List<Lote> lotesParaFefo = new ArrayList<>();
    private int quantidadeLoteAntesVenda = -1;
    private String numeroLoteVenda;

    @Before("@controlado")
    public void prepararAmbienteControlado() {
        limparEstado();
        bddContext.limpar();
        testSeed.semearAmbienteCompleto();
        pdvId = testSeed.obterPdvIdPorNumero(PDV_01_NUMERO);
        farmaceuticoId = testSeed.obterFarmaceuticoIdPorCrf(FARMACEUTICO_CRF);
        balconistaId = testSeed.obterBalconistaId();
    }

    private void limparEstado() {
        vendaOutput = null;
        validacaoOutput = null;
        excecaoCapturada = null;
        receitaAtual = null;
        receitaId = null;
        medicamentoAtual = null;
        loteAtual = null;
        precoUnitario = null;
        quantidadeVenda = 1;
        quantidadeValidacao = 1;
        compradorCpf = null;
        valorPagamento = null;
        lotesParaFefo.clear();
        quantidadeLoteAntesVenda = -1;
        numeroLoteVenda = null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // GIVEN
    // ══════════════════════════════════════════════════════════════════════

    @Dado("que o PDV {string} está com o caixa aberto")
    public void quePdvEstaComCaixaAberto(String numeroPdv) {
        assertThat(numeroPdv).isEqualTo(PDV_01_NUMERO);
        assertThat(pdvId).isEqualTo(testSeed.obterPdvIdPorNumero(numeroPdv));
    }

    @Dado("que o farmacêutico {string} está disponível para validação")
    public void queFarmaceuticoEstaDisponivel(String crf) {
        assertThat(crf).isEqualTo(FARMACEUTICO_CRF);
        assertThat(farmaceuticoId).isNotNull();
    }

    @Dado("que o medicamento controlado {string} está cadastrado com nível {string}")
    public void queMedicamentoControladoEstaCadastrado(String nome, String nivelControle) {
        medicamentoAtual = testSeed.garantirMedicamentoControlado(nome, 2);
        testSeed.zerarLotesDoMedicamento(medicamentoAtual.getId());
        assertThat(medicamentoAtual.getNivelControle().name()).isEqualTo(nivelControle);
        medicamentoAtual = medicamentoRepository.findById(medicamentoAtual.getId()).orElseThrow();
    }

    @Dado("que há {int} unidades do lote {string} com validade para {string}")
    public void queHaUnidadesDoLote(int quantidade, String numeroLote, String dataValidade) {
        LocalDate validade = LocalDate.parse(dataValidade);
        if (!validade.isAfter(LocalDate.now())) {
            validade = LocalDate.now().plusMonths(18);
        }
        loteAtual = testSeed.garantirLote(
            numeroLote,
            medicamentoAtual,
            validade,
            quantidade,
            StatusLote.ATIVO
        );
    }

    @Dado("que o cliente {string} apresenta receita Azul número {string}")
    public void queClienteApresentaReceitaAzul(String nomeCliente, String numeroReceita) {
        criarReceitaPendente(numeroReceita, TipoReceita.AZUL, "Dr. Ricardo Gomes", "54321/SP");
    }

    @Dado("que o cliente {string} apresenta receita Simples número {string}")
    public void queClienteApresentaReceitaSimples(String nomeCliente, String numeroReceita) {
        criarReceitaPendente(numeroReceita, TipoReceita.SIMPLES, "Dr. Pedro Alves", "11111/SP");
    }

    @Dado("que a receita foi emitida há {int} dias pelo Dr. {string} CRM {string}")
    public void queReceitaFoiEmitidaHaDiasPeloDr(int dias, String nomeDr, String crm) {
        Prescritor prescritor = testSeed.garantirPrescritor(nomeDr, crm);
        LocalDate dataEmissao = LocalDate.now().minusDays(dias);
        receitaAtual = receitaRepository.save(reconstruirReceita(receitaAtual)
            .prescritor(prescritor)
            .dataEmissao(dataEmissao)
            .dataValidade(dataEmissao.plusDays(receitaAtual.getTipo().getValidadeDias()))
            .build());
        receitaId = receitaAtual.getId();
    }

    @Dado("que a receita foi emitida há {int} dias e está válida")
    public void queReceitaFoiEmitidaHaDiasEValida(int dias) {
        LocalDate dataEmissao = LocalDate.now().minusDays(dias);
        receitaAtual = receitaRepository.save(reconstruirReceita(receitaAtual)
            .dataEmissao(dataEmissao)
            .dataValidade(dataEmissao.plusDays(receitaAtual.getTipo().getValidadeDias()))
            .build());
        receitaId = receitaAtual.getId();
    }

    @Dado("que a receita foi emitida há {int} dias \\(vencida — validade máxima {int} dias\\)")
    public void queReceitaFoiEmitidaHaXDiasVencida(int diasEmitida, int validadeMaxima) {
        Prescritor prescritor = receitaAtual.getPrescritor() != null
            ? receitaAtual.getPrescritor()
            : testSeed.garantirPrescritor("Dr. Seed", "99999/SP");
        receitaAtual = receitaRepository.save(reconstruirReceita(receitaAtual)
            .prescritor(prescritor)
            .dataEmissao(LocalDate.now().minusDays(diasEmitida))
            .dataValidade(LocalDate.now().minusDays(1))
            .tipo(TipoReceita.AZUL)
            .build());
        receitaId = receitaAtual.getId();
    }

    @Dado("que a receita foi aprovada pelo farmacêutico {string}")
    public void queReceitaFoiAprovada(String crf) {
        aprovarReceitaAtual(quantidadeVenda);
    }

    @Dado("que a receita Azul {string} foi aprovada pelo farmacêutico")
    public void queReceitaAzulFoiAprovadaPeloFarmaceutico(String numeroReceita) {
        criarReceitaPendente(numeroReceita, TipoReceita.AZUL, "Dr. Seed", "88888/SP");
        aprovarReceitaAtual(1);
    }

    @Dado("que a receita Azul {string} foi aprovada")
    public void queReceitaAzulFoiAprovada(String numeroReceita) {
        criarReceitaPendente(numeroReceita, TipoReceita.AZUL, "Dr. Seed", "77777/SP");
        aprovarReceitaAtual(1);
    }

    @Dado("que a receita Azul {string} foi aprovada para {int} unidades")
    public void queReceitaAzulFoiAprovadaParaUnidades(String numeroReceita, int unidades) {
        if (unidades > 2) {
            testSeed.ajustarQuantidadeMaximaReceita(medicamentoAtual.getId(), unidades);
            medicamentoAtual = medicamentoRepository.findById(medicamentoAtual.getId()).orElseThrow();
        }
        criarReceitaPendente(numeroReceita, TipoReceita.AZUL, "Dr. Seed", "66666/SP");
        quantidadeVenda = unidades;
        aprovarReceitaAtual(unidades);
    }

    @Dado("que o {string} tem PMC definido em R$ {bigdecimal}")
    public void queMedicamentoTemPmcDefinido(String nomeMed, BigDecimal pmc) {
        medicamentoAtual.definirPrecoMaximoConsumidor(pmc);
        medicamentoAtual = medicamentoRepository.save(medicamentoAtual);
    }

    @Dado("que há dois lotes disponíveis de {string}:")
    public void queHaDoisLotesDisponiveis(String nomeMedicamento, DataTable dataTable) {
        lotesParaFefo.clear();
        List<Map<String, String>> linhas = dataTable.asMaps();
        for (int i = 0; i < linhas.size(); i++) {
            Map<String, String> linha = linhas.get(i);
            // Validades relativas preservam a ordem FEFO independente da data do cenário Gherkin
            LocalDate validade = LocalDate.now().plusDays(30L * (i + 1));
            Lote lote = testSeed.garantirLote(
                linha.get("Lote"),
                medicamentoAtual,
                validade,
                Integer.parseInt(linha.get("Quantidade")),
                StatusLote.ATIVO
            );
            lotesParaFefo.add(lote);
        }
    }

    @Dado("que a {string} permite no máximo {int} embalagens por receita de {string}")
    public void quePortariaPermiteMaximo(String portaria, int maximo, String nomeMed) {
        medicamentoAtual = testSeed.garantirMedicamentoControlado(nomeMed, maximo);
        medicamentoAtual = medicamentoRepository.findById(medicamentoAtual.getId()).orElseThrow();
    }

    @Dado("que o Dr. {string} emitiu receita para {int} embalagens")
    public void queDrEmitiuReceitaParaEmbalagens(String nomeDr, int embalagens) {
        criarReceitaPendente("REC-PORTARIA-" + embalagens, TipoReceita.AZUL, nomeDr, "55555/SP");
        quantidadeValidacao = embalagens;
    }

    @Dado("que há apenas {int} unidades disponíveis de {string}")
    public void queHaApenasUnidadesDisponiveis(int quantidade, String nomeMed) {
        loteAtual = testSeed.garantirLote(
            loteAtual.getNumeroLote(),
            medicamentoAtual,
            loteAtual.getDataValidade(),
            quantidade,
            loteAtual.getStatus()
        );
    }

    @Dado("há apenas {int} unidades disponíveis de {string}")
    public void haApenasUnidadesDisponiveis(int quantidade, String nomeMed) {
        queHaApenasUnidadesDisponiveis(quantidade, nomeMed);
    }

    // ══════════════════════════════════════════════════════════════════════
    // WHEN
    // ══════════════════════════════════════════════════════════════════════

    @Quando("o balconista registra a venda de {int} unidade de {string} por R$ {bigdecimal}")
    public void balconistaRegistraVenda(int quantidade, String nomeMed, BigDecimal preco) {
        quantidadeVenda = quantidade;
        precoUnitario = preco;
    }

    @Quando("informa o CPF do comprador {string}")
    public void informaCpfComprador(String cpf) {
        compradorCpf = cpf.replaceAll("[^0-9]", "");
    }

    @Quando("registra o pagamento de R$ {bigdecimal} em dinheiro")
    public void registraPagamento(BigDecimal valor) {
        valorPagamento = valor;
        executarVenda();
    }

    @Quando("o farmacêutico tenta validar a receita {string}")
    public void farmaceuticoValidaReceita(String numeroReceita) {
        validarReceitaComTratamentoDeErro(1);
    }

    @Quando("o farmacêutico tenta validar a receita {string} para {string}")
    public void farmaceuticoValidaReceitaParaMedicamento(String numeroReceita, String nomeMed) {
        validarReceitaComTratamentoDeErro(1);
    }

    @Quando("o farmacêutico valida a receita")
    public void farmaceuticoValidaReceita() {
        validarReceitaComTratamentoDeErro(quantidadeValidacao);
    }

    @Quando("o balconista tenta finalizar a venda sem informar o CPF do comprador")
    public void tentaFinalizarSemCpf() {
        compradorCpf = null;
        valorPagamento = new BigDecimal("45.90");
        precoUnitario = new BigDecimal("45.90");
        executarVenda();
    }

    @Quando("o balconista tenta registrar a venda de {int} unidade por R$ {bigdecimal}")
    public void tentaRegistrarVendaComPreco(int quantidade, BigDecimal preco) {
        quantidadeVenda = quantidade;
        precoUnitario = preco;
        valorPagamento = preco;
        executarVenda();
    }

    @Quando("o balconista tenta finalizar a venda de {int} unidades")
    public void tentaFinalizarVendaDeUnidades(int quantidade) {
        quantidadeVenda = quantidade;
        precoUnitario = new BigDecimal("45.90");
        valorPagamento = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        compradorCpf = "12345678901";
        executarVenda();
    }

    @Quando("o balconista realiza a venda de {int} unidade")
    public void realizaVendaDeUmaUnidade(int quantidade) {
        quantidadeVenda = quantidade;
        precoUnitario = new BigDecimal("45.90");
        valorPagamento = new BigDecimal("50.00");
        compradorCpf = "12345678901";
        executarVenda();
        assertThat(excecaoCapturada)
            .as("Venda deveria ter sido executada sem exceção")
            .isNull();
    }

    @Quando("o balconista finaliza a venda normalmente com CPF {string}")
    public void finalizaVendaComCpf(String cpf) {
        compradorCpf = cpf.replaceAll("[^0-9]", "");
        precoUnitario = new BigDecimal("45.90");
        valorPagamento = new BigDecimal("50.00");
        numeroLoteVenda = bddContext.getNumeroLoteAtual() != null
            ? bddContext.getNumeroLoteAtual()
            : "LOT-VENCENDO";
        quantidadeLoteAntesVenda = loteRepository.findByNumeroLote(numeroLoteVenda)
            .map(Lote::getQuantidadeAtual)
            .orElse(-1);
        executarVenda();
    }

    // ══════════════════════════════════════════════════════════════════════
    // THEN
    // ══════════════════════════════════════════════════════════════════════

    @Entao("a venda deve ser finalizada com sucesso")
    public void aVendaDeveSerFinalizadaComSucesso() {
        assertThat(excecaoCapturada).isNull();
        assertThat(vendaOutput).isNotNull();
        assertThat(vendaOutput.vendaId()).isNotNull();
    }

    @Entao("o cupom fiscal deve ser gerado")
    public void oCupomFiscalDeveSerGerado() {
        assertThat(vendaOutput.numeroCupom())
            .isNotBlank()
            .startsWith("CUP-");
    }

    @Entao("o estoque do lote {string} deve ser decrementado para {int} unidades")
    public void estoqueDoLoteDeveSerDecrementado(String numeroLote, int qtdEsperada) {
        Lote loteAtualizado = buscarLotePorNumero(numeroLote);
        assertThat(loteAtualizado.getQuantidadeAtual()).isEqualTo(qtdEsperada);
    }

    @Entao("a receita {string} deve ser marcada como {string} e retida")
    public void receitaDeveSerMarcadaComoUtilizadaERetida(String numero, String status) {
        Receita receitaAtualizada = receitaRepository.findById(receitaId).orElseThrow();
        assertThat(receitaAtualizada.getStatus().name()).isEqualTo(status);
        assertThat(receitaAtualizada.getRetida()).isTrue();
    }

    @Entao("a receita deve ser rejeitada")
    public void aReceitaDeveSerRejeitada() {
        if (validacaoOutput != null) {
            assertThat(validacaoOutput.aprovada()).isFalse();
        } else {
            assertThat(excecaoCapturada).isNull();
        }
    }

    @Entao("a mensagem de erro deve conter {string}")
    public void aMensagemDeErroDeveConter(String textoEsperado) {
        if (validacaoOutput != null && !validacaoOutput.aprovada()) {
            assertThat(validacaoOutput.violacoes())
                .anyMatch(v -> v.toLowerCase().contains(textoEsperado.toLowerCase()));
        } else if (excecaoCapturada != null) {
            assertThat(excecaoCapturada.getMessage())
                .containsIgnoringCase(textoEsperado);
        } else {
            fail("Nenhuma violação ou exceção capturada para validar a mensagem");
        }
    }

    @Entao("a venda deve ser recusada")
    public void aVendaDeveSerRecusada() {
        assertThat(excecaoCapturada).isNotNull();
        assertThat(vendaOutput).isNull();
    }

    @Entao("a venda não deve ser finalizada")
    public void aVendaNaoDeveSerFinalizada() {
        assertThat(vendaOutput).isNull();
    }

    @Entao("a mensagem deve informar que {string}")
    public void aMensagemDeveInformarQue(String textoEsperado) {
        assertThat(excecaoCapturada).isNotNull();
        String msg = excecaoCapturada.getMessage().toLowerCase();
        String trecho = textoEsperado.toLowerCase()
            .replace("preço excede o pmc de r$ 45,90", "excede o pmc")
            .replace(",", ".");
        assertThat(msg).contains(trecho);
    }

    @Entao("a mensagem deve informar {string}")
    public void aMensagemDeveInformar(String textoEsperado) {
        if (validacaoOutput != null && !validacaoOutput.aprovada()) {
            assertThat(String.join(" ", validacaoOutput.violacoes()))
                .containsIgnoringCase(textoEsperado.replace("\"", ""));
        } else {
            assertThat(excecaoCapturada)
                .isNotNull()
                .satisfies(e -> assertThat(e.getMessage())
                    .containsIgnoringCase(textoEsperado.replace("\"", "")));
        }
    }

    @Entao("deve informar que o medicamento exige {string}")
    public void deveInformarQueMedicamentoExige(String tipoReceita) {
        assertThat(validacaoOutput).isNotNull();
        String esperado = tipoReceita.replace("receita ", "").trim();
        assertThat(validacaoOutput.violacoes())
            .anyMatch(v -> v.toLowerCase().contains(esperado.toLowerCase()));
    }

    @Entao("o estoque não deve ser alterado")
    public void oEstoqueNaoDeveSerAlterado() {
        Lote loteVerificado = loteRepository.findById(loteAtual.getId()).orElseThrow();
        assertThat(loteVerificado.getQuantidadeAtual()).isEqualTo(loteAtual.getQuantidadeAtual());
    }

    @Entao("o sistema deve descontar do lote {string} \\(mais próximo do vencimento\\)")
    public void sistemaDeveDescontarDoLoteMaisAntigo(String numeroLote) {
        Lote loteOriginal = lotesParaFefo.stream()
            .filter(l -> l.getNumeroLote().equals(numeroLote))
            .findFirst()
            .orElseThrow();
        Lote loteEsperado = loteRepository.findById(loteOriginal.getId()).orElseThrow();
        assertThat(loteEsperado.getQuantidadeAtual())
            .isEqualTo(loteOriginal.getQuantidadeAtual() - quantidadeVenda);
    }

    @Entao("o lote {string} não deve ser alterado")
    public void loteNaoDeveSerAlterado(String numeroLote) {
        Lote loteOriginal = lotesParaFefo.stream()
            .filter(l -> l.getNumeroLote().equals(numeroLote))
            .findFirst()
            .orElseThrow();
        Lote loteAtualizado = loteRepository.findById(loteOriginal.getId()).orElseThrow();
        assertThat(loteAtualizado.getQuantidadeAtual()).isEqualTo(loteOriginal.getQuantidadeAtual());
    }

    @Entao("a resposta deve conter um aviso {string}")
    public void aRespostaDeveConterAviso(String avisoEsperado) {
        String numeroLote = bddContext.getNumeroLoteAtual() != null
            ? bddContext.getNumeroLoteAtual().toLowerCase()
            : "lot-vencendo";
        assertThat(vendaOutput.avisos())
            .anyMatch(a -> {
                String msg = a.toLowerCase();
                return msg.contains(numeroLote) && msg.contains("10 dias");
            });
    }

    @Entao("o estoque deve ser decrementado normalmente")
    public void estoqueDeveSerDecrementadoNormalmente() {
        assertThat(vendaOutput).isNotNull();
        Lote lote = loteRepository.findByNumeroLote(numeroLoteVenda).orElseThrow();
        assertThat(lote.getQuantidadeAtual()).isEqualTo(quantidadeLoteAntesVenda - quantidadeVenda);
    }

    @Entao("o registro SNGPC deve ser enviado de forma assíncrona")
    public void registroSNGPCDeveSerEnviado() {
        assertThat(vendaOutput.vendaId()).isNotNull();
        verify(sngpcEventPublisher, atLeastOnce())
            .publicar(any(UUID.class), any(EnviarRegistroSNGPCUseCase.Input.class));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Lote buscarLotePorNumero(String numeroLote) {
        if (loteAtual != null && numeroLote.equals(loteAtual.getNumeroLote())) {
            return loteRepository.findById(loteAtual.getId()).orElseThrow();
        }
        return lotesParaFefo.stream()
            .filter(l -> l.getNumeroLote().equals(numeroLote))
            .findFirst()
            .map(l -> loteRepository.findById(l.getId()).orElseThrow())
            .orElseGet(() -> loteRepository.findByNumeroLote(numeroLote).orElseThrow());
    }

    private Receita.ReceitaBuilder reconstruirReceita(Receita receita) {
        return Receita.builder()
            .id(receita.getId())
            .numeroReceita(receita.getNumeroReceita())
            .dataEmissao(receita.getDataEmissao())
            .dataValidade(receita.getDataValidade())
            .tipo(receita.getTipo())
            .status(receita.getStatus())
            .cid(receita.getCid())
            .retida(receita.getRetida())
            .imagemPath(receita.getImagemPath())
            .motivoRejeicao(receita.getMotivoRejeicao())
            .prescritor(receita.getPrescritor())
            .cliente(receita.getCliente())
            .farmaceuticoId(receita.getFarmaceuticoId())
            .farmaceutico(receita.getFarmaceutico())
            .dataValidacao(receita.getDataValidacao());
    }

    private void criarReceitaPendente(String numero, TipoReceita tipo, String nomeDr, String crm) {
        Prescritor prescritor = testSeed.garantirPrescritor(nomeDr, crm);
        receitaAtual = ReceitaBuilder.umaReceita()
            .comNumero(numero)
            .comTipo(tipo)
            .comStatus(StatusReceita.PENDENTE)
            .comPrescritor(prescritor)
            .comEmissao(LocalDate.now().minusDays(3))
            .comValidade(LocalDate.now().plusDays(tipo.getValidadeDias() - 3))
            .build();
        receitaAtual = receitaRepository.save(receitaAtual);
        receitaId = receitaAtual.getId();
    }

    private void aprovarReceitaAtual(int quantidade) {
        var input = new ValidarReceitaUseCase.Input(
            receitaId,
            farmaceuticoId,
            List.of(new ValidarReceitaUseCase.Input.ItemValidacao(
                medicamentoAtual.getId(), quantidade))
        );
        validacaoOutput = validarReceitaUseCase.executar(input);
        assertThat(validacaoOutput.aprovada())
            .as("Receita deveria ser aprovada no Given")
            .isTrue();
    }

    private void validarReceitaComTratamentoDeErro(int quantidade) {
        try {
            var input = new ValidarReceitaUseCase.Input(
                receitaId,
                farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(
                    medicamentoAtual.getId(), quantidade))
            );
            validacaoOutput = validarReceitaUseCase.executar(input);
        } catch (Exception e) {
            excecaoCapturada = e;
        }
    }

    private void executarVenda() {
        try {
            var input = new RealizarVendaUseCase.Input(
                pdvId,
                balconistaId,
                null,
                receitaId,
                List.of(new RealizarVendaUseCase.Input.ItemInput(
                    medicamentoAtual.getId(),
                    quantidadeVenda,
                    precoUnitario,
                    BigDecimal.ZERO
                )),
                List.of(new RealizarVendaUseCase.Input.PagamentoInput(
                    br.com.farmacia.domain.venda.enums.FormaPagamento.DINHEIRO,
                    valorPagamento
                )),
                compradorCpf,
                compradorCpf != null ? "Comprador Teste" : null
            );
            vendaOutput = realizarVendaUseCase.executar(input);
        } catch (Exception e) {
            excecaoCapturada = e;
        }
    }
}
