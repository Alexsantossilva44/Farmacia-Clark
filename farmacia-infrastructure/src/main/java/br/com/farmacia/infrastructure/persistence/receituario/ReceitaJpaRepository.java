package br.com.farmacia.infrastructure.persistence.receituario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link ReceitaJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface ReceitaJpaRepository extends JpaRepository<ReceitaJpaEntity, UUID> {

    Optional<ReceitaJpaEntity> findFirstByNumeroReceita(String numeroReceita);
}
