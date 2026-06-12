package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.MedicamentoAssembler;
import br.com.farmacia.api.v1.model.MedicamentoModel;
import br.com.farmacia.api.v1.model.input.MedicamentoInput;
import br.com.farmacia.api.exceptionhandler.ApiExceptionHandler;
import br.com.farmacia.qa.config.WebMvcSecurityTestConfig;
import br.com.farmacia.application.medicamento.usecase.*;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.enums.TipoMedicamento;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de camada Web para {@link MedicamentoController}.
 *
 * <p><b>Heurística Júlio de Lima — @WebMvcTest</b>:</p>
 * <blockquote>
 * "Testes de controller com @WebMvcTest são mais rápidos que testes de
 * integração completos porque sobem apenas a camada web (sem JPA, sem banco).
 * Use-os para testar: serialização/desserialização JSON, validações @Valid,
 * mapeamento de URLs, códigos HTTP corretos e autorização por role."
 * </blockquote>
 *
 * <p>O que NÃO testar aqui (fica nos testes unitários do Use Case):</p>
 * <ul>
 *   <li>Regras de negócio</li>
 *   <li>Queries ao banco</li>
 *   <li>Lógica de domínio</li>
 * </ul>
 */
@WebMvcTest(MedicamentoController.class)
@Import({ApiExceptionHandler.class, WebMvcSecurityTestConfig.class})
@DisplayName("MedicamentoController — Camada Web (@WebMvcTest)")
class MedicamentoControllerWebMvcTest {

    @Autowired private MockMvc     mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CadastrarMedicamentoUseCase cadastrarMedicamentoUseCase;
    @MockBean private AtualizarMedicamentoUseCase atualizarMedicamentoUseCase;
    @MockBean private ConsultarMedicamentoUseCase consultarMedicamentoUseCase;
    @MockBean private ExcluirMedicamentoUseCase   excluirMedicamentoUseCase;
    @MockBean private MedicamentoAssembler        medicamentoAssembler;

    private static final String BASE_URL = "/api/v1/medicamentos";

    // ═══════════════════════════════════════════════════════════════════════
    // POST — Cadastro
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /medicamentos — Serialização e Validação de Entrada")
    class PostMedicamentos {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 201 com corpo correto ao cadastrar medicamento válido")
        void deve_retornar_201_com_corpo_correto() throws Exception {
            // ARRANGE
            var input   = criarInputValido();
            var entity  = MedicamentoBuilder.umMedicamento().build();
            var model   = criarMedicamentoModel(entity.getId());

            when(medicamentoAssembler.toEntity(any())).thenReturn(entity);
            when(cadastrarMedicamentoUseCase.executar(any())).thenReturn(entity);
            when(medicamentoAssembler.toModel(any())).thenReturn(model);

            // ACT + ASSERT
            mockMvc.perform(post(BASE_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(input)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nomeComercial").value("Dipirona Sódica 500mg"))
                .andExpect(jsonPath("$.nivelControle").value("LIVRE"))
                .andExpect(jsonPath("$.ativo").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 422 quando nomeComercial está em branco")
        void deve_retornar_422_quando_nome_comercial_vazio() throws Exception {
            // ARRANGE — input inválido: nome em branco
            var inputInvalido = criarInputValido();
            inputInvalido.setNomeComercial(""); // viola @NotBlank

            // ACT + ASSERT
            mockMvc.perform(post(BASE_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputInvalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.fields[*].name", hasItem("nomeComercial")))
                .andExpect(jsonPath("$.userMessage").isNotEmpty());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 422 quando tipo é nulo")
        void deve_retornar_422_quando_tipo_e_nulo() throws Exception {
            var inputSemTipo = criarInputValido();
            inputSemTipo.setTipo(null); // viola @NotNull

            mockMvc.perform(post(BASE_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputSemTipo)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[*].name", hasItem("tipo")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 422 quando precoMaximoConsumidor é zero")
        void deve_retornar_422_quando_pmc_e_zero() throws Exception {
            var inputPmcZero = criarInputValido();
            inputPmcZero.setPrecoMaximoConsumidor(BigDecimal.ZERO); // viola @DecimalMin("0.01")

            mockMvc.perform(post(BASE_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputPmcZero)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[*].name", hasItem("precoMaximoConsumidor")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 422 quando codigoEan não tem 13 dígitos")
        void deve_retornar_422_quando_codigo_ean_invalido() throws Exception {
            var inputEanCurto = criarInputValido();
            inputEanCurto.setCodigoEan("123"); // deve ter exatamente 13

            mockMvc.perform(post(BASE_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputEanCurto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[*].name", hasItem("codigoEan")));
        }

        @Test
        @WithMockUser(roles = "BALCONISTA") // role insuficiente
        @DisplayName("deve retornar 403 quando role é insuficiente para cadastrar")
        void deve_retornar_403_para_role_insuficiente() throws Exception {
            mockMvc.perform(post(BASE_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(criarInputValido())))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void deve_retornar_401_sem_autenticacao() throws Exception {
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET — Listagem
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /medicamentos — Listagem e Paginação")
    class GetMedicamentos {

        @Test
        @WithMockUser(roles = "BALCONISTA")
        @DisplayName("deve retornar 200 com lista paginada para qualquer role autorizada")
        void deve_retornar_200_com_lista_paginada() throws Exception {
            // ARRANGE
            var entity = MedicamentoBuilder.umMedicamento().build();
            var model  = criarMedicamentoModel(entity.getId());
            var page   = new PageImpl<>(List.of(entity));

            when(consultarMedicamentoUseCase.listarPaginado(any(Pageable.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(page);
            when(medicamentoAssembler.toModel(any())).thenReturn(model);

            // ACT + ASSERT
            mockMvc.perform(get(BASE_URL)
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser(roles = "BALCONISTA")
        @DisplayName("deve repassar filtro busca ao use case")
        void deve_repassar_filtro_busca() throws Exception {
            var entity = MedicamentoBuilder.umMedicamento().build();
            var page = new PageImpl<>(List.of(entity));

            when(consultarMedicamentoUseCase.listarPaginado(
                    any(Pageable.class), org.mockito.ArgumentMatchers.eq("dipirona")))
                .thenReturn(page);

            mockMvc.perform(get(BASE_URL)
                    .param("page", "0")
                    .param("size", "10")
                    .param("busca", "dipirona"))
                .andExpect(status().isOk());

            org.mockito.Mockito.verify(consultarMedicamentoUseCase)
                .listarPaginado(any(Pageable.class), org.mockito.ArgumentMatchers.eq("dipirona"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /{id} — Busca por ID
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /medicamentos/{id} — Problem Details")
    class GetMedicamentoPorId {

        @Test
        @WithMockUser(roles = "FARMACEUTICO")
        @DisplayName("deve retornar 404 com Problem Details quando não encontrado")
        void deve_retornar_404_com_problem_details() throws Exception {
            // ARRANGE — simula exceção do use case
            UUID idInexistente = UUID.randomUUID();
            when(consultarMedicamentoUseCase.buscarOuFalhar(idInexistente))
                .thenThrow(new MedicamentoNaoEncontradoException(idInexistente));

            // ACT + ASSERT
            mockMvc.perform(get(BASE_URL + "/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value(
                    containsString("nao-encontrado")))
                .andExpect(jsonPath("$.userMessage").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @WithMockUser(roles = "ESTOQUISTA")
        @DisplayName("deve retornar 200 quando medicamento existe")
        void deve_retornar_200_quando_medicamento_existe() throws Exception {
            // ARRANGE
            UUID id     = UUID.randomUUID();
            var entity  = MedicamentoBuilder.umMedicamento().comId(id).build();
            var model   = criarMedicamentoModel(id);

            when(consultarMedicamentoUseCase.buscarOuFalhar(id)).thenReturn(entity);
            when(medicamentoAssembler.toModel(entity)).thenReturn(model);

            // ACT + ASSERT
            mockMvc.perform(get(BASE_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nomeComercial").value("Dipirona Sódica 500mg"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE — Exclusão
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /medicamentos/{id} — Autorização")
    class DeleteMedicamento {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 204 quando admin exclui medicamento existente")
        void deve_retornar_204_quando_admin_exclui() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(excluirMedicamentoUseCase).executar(id);

            mockMvc.perform(delete(BASE_URL + "/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

            verify(excluirMedicamentoUseCase, times(1)).executar(id);
        }

        @Test
        @WithMockUser(roles = "GERENTE") // apenas ADMIN pode excluir
        @DisplayName("deve retornar 403 quando gerente tenta excluir")
        void deve_retornar_403_quando_gerente_tenta_excluir() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());

            verifyNoInteractions(excluirMedicamentoUseCase);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private MedicamentoInput criarInputValido() {
        var input = new MedicamentoInput();
        input.setNomeComercial("Dipirona Sódica 500mg");
        input.setNomeGenerico("Dipirona Monoidratada");
        input.setTipo(TipoMedicamento.GENERICO);
        input.setRequerReceita(false);
        input.setNivelControle(NivelControle.LIVRE);
        input.setCodigoEan("7891234567890");
        input.setPrecoMaximoConsumidor(new BigDecimal("12.50"));

        var fabricanteId = new MedicamentoInput.FabricanteIdInput();
        fabricanteId.setId(UUID.randomUUID());
        input.setFabricante(fabricanteId);

        var categoriaId = new MedicamentoInput.CategoriaIdInput();
        categoriaId.setId(UUID.randomUUID());
        input.setCategoria(categoriaId);

        return input;
    }

    private MedicamentoModel criarMedicamentoModel(UUID id) {
        var model = new MedicamentoModel();
        model.setId(id);
        model.setNomeComercial("Dipirona Sódica 500mg");
        model.setNivelControle(NivelControle.LIVRE);
        model.setAtivo(true);

        var fabricante = new MedicamentoModel.FabricanteResumoModel();
        fabricante.setRazaoSocial("Laboratório Teste Ltda");
        model.setFabricante(fabricante);

        return model;
    }
}
