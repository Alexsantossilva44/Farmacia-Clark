package br.com.farmacia.infrastructure.persistence.medicamento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link MedicamentoControladoJpaEntity}.
 */
public interface MedicamentoControladoJpaRepository extends JpaRepository<MedicamentoControladoJpaEntity, UUID> {

    Optional<MedicamentoControladoJpaEntity> findByMedicamentoId(UUID medicamentoId);
}
