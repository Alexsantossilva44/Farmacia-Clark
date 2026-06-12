package br.com.farmacia.infrastructure.persistence.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link PdvJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface PdvJpaRepository extends JpaRepository<PdvJpaEntity, UUID> {

    Optional<PdvJpaEntity> findByNumero(String numero);
}
