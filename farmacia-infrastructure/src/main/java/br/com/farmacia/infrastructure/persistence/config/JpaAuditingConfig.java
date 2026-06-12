package br.com.farmacia.infrastructure.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita a auditoria JPA ({@code @CreatedDate}, {@code @LastModifiedDate}).
 *
 * <p>Fica isolada no módulo de infraestrutura — e não na classe principal —
 * para que os testes de fatia web ({@code @WebMvcTest}) não tentem inicializar
 * o {@code jpaAuditingHandler} (que exige um metamodel JPA não vazio).</p>
 *
 * @author Alex Silva e Claude
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
