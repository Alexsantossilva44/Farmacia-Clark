package br.com.farmacia.qa.bdd.config;

import br.com.farmacia.api.FarmaciaApplication;
import br.com.farmacia.application.sngpc.usecase.SngpcEventPublisher;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Configuração Spring compartilhada para todos os cenários Cucumber.
 *
 * <p>Prioridade do datasource:</p>
 * <ol>
 *   <li>Variáveis {@code BDD_DATASOURCE_URL} / {@code BDD_DATASOURCE_USERNAME} /
 *       {@code BDD_DATASOURCE_PASSWORD} (CI ou Postgres local manual)</li>
 *   <li>Testcontainers PostgreSQL (local/CI com Docker)</li>
 *   <li>Fallback: {@code docker compose up postgres} na porta 5433</li>
 * </ol>
 *
 * @author Alex Silva e Claude
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = FarmaciaApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");

        String externalUrl = System.getenv("BDD_DATASOURCE_URL");
        if (externalUrl != null && !externalUrl.isBlank()) {
            aplicarPostgres(registry, externalUrl,
                envOrDefault("BDD_DATASOURCE_USERNAME", "test"),
                envOrDefault("BDD_DATASOURCE_PASSWORD", "test"));
            return;
        }

        if (iniciarTestcontainers(registry)) {
            return;
        }

        aplicarPostgres(registry,
            "jdbc:postgresql://localhost:5433/farmacia_dev",
            "farmacia",
            "farmacia123");
    }

    private static void aplicarPostgres(DynamicPropertyRegistry registry,
                                        String url, String username, String password) {
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
    }

    private static boolean iniciarTestcontainers(DynamicPropertyRegistry registry) {
        try {
            postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("farmacia_test")
                .withUsername("test")
                .withPassword("test");
            postgres.start();
            aplicarPostgres(registry, postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @MockBean
    private SngpcEventPublisher sngpcEventPublisher;
}
