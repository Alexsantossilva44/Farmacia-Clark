package br.com.farmacia.infrastructure.persistence.funcionario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link FarmaceuticoJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface FarmaceuticoJpaRepository extends JpaRepository<FarmaceuticoJpaEntity, UUID> {

    Optional<FarmaceuticoJpaEntity> findByFuncionarioId(UUID funcionarioId);
}
