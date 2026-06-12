package br.com.farmacia.qa.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.*;

/**
 * Runner principal do Cucumber.
 *
 * <p><b>Heurística Júlio de Lima</b>: o runner une a infraestrutura
 * JUnit 5 com o Cucumber. Configurar {@code GLUE} apontando para o
 * pacote dos steps e {@code FEATURES} para o diretório correto.</p>
 *
 * <p>Execute somente os testes BDD com a tag {@code @smoke} via:</p>
 * <pre>{@code mvn test -Dcucumber.filter.tags="@smoke"}</pre>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")   // src/test/resources/features
@ConfigurationParameters({
    @ConfigurationParameter(
        key = Constants.GLUE_PROPERTY_NAME,
        value = "br.com.farmacia.qa.bdd.config,br.com.farmacia.qa.bdd.steps"
    ),
    @ConfigurationParameter(
        key = Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/index.html, " +
                "json:target/cucumber-reports/cucumber.json, " +
                "junit:target/cucumber-reports/cucumber.xml"
    ),
    @ConfigurationParameter(
        key = Constants.FEATURES_PROPERTY_NAME,
        value = "src/test/resources/features"
    ),
    @ConfigurationParameter(
        key = Constants.EXECUTION_DRY_RUN_PROPERTY_NAME,
        value = "false"
    )
})
public class CucumberRunnerIT {
    /*
     * Esta classe só precisa das annotations acima.
     * O JUnit Platform encontra e executa os cenários automaticamente.
     *
     * Tags disponíveis para filtro:
     *   @smoke       — testes rápidos para CI
     *   @regressao   — suíte completa
     *   @controlado  — cenários de medicamento controlado
     *   @estoque     — cenários de estoque
     *   @sngpc       — cenários de integração SNGPC
     */
}

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * DEPENDÊNCIAS MAVEN NECESSÁRIAS PARA BDD COM CUCUMBER
 * Adicione ao pom.xml dentro de <dependencies>:
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * <!-- Cucumber BDD — Heurística Júlio de Lima -->
 *
 * <dependency>
 *     <groupId>io.cucumber</groupId>
 *     <artifactId>cucumber-java</artifactId>
 *     <version>7.15.0</version>
 *     <scope>test</scope>
 * </dependency>
 *
 * <dependency>
 *     <groupId>io.cucumber</groupId>
 *     <artifactId>cucumber-spring</artifactId>
 *     <version>7.15.0</version>
 *     <scope>test</scope>
 * </dependency>
 *
 * <dependency>
 *     <groupId>io.cucumber</groupId>
 *     <artifactId>cucumber-junit-platform-engine</artifactId>
 *     <version>7.15.0</version>
 *     <scope>test</scope>
 * </dependency>
 *
 * <dependency>
 *     <groupId>org.junit.platform</groupId>
 *     <artifactId>junit-platform-suite</artifactId>
 *     <scope>test</scope>
 * </dependency>
 *
 * <!-- REST Assured — para testes de integração de API -->
 * <dependency>
 *     <groupId>io.rest-assured</groupId>
 *     <artifactId>rest-assured</artifactId>
 *     <scope>test</scope>
 * </dependency>
 *
 * <dependency>
 *     <groupId>io.rest-assured</groupId>
 *     <artifactId>spring-mock-mvc</artifactId>
 *     <scope>test</scope>
 * </dependency>
 */
