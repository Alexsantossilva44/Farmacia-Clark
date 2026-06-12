package br.com.farmacia.infrastructure.bootstrap;

import br.com.farmacia.application.financeiro.usecase.AbrirCaixaUseCase;
import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import br.com.farmacia.domain.financeiro.repository.PdvRepository;
import br.com.farmacia.domain.funcionario.repository.FuncionarioRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Seed idempotente de catálogo, estoque e caixa aberto para desenvolvimento local.
 *
 * <p>Permite que o front-end ({@code farmacia-web}) realize vendas via
 * {@code POST /api/v1/vendas} sem depender do seed de testes.</p>
 */
@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class DevOperacionalSeed implements ApplicationRunner {

    static final UUID FABRICANTE_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID CATEGORIA_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID MEDICAMENTO_DIPIRONA_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID MEDICAMENTO_PARACETAMOL_ID =
        UUID.fromString("55555555-5555-5555-5555-555555555555");
    static final UUID PRESCRITOR_DEV_ID =
        UUID.fromString("66666666-6666-6666-6666-666666666666");

    static final UUID FORNECEDOR_DEV_ID =
        UUID.fromString("77777777-7777-7777-7777-777777777777");

    static final String PDV_01_NUMERO = "PDV-01";

    private final JdbcTemplate jdbcTemplate;
    private final MedicamentoRepository medicamentoRepository;
    private final EstoqueRepository estoqueRepository;
    private final LoteRepository loteRepository;
    private final PdvRepository pdvRepository;
    private final CaixaRepository caixaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final AbrirCaixaUseCase abrirCaixaUseCase;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        semearCatalogo();
        semearFornecedor();
        semearPrescritor();
        semearEstoque(MEDICAMENTO_DIPIRONA_ID, "Dipirona Dev 500mg", "DEV-DIP-001", new BigDecimal("12.50"));
        semearEstoque(MEDICAMENTO_PARACETAMOL_ID, "Paracetamol Dev 750mg", "DEV-PAR-001", new BigDecimal("18.90"));
        semearCaixaAberto();
        log.info("[dev] Ambiente operacional pronto — PDV-01 com caixa aberto e estoque seed");
    }

    private void semearCatalogo() {
        jdbcTemplate.update("""
            INSERT INTO fabricantes (id, razao_social, nome_fantasia, cnpj, ativo)
            VALUES (?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """, FABRICANTE_ID, "Laboratório Dev S.A.", "LabDev", "00000000000191");

        jdbcTemplate.update("""
            INSERT INTO categorias (id, nome, ativo)
            VALUES (?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """, CATEGORIA_ID, "Analgésicos");

        jdbcTemplate.update("""
            INSERT INTO medicamentos
                (id, codigo_ean, codigo_anvisa, nome_comercial, nome_generico, tipo,
                 forma_farmaceutica, concentracao, apresentacao, requer_receita, nivel_controle,
                 preco_maximo_consumidor, fabricante_id, categoria_id, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """,
            MEDICAMENTO_DIPIRONA_ID, "7891000000001", "100000000000001",
            "Dipirona Dev 500mg", "Dipirona Monoidratada", "GENERICO", "COMPRIMIDO",
            "500mg", "Caixa 20 comprimidos", false, "LIVRE", new BigDecimal("12.50"),
            FABRICANTE_ID, CATEGORIA_ID);

        jdbcTemplate.update("""
            INSERT INTO medicamentos
                (id, codigo_ean, codigo_anvisa, nome_comercial, nome_generico, tipo,
                 forma_farmaceutica, concentracao, apresentacao, requer_receita, nivel_controle,
                 preco_maximo_consumidor, fabricante_id, categoria_id, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """,
            MEDICAMENTO_PARACETAMOL_ID, "7891000000002", "100000000000002",
            "Paracetamol Dev 750mg", "Paracetamol", "GENERICO", "COMPRIMIDO",
            "750mg", "Caixa 10 comprimidos", false, "LIVRE", new BigDecimal("18.90"),
            FABRICANTE_ID, CATEGORIA_ID);
    }

    private void semearFornecedor() {
        jdbcTemplate.update("""
            INSERT INTO fornecedores (id, razao_social, nome_fantasia, cnpj, ativo)
            VALUES (?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """, FORNECEDOR_DEV_ID, "Distribuidora Dev Ltda", "DistDev", "00000000000272");
    }

    private void semearPrescritor() {
        jdbcTemplate.update("""
            INSERT INTO prescritores (id, nome, crm, uf_crm, especialidade, ativo)
            VALUES (?, ?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """, PRESCRITOR_DEV_ID, "Dr. Seed Dev", "12345", "SP", "Clínica Geral");
    }

    private void semearEstoque(UUID medicamentoId, String nomeComercial, String numeroLote,
                               BigDecimal precoPmc) {
        Medicamento med = medicamentoRepository.findById(medicamentoId)
            .orElseGet(() -> medicamentoRepository.findByNomeComercial(nomeComercial)
                .orElseThrow(() -> new IllegalStateException("Medicamento seed ausente: " + nomeComercial)));

        estoqueRepository.findByMedicamentoId(med.getId())
            .map(item -> estoqueRepository.salvar(ItemEstoque.builder()
                .id(item.getId())
                .medicamento(med)
                .quantidadeAtual(200)
                .quantidadeMinima(10)
                .quantidadeMaxima(item.getQuantidadeMaxima())
                .ultimaMovimentacao(item.getUltimaMovimentacao())
                .build()))
            .orElseGet(() -> estoqueRepository.salvar(ItemEstoque.builder()
                .medicamento(med)
                .quantidadeAtual(200)
                .quantidadeMinima(10)
                .quantidadeMaxima(500)
                .build()));

        LocalDate validade = LocalDate.now().plusMonths(18);
        loteRepository.findByNumeroLote(numeroLote)
            .map(lote -> loteRepository.save(Lote.builder()
                .id(lote.getId())
                .notaFiscalId(lote.getNotaFiscalId())
                .medicamento(med)
                .numeroLote(numeroLote)
                .dataFabricacao(lote.getDataFabricacao())
                .dataValidade(validade)
                .quantidadeRecebida(lote.getQuantidadeRecebida())
                .quantidadeAtual(200)
                .precoCusto(lote.getPrecoCusto())
                .status(StatusLote.ATIVO)
                .build()))
            .orElseGet(() -> loteRepository.save(Lote.builder()
                .medicamento(med)
                .numeroLote(numeroLote)
                .dataFabricacao(LocalDate.now().minusMonths(2))
                .dataValidade(validade)
                .quantidadeRecebida(200)
                .quantidadeAtual(200)
                .precoCusto(precoPmc.multiply(new BigDecimal("0.6")))
                .status(StatusLote.ATIVO)
                .build()));
    }

    private void semearCaixaAberto() {
        var pdv = pdvRepository.findByNumero(PDV_01_NUMERO)
            .orElseThrow(() -> new IllegalStateException("PDV-01 não encontrado (migration V5)"));

        if (caixaRepository.findCaixaAbertoPorPdv(pdv.getId()).isPresent()) {
            return;
        }

        var balconista = funcionarioRepository.findByEmail(DevAmbienteSeed.BALCONISTA_EMAIL)
            .or(() -> funcionarioRepository.findByEmail(DevAmbienteSeed.ADMIN_EMAIL))
            .orElseThrow(() -> new IllegalStateException(
                "Nenhum funcionário dev encontrado — verifique DevAmbienteSeed"));

        abrirCaixaUseCase.executar(new AbrirCaixaUseCase.Input(
            pdv.getId(),
            balconista.getId(),
            BigDecimal.ZERO
        ));
    }
}
