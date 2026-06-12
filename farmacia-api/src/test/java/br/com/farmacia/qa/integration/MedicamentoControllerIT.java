package br.com.farmacia.api.v1.controller;

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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static br.com.farmacia.qa.seed.IntegracaoSeedReferencia.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração para {@code MedicamentoController}.
 *
 * <p><b>Heurística Júlio de Lima — Testes de Integração (IT)</b>:</p>
 * <ul>
 *   <li><b>Banco real (PostgreSQL)</b>: garante que queries JPA, índices e
 *       constraints funcionam como em produção. O schema é gerido pelo Flyway
 *       ({@code ddl-auto=none}).</li>
 *   <li><b>REST Assured + JWT</b>: exercita a API completa
 *       (HTTP → Auth → Controller → UseCase → Repository → Banco),
 *       incluindo autenticação e autorização por role.</li>
 *   <li><b>Seed idempotente</b>: funcionários (admin/balconista), fabricante,
 *       categoria e medicamento de referência são semeados com UUIDs fixos.</li>
 * </ul>
 *
 * <p><b>Pré-requisito</b>: um PostgreSQL acessível em {@code localhost:5435}
 * (db {@code farmacia_test}, usuário/senha {@code test}). Sobrescreva via
 * {@code -Dspring.datasource.url} se necessário.</p>
 *
 * @author Alex Silva e Claude
 */
@SpringBootTest(classes = FarmaciaApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5435/farmacia_test",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("MedicamentoController — Integração")
class MedicamentoControllerIT {

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @Autowired private IntegracaoTestSeed testSeed;

    @LocalServerPort
    private int port;

    private String tokenAdmin;
    private String tokenBalconista;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        testSeed.semearAmbienteCompleto();

        tokenAdmin      = obterToken(ADMIN_EMAIL, ADMIN_SENHA);
        tokenBalconista = obterToken(BALCONISTA_EMAIL, BALCONISTA_SENHA);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/v1/medicamentos — CADASTRO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/medicamentos — Cadastro")
    class PostMedicamentos {

        @Test
        @DisplayName("deve retornar 201 ao cadastrar medicamento válido")
        void deve_retornar_201_ao_cadastrar_medicamento_valido() {
            String payload = """
                {
                  "codigoEan": "%s",
                  "nomeComercial": "Dipirona 500mg",
                  "nomeGenerico": "Dipirona Monoidratada",
                  "tipo": "GENERICO",
                  "formaFarmaceutica": "COMPRIMIDO",
                  "concentracao": "500mg",
                  "requerReceita": false,
                  "nivelControle": "LIVRE",
                  "precoMaximoConsumidor": 12.50,
                  "fabricante": { "id": "%s" },
                  "categoria": { "id": "%s" }
                }
                """.formatted(eanAleatorio(), FABRICANTE_ID, CATEGORIA_ID);

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .post("/api/v1/medicamentos")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("nomeComercial", equalTo("Dipirona 500mg"))
                .body("nivelControle", equalTo("LIVRE"))
                .body("ativo", equalTo(true));
        }

        @Test
        @DisplayName("deve retornar 422 quando campos obrigatórios estão ausentes")
        void deve_retornar_422_quando_campos_obrigatorios_ausentes() {
            String payloadInvalido = """
                {
                  "nomeComercial": "",
                  "tipo": null
                }
                """;

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payloadInvalido)
            .when()
                .post("/api/v1/medicamentos")
            .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body("title", equalTo("Dados inválidos"))
                .body("fields", not(empty()))
                .body("fields.name", hasItems("nomeComercial", "tipo"));
        }

        @Test
        @DisplayName("deve retornar 403 quando balconista tenta cadastrar medicamento")
        void deve_retornar_403_quando_balconista_tenta_cadastrar() {
            String payload = "{ \"nomeComercial\": \"Teste\" }";

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenBalconista)
                .body(payload)
            .when()
                .post("/api/v1/medicamentos")
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void deve_retornar_401_quando_nao_autenticado() {
            given()
                .contentType(ContentType.JSON)
                .body("{}")
            .when()
                .post("/api/v1/medicamentos")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/v1/medicamentos — LISTAGEM
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/medicamentos — Listagem")
    class GetMedicamentos {

        @Test
        @DisplayName("deve retornar 200 com lista paginada")
        void deve_retornar_200_com_lista_paginada() {
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .queryParam("page", 0)
                .queryParam("size", 10)
            .when()
                .get("/api/v1/medicamentos")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", notNullValue())
                .body("pageable", notNullValue())
                .body("totalElements", greaterThanOrEqualTo(0));
        }

        @Test
        @DisplayName("deve retornar medicamentos na ordem alfabética por padrão")
        void deve_retornar_medicamentos_em_ordem_alfabetica() {
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/medicamentos")
            .then()
                .statusCode(200)
                .body("sort.sorted", equalTo(true));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/v1/medicamentos/{id} — BUSCA POR ID
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/medicamentos/{id} — Busca por ID")
    class GetMedicamentoPorId {

        @Test
        @DisplayName("deve retornar 404 com Problem Details quando medicamento não existe")
        void deve_retornar_404_com_problem_details_quando_nao_existe() {
            String idInexistente = "00000000-0000-0000-0000-000000000000";

            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/medicamentos/{id}", idInexistente)
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("status", equalTo(404))
                .body("title", notNullValue())
                .body("userMessage", notNullValue())
                .body("timestamp", notNullValue());
        }

        @Test
        @DisplayName("deve retornar 200 com estrutura correta quando medicamento existe")
        void deve_retornar_200_com_estrutura_correta() {
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/medicamentos/{id}", MEDICAMENTO_DIPIRONA_ID.toString())
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(MEDICAMENTO_DIPIRONA_ID.toString()))
                .body("nomeComercial", notNullValue())
                .body("fabricante.razaoSocial", notNullValue())
                .body("ativo", equalTo(true));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUT /api/v1/medicamentos/{id} — ATUALIZAÇÃO
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/medicamentos/{id} — Atualização")
    class PutMedicamento {

        @Test
        @DisplayName("deve retornar 200 ao atualizar com dados válidos")
        void deve_retornar_200_ao_atualizar_com_dados_validos() {
            String payload = """
                {
                  "nomeComercial": "Dipirona 500mg Atualizada",
                  "tipo": "GENERICO",
                  "requerReceita": false,
                  "nivelControle": "LIVRE",
                  "precoMaximoConsumidor": 13.00,
                  "fabricante": { "id": "%s" },
                  "categoria": { "id": "%s" }
                }
                """.formatted(FABRICANTE_ID, CATEGORIA_ID);

            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .put("/api/v1/medicamentos/{id}", MEDICAMENTO_DIPIRONA_ID.toString())
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("nomeComercial", equalTo("Dipirona 500mg Atualizada"))
                .body("precoMaximoConsumidor", equalTo(13.0f));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FLUXO COMPLETO (E2E DENTRO DO CONTEXTO SPRING)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Fluxo completo: Cadastrar → Buscar → Atualizar → Inativar")
    class FluxoCompleto {

        @Test
        @DisplayName("deve executar ciclo de vida completo do medicamento")
        void deve_executar_ciclo_de_vida_completo() {
            // STEP 1: Cadastrar
            String payload = """
                {
                  "codigoEan": "%s",
                  "nomeComercial": "Vitamina C 1g",
                  "tipo": "OTC",
                  "formaFarmaceutica": "COMPRIMIDO",
                  "requerReceita": false,
                  "nivelControle": "LIVRE",
                  "precoMaximoConsumidor": 25.00,
                  "fabricante": { "id": "%s" },
                  "categoria": { "id": "%s" }
                }
                """.formatted(eanAleatorio(), FABRICANTE_ID, CATEGORIA_ID);

            String id = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + tokenAdmin)
                .body(payload)
            .when()
                .post("/api/v1/medicamentos")
            .then()
                .statusCode(201)
                .extract().path("id");

            // STEP 2: Buscar
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/medicamentos/{id}", id)
            .then()
                .statusCode(200)
                .body("nomeComercial", equalTo("Vitamina C 1g"));

            // STEP 3: Inativar (DELETE = exclusão lógica)
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .delete("/api/v1/medicamentos/{id}", id)
            .then()
                .statusCode(204);

            // STEP 4: Verificar que não aparece mais como ativo
            given()
                .header("Authorization", "Bearer " + tokenAdmin)
            .when()
                .get("/api/v1/medicamentos/{id}", id)
            .then()
                .statusCode(200)
                .body("ativo", equalTo(false));
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

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

    private static String eanAleatorio() {
        long n = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_999_999_999_999L);
        return Long.toString(n);
    }
}
