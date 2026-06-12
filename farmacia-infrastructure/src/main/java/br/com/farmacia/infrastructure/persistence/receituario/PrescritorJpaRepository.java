package br.com.farmacia.infrastructure.persistence.receituario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link PrescritorJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface PrescritorJpaRepository extends JpaRepository<PrescritorJpaEntity, UUID> {

    Optional<PrescritorJpaEntity> findByCrmAndUfCrm(String crm, String ufCrm);
}
