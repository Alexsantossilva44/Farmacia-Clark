package br.com.farmacia.infrastructure.persistence.medicamento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositório Spring Data para {@link CategoriaJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface CategoriaJpaRepository extends JpaRepository<CategoriaJpaEntity, UUID> {
}
