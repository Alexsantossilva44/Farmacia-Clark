package br.com.farmacia.qa.integration;

import br.com.farmacia.api.FarmaciaApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de fumaça do contexto da aplicação.
 *
 * <p>Sobe o {@code ApplicationContext} completo contra um PostgreSQL real,
 * executando as migrations Flyway (o schema é gerido pelo Flyway, então
 * {@code ddl-auto=none}, como em produção). Garante que todos os adapters de
 * persistência, casos de uso, schedulers e a topologia de mensageria estão
 * corretamente conectados e que o metamodelo JPA é construído.</p>
 *
 * <p>O {@code ConnectionFactory} do RabbitMQ é mockado porque o
 * {@code RabbitAutoConfiguration} é desabilitado no perfil de teste — assim a
 * topologia de mensageria é construída sem um broker real.</p>
 *
 * <p><b>Pré-requisito</b>: um PostgreSQL acessível em
 * {@code localhost:5435} (db {@code farmacia_test}, usuário/senha
 * {@code test}). Sobrescreva via {@code -Dspring.datasource.url} se necessário.</p>
 *
 * @author Alex Silva e Claude
 */
@SpringBootTest(classes = FarmaciaApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5435/farmacia_test",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("Contexto da Aplicação — Boot completo")
class ContextoAplicacaoIT {

    @MockBean
    private ConnectionFactory rabbitConnectionFactory;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("deve carregar o contexto com todos os adapters de persistência")
    void deve_carregar_o_contexto_completo() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionCount()).isPositive();
    }
}
