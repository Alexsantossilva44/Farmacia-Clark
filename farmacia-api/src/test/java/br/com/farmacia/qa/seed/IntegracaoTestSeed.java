package br.com.farmacia.qa.seed;

import br.com.farmacia.application.financeiro.usecase.AbrirCaixaUseCase;
import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.enums.StatusPDV;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import br.com.farmacia.domain.financeiro.repository.PdvRepository;
import br.com.farmacia.domain.funcionario.entity.Cargo;
import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.funcionario.enums.RoleSistema;
import br.com.farmacia.domain.funcionario.repository.FarmaceuticoRepository;
import br.com.farmacia.domain.funcionario.repository.FuncionarioRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.entity.MedicamentoControlado;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.domain.receituario.entity.Prescritor;
import br.com.farmacia.infrastructure.persistence.funcionario.CargoJpaRepository;
import br.com.farmacia.infrastructure.persistence.receituario.PrescritorJpaRepository;
import br.com.farmacia.infrastructure.persistence.receituario.PrescritorPersistenceMapper;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.MedicamentoBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;

import static br.com.farmacia.qa.seed.IntegracaoSeedReferencia.*;

/**
 * Seed idempotente para testes de integração e BDD.
 *
 * <p>Garante que agregados referenciados por FK (fabricante → medicamento →
 * lote → item_estoque → pdv → caixa → funcionário) existam com ids fixos,
 * facilitando a evolução do front-end sobre contratos estáveis.</p>
 *
 * @author Alex Silva e Claude
 */
@Component
@RequiredArgsConstructor
public class IntegracaoTestSeed {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final CargoJpaRepository cargoJpaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FarmaceuticoRepository farmaceuticoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final EstoqueRepository estoqueRepository;
    private final LoteRepository loteRepository;
    private final PdvRepository pdvRepository;
    private final CaixaRepository caixaRepository;
    private final AbrirCaixaUseCase abrirCaixaUseCase;
    private final PrescritorJpaRepository prescritorJpaRepository;

    /**
     * Semeia catálogo base, funcionários e PDV-01 com caixa aberto.
     * Idempotente — pode ser chamado em {@code @BeforeEach} de ITs/BDD.
     */
    @Transactional
    public void semearAmbienteCompleto() {
        semearCatalogoBase();
        semearFuncionarios();
        semearPdvComCaixaAberto();
    }

    @Transactional
    public void semearCatalogoBase() {
        jdbcTemplate.update("""
            INSERT INTO fabricantes (id, razao_social, nome_fantasia, cnpj, ativo)
            VALUES (?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """, FABRICANTE_ID, "Laboratório Seed S.A.", "LabSeed", "00000000000191");

        jdbcTemplate.update("""
            INSERT INTO categorias (id, nome, ativo)
            VALUES (?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """, CATEGORIA_ID, "Analgésicos");

        jdbcTemplate.update("""
            INSERT INTO medicamentos
                (id, nome_comercial, tipo, requer_receita, nivel_controle,
                 preco_maximo_consumidor, fabricante_id, categoria_id, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)
            ON CONFLICT (id) DO NOTHING
            """, MEDICAMENTO_DIPIRONA_ID, "Dipirona Seed 500mg", "GENERICO",
            false, "LIVRE", new BigDecimal("12.50"), FABRICANTE_ID, CATEGORIA_ID);
    }

    @Transactional
    public void semearFuncionarios() {
        semearFuncionario(ADMIN_EMAIL, ADMIN_SENHA, RoleSistema.ROLE_ADMIN,
            "Administrador Seed", "00000000001");
        semearFuncionario(BALCONISTA_EMAIL, BALCONISTA_SENHA, RoleSistema.ROLE_BALCONISTA,
            "Balconista Seed", "00000000002");
        semearFuncionario(FARMACEUTICO_EMAIL, FARMACEUTICO_SENHA, RoleSistema.ROLE_FARMACEUTICO,
            "Farmacêutico Seed", "00000000003");
        // Mesmos papéis do DevAmbienteSeed — testes de permissão por role.
        semearFuncionario(GERENTE_EMAIL, GERENTE_SENHA, RoleSistema.ROLE_GERENTE,
            "Gerente Seed", "00000000004");
        semearFuncionario(ESTOQUISTA_EMAIL, ESTOQUISTA_SENHA, RoleSistema.ROLE_ESTOQUISTA,
            "Estoquista Seed", "00000000005");
        semearFarmaceutico(FARMACEUTICO_CRF);
    }

    @Transactional
    public void semearPdvComCaixaAberto() {
        PDV pdv = pdvRepository.findByNumero(PDV_01_NUMERO).orElseGet(() ->
            pdvRepository.save(PDV.builder()
                .id(PDV_01_ID)
                .numero(PDV_01_NUMERO)
                .descricao("PDV principal — seed de testes")
                .status(StatusPDV.FECHADO)
                .build()));

        var balconista = funcionarioRepository.findByEmail(BALCONISTA_EMAIL).orElseThrow();
        UUID pdvId = pdv.getId();

        if (caixaRepository.findCaixaAbertoPorPdv(pdvId).isEmpty()) {
            abrirCaixaUseCase.executar(new AbrirCaixaUseCase.Input(
                pdvId,
                balconista.getId(),
                BigDecimal.ZERO
            ));
        }
    }

    @Transactional
    public Medicamento garantirMedicamento(String nomeComercial,
                                           Function<MedicamentoBuilder, MedicamentoBuilder> customizador) {
        return medicamentoRepository.findByNomeComercial(nomeComercial)
            .orElseGet(() -> {
                MedicamentoBuilder builder = MedicamentoBuilder.umMedicamento()
                    .comNomeComercial(nomeComercial);
                if (customizador != null) {
                    builder = customizador.apply(builder);
                }
                return medicamentoRepository.save(normalizarReferencias(builder.build()));
            });
    }

    /** Garante FKs de catálogo (fabricante/categoria) e EAN único por medicamento. */
    private Medicamento normalizarReferencias(Medicamento med) {
        var fabricante = br.com.farmacia.domain.medicamento.entity.Fabricante.builder()
            .id(FABRICANTE_ID).build();
        var categoria = br.com.farmacia.domain.medicamento.entity.Categoria.builder()
            .id(CATEGORIA_ID).build();
        String codigoEan = med.getCodigoEan() != null
            ? gerarEanUnico(med.getNomeComercial()) : null;
        String codigoAnvisa = med.getCodigoAnvisa() != null
            ? gerarCodigoAnvisaUnico(med.getNomeComercial()) : null;

        med.atualizar(
            codigoEan,
            codigoAnvisa,
            med.getNomeComercial(),
            med.getNomeGenerico(),
            med.getTipo(),
            med.getFormaFarmaceutica(),
            med.getConcentracao(),
            med.getApresentacao(),
            med.getClasseTerapeutica(),
            med.getRequerReceita(),
            med.getNivelControle(),
            med.getPrecoMaximoConsumidor(),
            fabricante,
            categoria,
            null
        );
        return med;
    }

    private static String gerarEanUnico(String nome) {
        long hash = Math.abs((long) nome.hashCode()) % 1_000_000_000_000L;
        return String.format("789%012d", hash).substring(0, 13);
    }

    private static String gerarCodigoAnvisaUnico(String nome) {
        return String.format("%015d", Math.abs(nome.hashCode()) % 1_000_000_000_000_000L);
    }

    @Transactional
    public ItemEstoque garantirItemEstoque(UUID medicamentoId, int quantidadeAtual, int quantidadeMinima) {
        Medicamento medicamento = Medicamento.builder().id(medicamentoId).build();
        return estoqueRepository.findByMedicamentoId(medicamentoId)
            .map(item -> estoqueRepository.salvar(ItemEstoque.builder()
                .id(item.getId())
                .medicamento(medicamento)
                .quantidadeAtual(quantidadeAtual)
                .quantidadeMinima(quantidadeMinima)
                .quantidadeMaxima(item.getQuantidadeMaxima())
                .ultimaMovimentacao(item.getUltimaMovimentacao())
                .build()))
            .orElseGet(() -> estoqueRepository.salvar(ItemEstoque.builder()
                .medicamento(medicamento)
                .quantidadeAtual(quantidadeAtual)
                .quantidadeMinima(quantidadeMinima)
                .quantidadeMaxima(500)
                .build()));
    }

    @Transactional
    public Lote garantirLote(String numeroLote, Medicamento medicamento,
                               LocalDate validade, int quantidade, br.com.farmacia.domain.estoque.enums.StatusLote status) {
        return loteRepository.findByNumeroLote(numeroLote)
            .map(lote -> loteRepository.save(Lote.builder()
                .id(lote.getId())
                .notaFiscalId(lote.getNotaFiscalId())
                .medicamento(medicamento)
                .numeroLote(numeroLote)
                .dataFabricacao(lote.getDataFabricacao())
                .dataValidade(validade)
                .quantidadeRecebida(lote.getQuantidadeRecebida())
                .quantidadeAtual(quantidade)
                .precoCusto(lote.getPrecoCusto())
                .status(status)
                .build()))
            .orElseGet(() -> loteRepository.save(
                br.com.farmacia.qa.builder.FarmaciaTestBuilders.LoteBuilder.umLote()
                    .comNumeroLote(numeroLote)
                    .comMedicamento(medicamento)
                    .comDataValidade(validade)
                    .comQuantidadeAtual(quantidade)
                    .comStatus(status)
                    .build()));
    }

    /** Garante medicamento C1 com registro em {@code medicamentos_controlados}. */
    @Transactional
    public Medicamento garantirMedicamentoControlado(String nomeComercial, int quantidadeMaximaReceita) {
        Medicamento med = garantirMedicamento(nomeComercial, b ->
            br.com.farmacia.qa.builder.FarmaciaTestBuilders.MedicamentoBuilder
                .umMedicamentoControlado()
                .comNomeComercial(nomeComercial));

        MedicamentoControlado ctrl = MedicamentoControlado.builder()
            .medicamento(med)
            .portaria("Portaria 344/98")
            .lista("C1")
            .quantidadeMaximaReceita(quantidadeMaximaReceita)
            .validadeReceitaDias(30)
            .psicootropico(true)
            .entorpecente(false)
            .build();
        med.associarMedicamentoControlado(ctrl);
        return medicamentoRepository.save(med);
    }

    @Transactional
    public Prescritor garantirPrescritor(String nome, String crmCompleto) {
        String[] partes = crmCompleto.split("/");
        String crm = partes[0].trim();
        String uf = partes.length > 1 ? partes[1].trim() : "SP";

        return prescritorJpaRepository.findByCrmAndUfCrm(crm, uf)
            .map(PrescritorPersistenceMapper::toDomain)
            .orElseGet(() -> {
                Prescritor prescritor = Prescritor.builder()
                    .id(UUID.randomUUID())
                    .nome(nome)
                    .crm(crm)
                    .ufCrm(uf)
                    .ativo(true)
                    .build();
                return PrescritorPersistenceMapper.toDomain(
                    prescritorJpaRepository.save(PrescritorPersistenceMapper.toJpa(prescritor)));
            });
    }

    @Transactional(readOnly = true)
    public UUID obterFarmaceuticoIdPorCrf(String crf) {
        return farmaceuticoRepository.findByFuncionarioId(
                funcionarioRepository.findByEmail(FARMACEUTICO_EMAIL).orElseThrow().getId())
            .orElseThrow(() -> new IllegalStateException("Farmacêutico seed não encontrado"))
            .getId();
    }

    @Transactional(readOnly = true)
    public UUID obterPdvIdPorNumero(String numero) {
        return pdvRepository.findByNumero(numero)
            .orElseThrow(() -> new IllegalStateException("PDV não encontrado: " + numero))
            .getId();
    }

    /** Zera saldo de lotes anteriores para isolar cenários BDD (sem apagar FKs). */
    @Transactional
    public void zerarLotesDoMedicamento(UUID medicamentoId) {
        jdbcTemplate.update(
            "UPDATE lotes SET quantidade_atual = 0, status = 'ESGOTADO' WHERE medicamento_id = ?",
            medicamentoId);
    }

    @Transactional
    public void ajustarQuantidadeMaximaReceita(UUID medicamentoId, int maximo) {
        jdbcTemplate.update(
            "UPDATE medicamentos_controlados SET quantidade_maxima_receita = ? WHERE medicamento_id = ?",
            maximo, medicamentoId);
    }

    @Transactional(readOnly = true)
    public UUID obterBalconistaId() {
        return funcionarioRepository.findByEmail(BALCONISTA_EMAIL)
            .orElseThrow(() -> new IllegalStateException("Balconista seed não encontrado"))
            .getId();
    }

    private void semearFuncionario(String email, String senha, RoleSistema role,
                                   String nome, String cpf) {
        if (funcionarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        UUID cargoId = cargoJpaRepository.findByRoleSistema(role)
            .orElseThrow(() -> new IllegalStateException(
                "Cargo não semeado para role " + role + " (verifique a migration V3)"))
            .getId();

        funcionarioRepository.save(Funcionario.builder()
            .nome(nome)
            .cpf(cpf)
            .email(email)
            .senhaHash(passwordEncoder.encode(senha))
            .cargo(Cargo.builder().id(cargoId).roleSistema(role).build())
            .dataAdmissao(LocalDate.now())
            .ativo(true)
            .build());
    }

    private void semearFarmaceutico(String crf) {
        var funcionario = funcionarioRepository.findByEmail(FARMACEUTICO_EMAIL).orElseThrow();
        if (farmaceuticoRepository.findByFuncionarioId(funcionario.getId()).isPresent()) {
            return;
        }
        farmaceuticoRepository.save(Farmaceutico.builder()
            .funcionario(funcionario)
            .crf(crf)
            .ufCrf("SP")
            .responsavelTecnico(true)
            .ativo(true)
            .build());
    }
}
