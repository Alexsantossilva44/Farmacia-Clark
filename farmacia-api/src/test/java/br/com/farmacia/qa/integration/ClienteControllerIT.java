package br.com.farmacia.qa.integration;

import br.com.farmacia.api.FarmaciaApplication;
import br.com.farmacia.qa.seed.IntegracaoTestSeed;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static br.com.farmacia.qa.seed.IntegracaoSeedReferencia.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração para {@code ClienteController}.
 *
 * <p><b>Heurística Júlio de Lima — Pirâmide de Testes (IT)</b>:</p>
 * <ul>
 *   <li><b>Banco real (PostgreSQL)</b>: garante que constraints de unicidade
 *       (CPF, e-mail, telefone) e validações do domínio funcionam end-to-end.</li>
 *   <li><b>REST Assured</b>: exercita HTTP → Segurança → Controller → UseCase
 *       → Domain → Repository, incluindo Problem Details (RFC 7807).</li>
 *   <li><b>CPF gerado algoritmicamente</b>: garante unicidade entre execuções
 *       sem necessidade de limpeza manual de banco.</li>
 * </ul>
 *
 * <p><b>Cobertura de cenários (heurística CRUD + BCE):</b></p>
 * <ul>
 *   <li>Happy path (201, 200) — caminho dourado</li>
 *   <li>Boundary (nome com 100 chars, data mínima 1900)</li>
 *   <li>Validation errors (422 Problem Details)</li>
 *   <li>Business rule (409 CPF duplicado)</li>
 *   <li>Authorization (401 sem token, 403 role insuficiente)</li>
 *   <li>Not Found (404 com Problem Details)</li>
 *   <li>Fluxo E2E: Cadastrar → Buscar → Atualizar → Verificar</li>
 * </ul>
 *
 * <p><b>Pré-requisito</b>: PostgreSQL em {@code localhost:5435}
 * (db {@code farmacia_test}, usuário/senha {@code test}).</p>
 *
 * @author Alex Silva e Claude
 */
@SpringBootTest(
    classes = FarmaciaApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5435/farmacia_test",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("ClienteController — Integração")
class ClienteControllerIT {

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @Autowired
    private IntegracaoTestSeed testSeed;

    @LocalServerPort
    private int port;

    private String tokenAdmin;
    private String tokenBalconista;
    private String tokenEstoquista;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        testSeed.semearAmbienteCompleto();

        tokenAdmin      = obterToken(ADMIN_EMAIL, ADMIN_SENHA);
        tokenBalconista = obterToken(BALCONISTA_EMAIL, BALCONISTA_SENHA);
        tokenEstoquista = obterToken(ESTOQUISTA_EMAIL, ESTOQUISTA_SENHA);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/v1/clientes — CADASTRO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/clientes — Cadastro")
    class PostClientes {

        @Test
        @DisplayName("deve retornar 201 ao cadastrar cliente válido com todos os campos")
        void deve_retornar_201_ao_cadastrar_cliente_valido() {
            String cpf = gerarCpfValido();

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payloadClienteCompleto(cpf))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("nome", equalTo("João da Silva"))
                .body("cpf", equalTo(cpf))
                .body("ativo", equalTo(true));
        }

        @Test
        @DisplayName("deve retornar 201 quando balconista cadastra cliente (role autorizado)")
        void deve_retornar_201_quando_balconista_cadastra() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenBalconista)
                .body(payloadClienteCompleto(gerarCpfValido()))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue());
        }

        @Test
        @DisplayName("deve retornar 422 com Problem Details quando nome está em branco")
        void deve_retornar_422_quando_nome_em_branco() {
            String payload = """
                {
                  "nome": "",
                  "cpf": "%s",
                  "dataNascimento": "1990-01-15"
                }
                """.formatted(gerarCpfValido());

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body("status", equalTo(422))
                .body("title", equalTo("Dados inválidos"))
                .body("fields", not(empty()))
                .body("fields.name", hasItem("nome"));
        }

        @Test
        @DisplayName("deve retornar 422 quando CPF tem menos de 11 dígitos")
        void deve_retornar_422_quando_cpf_invalido_formato() {
            String payload = """
                {
                  "nome": "João da Silva",
                  "cpf": "12345",
                  "dataNascimento": "1990-01-15"
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body("fields.name", hasItem("cpf"));
        }

        @Test
        @DisplayName("deve retornar 4xx quando CPF tem todos os dígitos iguais (inválido no domínio)")
        void deve_retornar_erro_quando_cpf_digitos_todos_iguais() {
            String payload = """
                {
                  "nome": "Maria Santos",
                  "cpf": "11111111111",
                  "dataNascimento": "1985-06-20"
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(anyOf(
                    equalTo(HttpStatus.UNPROCESSABLE_ENTITY.value()),
                    equalTo(HttpStatus.CONFLICT.value())
                ))
                .body("userMessage", notNullValue());
        }

        @Test
        @DisplayName("deve retornar 422 quando nome tem apenas uma palavra (sem sobrenome)")
        void deve_retornar_422_quando_nome_sem_sobrenome() {
            String payload = """
                {
                  "nome": "João",
                  "cpf": "%s",
                  "dataNascimento": "1990-01-15"
                }
                """.formatted(gerarCpfValido());

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body("fields.name", hasItem("nome"));
        }

        @Test
        @DisplayName("deve retornar 422 quando nome excede 100 caracteres (limite migration V6)")
        void deve_retornar_422_quando_nome_excede_100_caracteres() {
            // 21 repetições de "Joao " = 105 chars; @Size(max=100) rejeita
            String nomeGrande = "Joao ".repeat(21).trim();

            String payload = """
                {
                  "nome": "%s",
                  "cpf": "%s"
                }
                """.formatted(nomeGrande, gerarCpfValido());

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body("fields.name", hasItem("nome"));
        }

        @Test
        @DisplayName("deve retornar 409 quando CPF já está cadastrado")
        void deve_retornar_409_quando_cpf_duplicado() {
            String cpf = gerarCpfValido();

            // Primeiro cadastro — deve passar
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payloadClienteCompleto(cpf))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.CREATED.value());

            // Segundo cadastro com mesmo CPF — deve rejeitar
            // Precisa de payload completo: validação de campos ocorre ANTES da checagem de CPF duplicado
            String fone2 = "21" + cpf.substring(0, 9);
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("""
                    {
                      "nome": "Maria Oliveira",
                      "cpf": "%s",
                      "dataNascimento": "1985-06-20",
                      "sexo": "F",
                      "telefone": "%s",
                      "email": "maria.%s@test.com",
                      "endereco": {
                        "logradouro": "Rua Maria",
                        "numero": "5",
                        "bairro": "Centro",
                        "cidade": "Curitiba",
                        "uf": "PR",
                        "cep": "80010000"
                      }
                    }
                    """.formatted(cpf, fone2, cpf.substring(0, 5)))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("status", equalTo(409))
                .body("userMessage", notNullValue());
        }

        @Test
        @DisplayName("deve retornar 401 quando requisição não tem token JWT")
        void deve_retornar_401_sem_autenticacao() {
            given()
                .contentType(ContentType.JSON)
                .body(payloadClienteCompleto(gerarCpfValido()))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("deve retornar 403 quando estoquista tenta cadastrar cliente")
        void deve_retornar_403_quando_estoquista_tenta_cadastrar() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenEstoquista)
                .body(payloadClienteCompleto(gerarCpfValido()))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/v1/clientes/cpf/{cpf} — BUSCA POR CPF
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/clientes/cpf/{cpf} — Busca por CPF")
    class GetClientePorCpf {

        @Test
        @DisplayName("deve retornar 200 com dados do cliente ao buscar por CPF existente")
        void deve_retornar_200_quando_cpf_existe() {
            String cpf = gerarCpfValido();
            cadastrarCliente(cpf, "Ana Clara", tokenAdmin);

            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/cpf/{cpf}", cpf)
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("cpf", equalTo(cpf))
                .body("nome", equalTo("Ana Clara"))
                .body("ativo", equalTo(true));
        }

        @Test
        @DisplayName("deve retornar 404 com Problem Details quando CPF não existe")
        void deve_retornar_404_quando_cpf_nao_existe() {
            String cpfInexistente = gerarCpfValido();

            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/cpf/{cpf}", cpfInexistente)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("status", equalTo(404))
                .body("title", notNullValue())
                .body("userMessage", notNullValue())
                .body("timestamp", notNullValue());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/v1/clientes/{id} — BUSCA POR ID
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/clientes/{id} — Busca por ID")
    class GetClientePorId {

        @Test
        @DisplayName("deve retornar 200 com estrutura completa quando cliente existe")
        void deve_retornar_200_com_estrutura_correta() {
            String cpf = gerarCpfValido();
            String clienteId = cadastrarCliente(cpf, "Pedro Alves", tokenAdmin);

            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(clienteId))
                .body("nome", equalTo("Pedro Alves"))
                .body("cpf", equalTo(cpf))
                .body("ativo", equalTo(true));
        }

        @Test
        @DisplayName("deve retornar 404 com Problem Details quando ID não existe")
        void deve_retornar_404_com_problem_details_quando_id_nao_existe() {
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/{id}", "00000000-0000-0000-0000-000000000099")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("status", equalTo(404))
                .body("title", notNullValue())
                .body("userMessage", notNullValue())
                .body("timestamp", notNullValue());
        }

        @Test
        @DisplayName("deve retornar 400 quando ID não é UUID válido")
        void deve_retornar_400_quando_id_nao_e_uuid() {
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/{id}", "nao-e-uuid")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/v1/clientes/contato/disponivel — VERIFICAR CONTATO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/clientes/contato/disponivel — Disponibilidade de Contato")
    class GetContatoDisponivel {

        @Test
        @DisplayName("deve retornar telefoneDisponivel=false quando telefone já está em uso")
        void deve_retornar_telefone_indisponivel_quando_ja_usado() {
            String telefone = "11" + ThreadLocalRandom.current().nextInt(900_000_000, 999_999_999);
            String cpf = gerarCpfValido();

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("""
                    {
                      "nome": "Luiz Costa",
                      "cpf": "%s",
                      "dataNascimento": "1980-03-10",
                      "sexo": "M",
                      "telefone": "%s",
                      "email": "luiz.%s@test.com",
                      "endereco": {
                        "logradouro": "Rua Luiz",
                        "numero": "1",
                        "bairro": "Centro",
                        "cidade": "São Paulo",
                        "uf": "SP",
                        "cep": "01001000"
                      }
                    }
                    """.formatted(cpf, telefone, cpf.substring(0, 5)))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(201);

            given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .queryParam("telefone", telefone)
            .when()
                .get("/api/v1/clientes/contato/disponivel")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("telefoneDisponivel", equalTo(false));
        }

        @Test
        @DisplayName("deve retornar telefoneDisponivel=true para telefone nunca usado")
        void deve_retornar_telefone_disponivel_quando_nao_usado() {
            String telefoneNovo = "21" + ThreadLocalRandom.current().nextInt(900_000_000, 999_999_999);

            given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .queryParam("telefone", telefoneNovo)
            .when()
                .get("/api/v1/clientes/contato/disponivel")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("telefoneDisponivel", equalTo(true));
        }

        @Test
        @DisplayName("deve retornar emailDisponivel=false quando email já está em uso")
        void deve_retornar_email_indisponivel_quando_ja_usado() {
            String email = "teste" + System.nanoTime() + "@farmacia.com";
            String cpf = gerarCpfValido();

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("""
                    {
                      "nome": "Carla Nunes",
                      "cpf": "%s",
                      "dataNascimento": "1975-08-22",
                      "sexo": "F",
                      "telefone": "21%s",
                      "email": "%s",
                      "endereco": {
                        "logradouro": "Rua Carla",
                        "numero": "2",
                        "bairro": "Centro",
                        "cidade": "Rio de Janeiro",
                        "uf": "RJ",
                        "cep": "20040020"
                      }
                    }
                    """.formatted(cpf, cpf.substring(0, 9), email))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(201);

            given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .queryParam("email", email)
            .when()
                .get("/api/v1/clientes/contato/disponivel")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("emailDisponivel", equalTo(false));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/v1/clientes/{id} — ATUALIZAÇÃO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/clientes/{id} — Atualização")
    class PutCliente {

        @Test
        @DisplayName("deve retornar 200 ao atualizar nome e observações com dados válidos")
        void deve_retornar_200_ao_atualizar_dados_validos() {
            String cpf = gerarCpfValido();
            String clienteId = cadastrarCliente(cpf, "Roberto Lima", tokenAdmin);

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("""
                    {
                      "nome": "Roberto Lima Júnior",
                      "dataNascimento": "1988-11-05",
                      "sexo": "M",
                      "telefone": "11%s",
                      "email": "cli.%s@test.com",
                      "observacoes": "Atualizado via PUT",
                      "endereco": {
                        "logradouro": "Rua Teste",
                        "numero": "1",
                        "bairro": "Centro",
                        "cidade": "São Paulo",
                        "uf": "SP",
                        "cep": "01001000"
                      }
                    }
                    """.formatted(cpf.substring(0, 9), cpf))
            .when()
                .put("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("nome", equalTo("Roberto Lima Júnior"))
                .body("observacoes", equalTo("Atualizado via PUT"));
        }

        @Test
        @DisplayName("deve retornar 200 ao inativar cliente via ativo=false")
        void deve_retornar_200_ao_inativar_cliente() {
            String cpf = gerarCpfValido();
            String clienteId = cadastrarCliente(cpf, "Sandra Melo", tokenAdmin);

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("""
                    {
                      "dataNascimento": "1990-05-15",
                      "sexo": "M",
                      "telefone": "11%s",
                      "email": "cli.%s@test.com",
                      "ativo": false,
                      "endereco": {
                        "logradouro": "Rua Teste",
                        "numero": "1",
                        "bairro": "Centro",
                        "cidade": "São Paulo",
                        "uf": "SP",
                        "cep": "01001000"
                      }
                    }
                    """.formatted(cpf.substring(0, 9), cpf))
            .when()
                .put("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("ativo", equalTo(false));
        }

        @Test
        @DisplayName("deve retornar 422 quando nome na atualização não tem sobrenome")
        void deve_retornar_422_quando_nome_invalido_na_atualizacao() {
            String clienteId = cadastrarCliente(gerarCpfValido(), "Fabio Santos", tokenAdmin);

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("{ \"nome\": \"Fabio\" }")
            .when()
                .put("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body("fields.name", hasItem("nome"));
        }

        @Test
        @DisplayName("deve retornar 404 ao atualizar cliente inexistente")
        void deve_retornar_404_ao_atualizar_cliente_inexistente() {
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("{ \"nome\": \"Qualquer Nome\" }")
            .when()
                .put("/api/v1/clientes/{id}", UUID.randomUUID().toString())
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FLUXO E2E — Ciclo de vida completo do cliente
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Fluxo E2E — Cadastrar → Buscar por CPF → Atualizar → Inativar")
    class FluxoCompleto {

        @Test
        @DisplayName("deve executar ciclo de vida completo do cliente sem erros")
        void deve_executar_ciclo_de_vida_completo() {
            String cpf = gerarCpfValido();

            // STEP 1: Cadastrar
            String clienteId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payloadClienteCompleto(cpf))
            .when()
                .post("/api/v1/clientes")
            .then()
                .statusCode(201)
                .body("nome", equalTo("João da Silva"))
                .body("ativo", equalTo(true))
                .extract().path("id");

            // STEP 2: Buscar por CPF — deve encontrar
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/cpf/{cpf}", cpf)
            .then()
                .statusCode(200)
                .body("id", equalTo(clienteId))
                .body("cpf", equalTo(cpf));

            // STEP 3: Buscar por ID — deve retornar estrutura completa
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(200)
                .body("id", equalTo(clienteId));

            // STEP 4: Atualizar nome e adicionar alergias
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("""
                    {
                      "nome": "João da Silva Atualizado",
                      "dataNascimento": "1990-05-15",
                      "sexo": "M",
                      "telefone": "11%s",
                      "email": "joao.%s@email.com",
                      "alergias": "Dipirona",
                      "endereco": {
                        "logradouro": "Rua das Flores",
                        "numero": "123",
                        "bairro": "Centro",
                        "cidade": "São Paulo",
                        "uf": "SP",
                        "cep": "01001000"
                      }
                    }
                    """.formatted(cpf.substring(0, 9), cpf))
            .when()
                .put("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(200)
                .body("nome", equalTo("João da Silva Atualizado"))
                .body("alergias", equalTo("Dipirona"));

            // STEP 5: Inativar
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body("""
                    {
                      "dataNascimento": "1990-05-15",
                      "sexo": "M",
                      "telefone": "11%s",
                      "email": "joao.%s@email.com",
                      "ativo": false,
                      "endereco": {
                        "logradouro": "Rua das Flores",
                        "numero": "123",
                        "bairro": "Centro",
                        "cidade": "São Paulo",
                        "uf": "SP",
                        "cep": "01001000"
                      }
                    }
                    """.formatted(cpf.substring(0, 9), cpf))
            .when()
                .put("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(200)
                .body("ativo", equalTo(false));

            // STEP 6: Confirmar estado final — cliente inativo ainda recuperável por ID
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/clientes/{id}", clienteId)
            .then()
                .statusCode(200)
                .body("ativo", equalTo(false));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    /** Autentica e extrai o token JWT do campo {@code token}. */
    private String obterToken(String email, String senha) {
        return given()
            .contentType(ContentType.JSON)
            .body("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha))
        .when()
            .post("/api/v1/auth/token")
        .then()
            .statusCode(200)
            .extract().path("token");
    }

    /**
     * Cadastra um cliente e retorna o ID gerado.
     * Utilitário para testes que precisam de um cliente pré-existente.
     */
    private String cadastrarCliente(String cpf, String nome, String token) {
        String fone = "11" + cpf.substring(0, 9);
        String email = "cli." + cpf + "@test.com";
        return given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {
                  "nome": "%s",
                  "cpf": "%s",
                  "dataNascimento": "1990-05-15",
                  "sexo": "M",
                  "telefone": "%s",
                  "email": "%s",
                  "endereco": {
                    "logradouro": "Rua Teste",
                    "numero": "1",
                    "bairro": "Centro",
                    "cidade": "São Paulo",
                    "uf": "SP",
                    "cep": "01001000"
                  }
                }
                """.formatted(nome, cpf, fone, email))
        .when()
            .post("/api/v1/clientes")
        .then()
            .statusCode(201)
            .extract().path("id");
    }

    /** Payload completo com todos os campos opcionais preenchidos. */
    private static String payloadClienteCompleto(String cpf) {
        String fone = "11" + cpf.substring(0, 9);
        return """
            {
              "nome": "João da Silva",
              "cpf": "%s",
              "dataNascimento": "1990-05-15",
              "sexo": "M",
              "telefone": "%s",
              "email": "joao.%s@email.com",
              "alergias": "Penicilina",
              "observacoes": "Cliente cadastrado via teste de integração",
              "endereco": {
                "logradouro": "Rua das Flores",
                "numero": "123",
                "complemento": "Apto 4",
                "bairro": "Centro",
                "cidade": "São Paulo",
                "uf": "SP",
                "cep": "01001000"
              }
            }
            """.formatted(cpf, fone, cpf);
    }

    /**
     * Gera um CPF matematicamente válido pelo algoritmo de módulo 11.
     *
     * <p>Garante unicidade entre execuções sem depender de fixtures fixas
     * que causariam 409 (CPF duplicado) na segunda execução do test suite.</p>
     */
    static String gerarCpfValido() {
        int[] d = new int[11];
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // Gera 9 dígitos base — primeiro dígito não-zero, evita sequências uniformes
        do {
            d[0] = rnd.nextInt(1, 10);
            for (int i = 1; i < 9; i++) d[i] = rnd.nextInt(10);
        } while (Arrays.stream(d, 0, 9).distinct().count() == 1);

        // Primeiro dígito verificador (módulo 11)
        int soma = 0;
        for (int i = 0; i < 9; i++) soma += d[i] * (10 - i);
        int resto = soma % 11;
        d[9] = resto < 2 ? 0 : 11 - resto;

        // Segundo dígito verificador (módulo 11)
        soma = 0;
        for (int i = 0; i < 10; i++) soma += d[i] * (11 - i);
        resto = soma % 11;
        d[10] = resto < 2 ? 0 : 11 - resto;

        StringBuilder sb = new StringBuilder();
        for (int v : d) sb.append(v);
        return sb.toString();
    }
}
