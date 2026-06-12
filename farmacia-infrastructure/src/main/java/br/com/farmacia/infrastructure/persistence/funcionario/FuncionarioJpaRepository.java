package br.com.farmacia.infrastructure.persistence.funcionario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link FuncionarioJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface FuncionarioJpaRepository extends JpaRepository<FuncionarioJpaEntity, UUID> {

    Optional<FuncionarioJpaEntity> findByEmail(String email);
}
