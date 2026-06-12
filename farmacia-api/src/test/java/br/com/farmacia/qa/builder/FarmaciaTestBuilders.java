package br.com.farmacia.qa.builder;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.medicamento.entity.*;
import br.com.farmacia.domain.medicamento.enums.*;
import br.com.farmacia.domain.receituario.entity.*;
import br.com.farmacia.domain.receituario.enums.*;
import br.com.farmacia.domain.venda.entity.*;
import br.com.farmacia.domain.venda.enums.*;
import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.financeiro.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Test Data Builders centralizados.
 *
 * <p><b>Heurística Júlio de Lima (Mentor Master)</b>:</p>
 * <ul>
 *   <li><b>Builder Pattern nos testes</b>: evita fixtures frágeis e comentários
 *       tipo "esse campo não importa". Cada builder explicita o que importa
 *       para aquele cenário.</li>
 *   <li><b>Defaults realistas</b>: valores padrão válidos para o domínio.
 *       O teste só sobrescreve o que é relevante para o cenário em questão.</li>
 *   <li><b>Nomenclatura semântica</b>: {@code umMedicamentoControlado()},
 *       {@code umLoteVencido()}, {@code umaReceitaAprovada()} — lê como
 *       especificação de comportamento.</li>
 *   <li><b>Heurística SFDIPOT aplicada</b>: os builders cobrem variações de
 *       Structure (entidades aninhadas), Function (regras de negócio),
 *       Data (valores extremos) e Time (validades/datas).</li>
 * </ul>
 */
public final class FarmaciaTestBuilders {

    private FarmaciaTestBuilders() {}

    // ═══════════════════════════════════════════════════════════════════════
    // MEDICAMENTO
    // ═══════════════════════════════════════════════════════════════════════

    public static class MedicamentoBuilder {

        private UUID id                         = UUID.randomUUID();
        private String codigoEan                = "7891234567890";
        private String codigoAnvisa             = "123456789012345";
        private String nomeComercial            = "Dipirona Sódica 500mg";
        private String nomeGenerico             = "Dipirona Monoidratada";
        private TipoMedicamento tipo            = TipoMedicamento.GENERICO;
        private FormaFarmaceutica forma         = FormaFarmaceutica.COMPRIMIDO;
        private String concentracao             = "500mg";
        private Boolean requerReceita           = false;
        private NivelControle nivelControle     = NivelControle.LIVRE;
        private BigDecimal pmc                  = new BigDecimal("12.50");
        private Boolean ativo                   = true;
        private Fabricante fabricante           = FabricanteBuilder.umFabricante().build();
        private MedicamentoControlado controlado = null;

        public static MedicamentoBuilder umMedicamento() {
            return new MedicamentoBuilder();
        }

        /** Medicamento de venda livre — sem receita. */
        public static MedicamentoBuilder umMedicamentoLivre() {
            return new MedicamentoBuilder()
                .comNivelControle(NivelControle.LIVRE)
                .comRequerReceita(false);
        }

        /** Medicamento que exige receita simples. */
        public static MedicamentoBuilder umMedicamentoComReceitaSimples() {
            return new MedicamentoBuilder()
                .comNomeComercial("Amoxicilina 500mg")
                .comNivelControle(NivelControle.RECEITA_SIMPLES)
                .comRequerReceita(true);
        }

        /** Medicamento controlado C1 — exige receita Azul. */
        public static MedicamentoBuilder umMedicamentoControlado() {
            return new MedicamentoBuilder()
                .comNomeComercial("Rivotril 2mg")
                .comNomeGenerico("Clonazepam")
                .comNivelControle(NivelControle.CONTROLADO_C1)
                .comRequerReceita(true)
                .comPmc(new BigDecimal("45.90"));
        }

        /** Medicamento entorpecente B1 — exige receita Branca Especial. */
        public static MedicamentoBuilder umMedicamentoEntorpecente() {
            return new MedicamentoBuilder()
                .comNomeComercial("Morfina 10mg")
                .comNomeGenerico("Cloridrato de Morfina")
                .comNivelControle(NivelControle.CONTROLADO_B1)
                .comRequerReceita(true)
                .comPmc(new BigDecimal("120.00"));
        }

        /** Antimicrobiano — exige receita com retenção (RDC 20/2011). */
        public static MedicamentoBuilder umAntimicrobiano() {
            return new MedicamentoBuilder()
                .comNomeComercial("Azitromicina 500mg")
                .comNivelControle(NivelControle.ANTIMICROBIANO)
                .comRequerReceita(true);
        }

        public MedicamentoBuilder comId(UUID id) { this.id = id; return this; }
        public MedicamentoBuilder comNomeComercial(String nome) { this.nomeComercial = nome; return this; }
        public MedicamentoBuilder comNomeGenerico(String nome) { this.nomeGenerico = nome; return this; }
        public MedicamentoBuilder comNivelControle(NivelControle nivel) { this.nivelControle = nivel; return this; }
        public MedicamentoBuilder comRequerReceita(Boolean req) { this.requerReceita = req; return this; }
        public MedicamentoBuilder comPmc(BigDecimal pmc) { this.pmc = pmc; return this; }
        public MedicamentoBuilder inativo() { this.ativo = false; return this; }
        public MedicamentoBuilder comControlado(MedicamentoControlado ctrl) { this.controlado = ctrl; return this; }
        public MedicamentoBuilder comCodigoEan(String ean) { this.codigoEan = ean; return this; }

        public Medicamento build() {
            Medicamento med = Medicamento.builder()
                .id(id)
                .codigoEan(codigoEan)
                .codigoAnvisa(codigoAnvisa)
                .nomeComercial(nomeComercial)
                .nomeGenerico(nomeGenerico)
                .tipo(tipo)
                .formaFarmaceutica(forma)
                .concentracao(concentracao)
                .requerReceita(requerReceita)
                .nivelControle(nivelControle)
                .precoMaximoConsumidor(pmc)
                .ativo(ativo)
                .fabricante(fabricante)
                .principiosAtivos(new ArrayList<>())
                .build();

            if (controlado != null) {
                med.associarMedicamentoControlado(controlado);
            }
            return med;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOTE
    // ═══════════════════════════════════════════════════════════════════════

    public static class LoteBuilder {

        private UUID id                  = UUID.randomUUID();
        private String numeroLote        = "LOT-2024-001";
        private LocalDate dataFabricacao = LocalDate.now().minusMonths(6);
        private LocalDate dataValidade   = LocalDate.now().plusMonths(18);
        private Integer qtdRecebida      = 100;
        private Integer qtdAtual         = 100;
        private BigDecimal precoCusto    = new BigDecimal("8.00");
        private StatusLote status        = StatusLote.ATIVO;
        private Medicamento medicamento  = MedicamentoBuilder.umMedicamento().build();

        public static LoteBuilder umLote() { return new LoteBuilder(); }

        /** Lote com estoque farto e validade longa — cenário padrão feliz. */
        public static LoteBuilder umLoteDisponivel() {
            return new LoteBuilder()
                .comQuantidadeAtual(100)
                .comDataValidade(LocalDate.now().plusMonths(18));
        }

        /** Lote vencido — não deve ser dispensado. */
        public static LoteBuilder umLoteVencido() {
            return new LoteBuilder()
                .comDataValidade(LocalDate.now().minusDays(1))
                // C-08: status ATIVO simula lote expirado ainda não processado pelo scheduler;
                // findByStatusAndDataValidadeBefore(ATIVO, ...) só encontra lotes ATIVOS
                .comStatus(StatusLote.ATIVO);
        }

        /** Lote prestes a vencer (dentro de 30 dias) — deve gerar alerta. */
        public static LoteBuilder umLoteProximoDoVencimento() {
            return new LoteBuilder()
                .comDataValidade(LocalDate.now().plusDays(15));
        }

        /** Lote com estoque zerado. */
        public static LoteBuilder umLoteEsgotado() {
            return new LoteBuilder()
                .comQuantidadeAtual(0)
                .comStatus(StatusLote.ESGOTADO);
        }

        /** Lote com apenas 1 unidade — cenário de borda de estoque. */
        public static LoteBuilder umLoteComUmaUnidade() {
            return new LoteBuilder().comQuantidadeAtual(1);
        }

        public LoteBuilder comId(UUID id) { this.id = id; return this; }
        public LoteBuilder comNumeroLote(String num) { this.numeroLote = num; return this; }
        public LoteBuilder comDataValidade(LocalDate data) { this.dataValidade = data; return this; }
        public LoteBuilder comDataFabricacao(LocalDate data) { this.dataFabricacao = data; return this; }
        public LoteBuilder comQuantidadeAtual(int qtd) { this.qtdAtual = qtd; return this; }
        public LoteBuilder comStatus(StatusLote status) { this.status = status; return this; }
        public LoteBuilder comMedicamento(Medicamento med) { this.medicamento = med; return this; }
        public LoteBuilder comPrecoCusto(BigDecimal preco) { this.precoCusto = preco; return this; }

        public Lote build() {
            return Lote.builder()
                .id(id)
                .numeroLote(numeroLote)
                .dataFabricacao(dataFabricacao)
                .dataValidade(dataValidade)
                .quantidadeRecebida(qtdRecebida)
                .quantidadeAtual(qtdAtual)
                .precoCusto(precoCusto)
                .status(status)
                .medicamento(medicamento)
                .build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RECEITA
    // ═══════════════════════════════════════════════════════════════════════

    public static class ReceitaBuilder {

        private UUID id               = UUID.randomUUID();
        private String numero         = "REC-2024-001";
        private LocalDate emissao     = LocalDate.now().minusDays(5);
        private LocalDate validade    = LocalDate.now().plusDays(25);
        private TipoReceita tipo      = TipoReceita.SIMPLES;
        private StatusReceita status  = StatusReceita.PENDENTE;
        private Boolean retida        = false;
        private Prescritor prescritor = PrescritorBuilder.umPrescritor().build();

        public static ReceitaBuilder umaReceita() { return new ReceitaBuilder(); }

        /** Receita simples aprovada e dentro da validade. */
        public static ReceitaBuilder umaReceitaAprovada() {
            return new ReceitaBuilder()
                .comStatus(StatusReceita.APROVADA)
                .comValidade(LocalDate.now().plusDays(25));
        }

        /** Receita Azul (C1 - psicotrópico) aprovada. */
        public static ReceitaBuilder umaReceitaAzulAprovada() {
            return new ReceitaBuilder()
                .comTipo(TipoReceita.AZUL)
                .comStatus(StatusReceita.APROVADA)
                .comValidade(LocalDate.now().plusDays(25));
        }

        /** Receita Branca Especial (B1 - entorpecente) aprovada. */
        public static ReceitaBuilder umaReceitaBrancaEspecialAprovada() {
            return new ReceitaBuilder()
                .comTipo(TipoReceita.BRANCA_ESPECIAL)
                .comStatus(StatusReceita.APROVADA)
                .comValidade(LocalDate.now().plusDays(25));
        }

        /** Receita vencida — não deve ser aceita. */
        public static ReceitaBuilder umaReceitaVencida() {
            return new ReceitaBuilder()
                .comValidade(LocalDate.now().minusDays(1))
                .comStatus(StatusReceita.PENDENTE);
        }

        /** Receita rejeitada pelo farmacêutico. */
        public static ReceitaBuilder umaReceitaRejeitada() {
            return new ReceitaBuilder()
                .comStatus(StatusReceita.REJEITADA);
        }

        public ReceitaBuilder comId(UUID id) { this.id = id; return this; }
        public ReceitaBuilder comNumero(String num) { this.numero = num; return this; }
        public ReceitaBuilder comEmissao(LocalDate data) { this.emissao = data; return this; }
        public ReceitaBuilder comValidade(LocalDate data) { this.validade = data; return this; }
        public ReceitaBuilder comTipo(TipoReceita tipo) { this.tipo = tipo; return this; }
        public ReceitaBuilder comStatus(StatusReceita status) { this.status = status; return this; }
        public ReceitaBuilder retida() { this.retida = true; return this; }
        public ReceitaBuilder comPrescritor(Prescritor p) { this.prescritor = p; return this; }

        public Receita build() {
            return Receita.builder()
                .id(id)
                .numeroReceita(numero)
                .dataEmissao(emissao)
                .dataValidade(validade)
                .tipo(tipo)
                .status(status)
                .retida(retida)
                .prescritor(prescritor)
                .build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRESCRITOR
    // ═══════════════════════════════════════════════════════════════════════

    public static class PrescritorBuilder {

        private UUID id             = UUID.randomUUID();
        private String nome         = "Dr. Ricardo Santos";
        private String crm          = "123456";
        private String uf           = "SP";
        private String especialidade = "Clínica Geral";

        public static PrescritorBuilder umPrescritor() { return new PrescritorBuilder(); }

        /** Prescritor com CRM inválido (sem número). */
        public static PrescritorBuilder umPrescritorSemCrm() {
            PrescritorBuilder b = new PrescritorBuilder();
            b.crm = null;
            return b;
        }

        public PrescritorBuilder comCrm(String crm) { this.crm = crm; return this; }
        public PrescritorBuilder comUf(String uf) { this.uf = uf; return this; }

        public Prescritor build() {
            return Prescritor.builder()
                .id(id)
                .nome(nome)
                .crm(crm)
                .ufCrm(uf)
                .especialidade(especialidade)
                .build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FABRICANTE
    // ═══════════════════════════════════════════════════════════════════════

    public static class FabricanteBuilder {

        private UUID id             = UUID.randomUUID();
        private String razaoSocial  = "Laboratório Teste Ltda";
        private String cnpj         = "12345678000195";
        private Boolean ativo       = true;

        public static FabricanteBuilder umFabricante() { return new FabricanteBuilder(); }

        public Fabricante build() {
            return Fabricante.builder()
                .id(id)
                .razaoSocial(razaoSocial)
                .cnpj(cnpj)
                .ativo(ativo)
                .build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLIENTE
    // ═══════════════════════════════════════════════════════════════════════

    public static class ClienteBuilder {

        private UUID id         = UUID.randomUUID();
        private String nome     = "João da Silva";
        private String cpf      = "12345678901";
        private String telefone = "11999999999";
        private Boolean ativo   = true;

        public static ClienteBuilder umCliente() { return new ClienteBuilder(); }

        public ClienteBuilder comCpf(String cpf) { this.cpf = cpf; return this; }
        public ClienteBuilder comNome(String nome) { this.nome = nome; return this; }

        public Cliente build() {
            return Cliente.builder()
                .id(id)
                .nome(nome)
                .cpf(cpf)
                .telefone(telefone)
                .ativo(ativo)
                .build();
        }
    }
}
