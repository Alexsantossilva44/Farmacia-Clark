package br.com.farmacia.infrastructure.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registra as entidades JPA e os repositórios Spring Data localizados no
 * módulo de infraestrutura.
 *
 * <p>É necessária porque a classe principal vive em {@code br.com.farmacia.api}
 * e, por padrão, o Spring Boot só varreria esse pacote em busca de
 * {@code @Entity} e repositórios. Mantida fora das fatias {@code @WebMvcTest}
 * (que não carregam configurações de infraestrutura).</p>
 *
 * @author Alex Silva e Claude
 */
@Configuration
@EntityScan(basePackages = "br.com.farmacia.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "br.com.farmacia.infrastructure.persistence")
public class PersistenceJpaConfig {
}
