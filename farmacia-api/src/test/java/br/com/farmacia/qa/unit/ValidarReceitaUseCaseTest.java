package br.com.farmacia.application.receituario.usecase;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.entity.MedicamentoControlado;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.enums.TipoReceita;
import br.com.farmacia.domain.receituario.exception.FarmaceuticoNaoEncontradoException;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.funcionario.repository.FarmaceuticoRepository;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link ValidarReceitaUseCase}.
 *
 * <p><b>Heurística Júlio de Lima (Mentor Master)</b>:</p>
 * <ul>
 *   <li><b>@Nested + @DisplayName</b>: agrupa cenários por comportamento esperado,
 *       criando uma documentação viva do use case.</li>
 *   <li><b>Tripla A (AAA)</b>: Arrange / Act / Assert explícito com comentários.</li>
 *   <li><b>Nomenclatura</b>: {@code deve_[resultado]_quando_[condição]} em português
 *       — fácil leitura nos reports do JUnit.</li>
 *   <li><b>Heurística RCRCRC</b>: Real data, Correct, Range, Collection,
 *       Reference, Calculation — cada bloco @Nested cobre um aspecto.</li>
 *   <li><b>Mocks cirúrgicos</b>: apenas dependências externas são mockadas.
 *       O comportamento do domínio é testado real.</li>
 *   <li><b>AssertJ</b>: assertions fluentes e mensagens de erro descritivas.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidarReceitaUseCase — Validação de Receitas Médicas")
class ValidarReceitaUseCaseTest {

    @Mock
    private ReceitaRepository        receitaRepository;
    @Mock
    private MedicamentoRepository    medicamentoRepository;
    @Mock
    private FarmaceuticoRepository   farmaceuticoRepository;

    @InjectMocks
    private ValidarReceitaUseCase sut; // System Under Test

    private UUID receitaId;
    private UUID farmaceuticoId;
    private Farmaceutico farmaceuticoAtivo;

    @BeforeEach
    void setUp() {
        receitaId       = UUID.randomUUID();
        farmaceuticoId  = UUID.randomUUID();
        farmaceuticoAtivo = criarFarmaceuticoAtivo();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CENÁRIOS DE APROVAÇÃO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que a receita é válida")
    class DadoQueAReceitaEValida {

        @Test
        @DisplayName("deve aprovar receita simples com medicamento sem controle")
        void deve_aprovar_receita_simples_com_medicamento_livre() {
            // ARRANGE
            Receita receita      = ReceitaBuilder.umaReceitaAprovada()
                                    .comId(receitaId)
                                    .comStatus(StatusReceita.PENDENTE)
                                    .build();

            Medicamento medicamento = MedicamentoBuilder.umMedicamentoComReceitaSimples().build();

            configurarMocksBasicos(receita, medicamento);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(
                    medicamento.getId(), 1))
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada())
                .as("Receita válida com medicamento compatível deve ser aprovada")
                .isTrue();
            assertThat(output.status()).isEqualTo("APROVADA");
            assertThat(output.violacoes()).isEmpty();

            verify(receitaRepository).save(any(Receita.class));
        }

        @Test
        @DisplayName("deve aprovar receita Azul para medicamento controlado C1")
        void deve_aprovar_receita_azul_para_medicamento_C1() {
            // ARRANGE
            Receita receita = ReceitaBuilder.umaReceitaAzulAprovada()
                .comId(receitaId)
                .comStatus(StatusReceita.PENDENTE)
                .build();

            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            MedicamentoControlado ctrl = criarControladoC1(med, 2, 30);
            med.associarMedicamentoControlado(ctrl);

            configurarMocksBasicos(receita, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada()).isTrue();
            assertThat(output.violacoes()).isEmpty();
        }

        @Test
        @DisplayName("deve marcar receita Azul como retida após aprovação")
        void deve_marcar_receita_azul_como_retida_apos_aprovacao() {
            // ARRANGE
            Receita receita = ReceitaBuilder.umaReceitaAzulAprovada()
                .comId(receitaId)
                .comStatus(StatusReceita.PENDENTE)
                .build();

            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            configurarMocksBasicos(receita, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
            );

            // ACT
            sut.executar(input);

            // ASSERT — verifica que o save foi chamado com receita retida
            verify(receitaRepository).save(argThat(r ->
                r.getRetida().equals(Boolean.TRUE)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CENÁRIOS DE REJEIÇÃO — DADOS DA RECEITA
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que a receita possui problemas nos dados")
    class DadoQueAReceitaPossuiProblemasNosDados {

        @Test
        @DisplayName("deve rejeitar quando receita está vencida")
        void deve_rejeitar_quando_receita_esta_vencida() {
            // ARRANGE
            Receita receitaVencida = ReceitaBuilder.umaReceitaVencida()
                .comId(receitaId).build();

            Medicamento med = MedicamentoBuilder.umMedicamento().build();
            configurarMocksBasicos(receitaVencida, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada()).isFalse();
            assertThat(output.violacoes())
                .as("Deve indicar que a receita está vencida")
                .anyMatch(v -> v.toLowerCase().contains("venc"));
        }

        @Test
        @DisplayName("deve rejeitar quando prescritor não possui CRM")
        void deve_rejeitar_quando_prescritor_nao_possui_crm() {
            // ARRANGE
            Receita receita = ReceitaBuilder.umaReceita()
                .comId(receitaId)
                .comPrescritor(PrescritorBuilder.umPrescritorSemCrm().build())
                .build();

            Medicamento med = MedicamentoBuilder.umMedicamento().build();
            configurarMocksBasicos(receita, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada()).isFalse();
            assertThat(output.violacoes())
                .anyMatch(v -> v.toLowerCase().contains("crm"));
        }

        @Test
        @DisplayName("deve rejeitar quando receita não está com status PENDENTE")
        void deve_rejeitar_quando_receita_nao_esta_pendente() {
            // ARRANGE
            Receita receitaJaProcessada = ReceitaBuilder.umaReceitaRejeitada()
                .comId(receitaId).build();

            Medicamento med = MedicamentoBuilder.umMedicamento().build();
            configurarMocksBasicos(receitaJaProcessada, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada()).isFalse();
            assertThat(output.violacoes())
                .anyMatch(v -> v.toLowerCase().contains("status") || v.toLowerCase().contains("processada"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CENÁRIOS DE REJEIÇÃO — INCOMPATIBILIDADE DE TIPO DE RECEITA
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que o tipo de receita é incompatível com o medicamento")
    class DadoQueOTipoDeReceitaEIncompativel {

        @Test
        @DisplayName("deve rejeitar receita simples para medicamento controlado C1")
        void deve_rejeitar_receita_simples_para_medicamento_C1() {
            // ARRANGE — receita simples, medicamento exige Azul
            Receita receitaSimples = ReceitaBuilder.umaReceitaAprovada()
                .comId(receitaId)
                .comTipo(TipoReceita.SIMPLES)
                .comStatus(StatusReceita.PENDENTE)
                .build();

            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            configurarMocksBasicos(receitaSimples, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada()).isFalse();
            assertThat(output.violacoes())
                .as("Deve informar o tipo de receita requerido")
                .anyMatch(v -> v.contains("AZUL") || v.contains("azul") || v.contains("receita"));
        }

        @Test
        @DisplayName("deve rejeitar receita Azul para medicamento entorpecente B1")
        void deve_rejeitar_receita_azul_para_medicamento_B1() {
            // ARRANGE — medicamento B1 exige Branca Especial, não Azul
            Receita receitaAzul = ReceitaBuilder.umaReceitaAzulAprovada()
                .comId(receitaId)
                .comStatus(StatusReceita.PENDENTE)
                .build();

            Medicamento med = MedicamentoBuilder.umMedicamentoEntorpecente().build();
            configurarMocksBasicos(receitaAzul, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada()).isFalse();
            assertThat(output.violacoes()).isNotEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CENÁRIOS — QUANTIDADE MÁXIMA (PORTARIA 344)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que a quantidade excede o permitido pela Portaria 344")
    class DadoQueAQuantidadeExcedeOPermitido {

        @Test
        @DisplayName("deve rejeitar quando quantidade excede o máximo por receita")
        void deve_rejeitar_quando_quantidade_excede_maximo_por_receita() {
            // ARRANGE — máximo é 2 embalagens, solicitando 5
            Receita receita = ReceitaBuilder.umaReceitaAzulAprovada()
                .comId(receitaId)
                .comStatus(StatusReceita.PENDENTE)
                .build();

            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            MedicamentoControlado ctrl = criarControladoC1(med, 2, 30); // max 2
            med.associarMedicamentoControlado(ctrl);

            configurarMocksBasicos(receita, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 5)) // solicitando 5
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada()).isFalse();
            assertThat(output.violacoes())
                .anyMatch(v -> v.contains("excede") || v.contains("máximo"));
        }

        @Test
        @DisplayName("deve aprovar quando quantidade está exatamente no limite máximo")
        void deve_aprovar_quando_quantidade_esta_no_limite_maximo() {
            // ARRANGE — máximo é 2, solicitando exatamente 2 (valor de borda)
            Receita receita = ReceitaBuilder.umaReceitaAzulAprovada()
                .comId(receitaId)
                .comStatus(StatusReceita.PENDENTE)
                .build();

            Medicamento med = MedicamentoBuilder.umMedicamentoControlado().build();
            MedicamentoControlado ctrl = criarControladoC1(med, 2, 30);
            med.associarMedicamentoControlado(ctrl);

            configurarMocksBasicos(receita, med);

            var input = new ValidarReceitaUseCase.Input(
                receitaId, farmaceuticoId,
                List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 2)) // limite exato
            );

            // ACT
            var output = sut.executar(input);

            // ASSERT
            assertThat(output.aprovada())
                .as("Quantidade no limite máximo permitido deve ser aprovada")
                .isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CENÁRIOS — EXCEÇÕES DE INFRAESTRUTURA
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dado que recursos não são encontrados")
    class DadoQueRecursosNaoSaoEncontrados {

        @Test
        @DisplayName("deve lançar ReceitaNaoEncontradaException quando receita não existe")
        void deve_lancar_exception_quando_receita_nao_existe() {
            // ARRANGE — a receita é buscada primeiro; basta ela não existir
            when(receitaRepository.findById(receitaId))
                .thenReturn(Optional.empty());

            var input = new ValidarReceitaUseCase.Input(receitaId, farmaceuticoId, List.of());

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(ReceitaNaoEncontradaException.class)
                .hasMessageContaining(receitaId.toString());
        }

        @Test
        @DisplayName("deve lançar FarmaceuticoNaoEncontradoException quando farmacêutico não existe")
        void deve_lancar_exception_quando_farmaceutico_nao_existe() {
            // ARRANGE — receita existe, mas o farmacêutico não
            when(receitaRepository.findById(receitaId))
                .thenReturn(Optional.of(ReceitaBuilder.umaReceita().comId(receitaId).build()));
            when(farmaceuticoRepository.findById(farmaceuticoId))
                .thenReturn(Optional.empty());

            var input = new ValidarReceitaUseCase.Input(receitaId, farmaceuticoId, List.of());

            // ACT + ASSERT
            assertThatThrownBy(() -> sut.executar(input))
                .isInstanceOf(FarmaceuticoNaoEncontradoException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TESTE PARAMETRIZADO — TODOS OS TIPOS DE RECEITA INCOMPATÍVEL COM B1
    // ═══════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "Tipo {0} não deve ser aceito para medicamento B1")
    @EnumSource(value = TipoReceita.class,
        names = {"SIMPLES", "AZUL", "AMARELA", "ANTIMICROBIANO"}) // apenas BRANCA_ESPECIAL é aceita
    @DisplayName("deve rejeitar tipos de receita incompatíveis com B1")
    void deve_rejeitar_todos_os_tipos_incompativeis_com_B1(TipoReceita tipo) {
        // ARRANGE
        Receita receita = ReceitaBuilder.umaReceita()
            .comId(receitaId)
            .comTipo(tipo)
            .comValidade(LocalDate.now().plusDays(20))
            .build();

        Medicamento med = MedicamentoBuilder.umMedicamentoEntorpecente().build();
        configurarMocksBasicos(receita, med);

        var input = new ValidarReceitaUseCase.Input(
            receitaId, farmaceuticoId,
            List.of(new ValidarReceitaUseCase.Input.ItemValidacao(med.getId(), 1))
        );

        // ACT
        var output = sut.executar(input);

        // ASSERT
        assertThat(output.aprovada())
            .as("Tipo de receita %s não deve ser aceito para B1", tipo)
            .isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarMocksBasicos(Receita receita, Medicamento medicamento) {
        when(farmaceuticoRepository.findById(farmaceuticoId))
            .thenReturn(Optional.of(farmaceuticoAtivo));
        when(receitaRepository.findById(receitaId))
            .thenReturn(Optional.of(receita));
        when(medicamentoRepository.findById(medicamento.getId()))
            .thenReturn(Optional.of(medicamento));
        when(receitaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Farmaceutico criarFarmaceuticoAtivo() {
        return Farmaceutico.builder()
            .id(farmaceuticoId)
            .crf("12345")
            .ufCrf("SP")
            .ativo(true)
            .build();
    }

    private MedicamentoControlado criarControladoC1(Medicamento med, int qtdMax, int validadeDias) {
        return MedicamentoControlado.builder()
            .id(UUID.randomUUID())
            .medicamento(med)
            .portaria("Portaria 344/98")
            .lista("C1")
            .quantidadeMaximaReceita(qtdMax)
            .validadeReceitaDias(validadeDias)
            .psicootropico(true)
            .entorpecente(false)
            .build();
    }
}
