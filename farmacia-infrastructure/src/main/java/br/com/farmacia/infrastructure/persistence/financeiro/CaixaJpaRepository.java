package br.com.farmacia.infrastructure.persistence.financeiro;

import br.com.farmacia.domain.financeiro.enums.StatusCaixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link CaixaJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface CaixaJpaRepository extends JpaRepository<CaixaJpaEntity, UUID> {

    Optional<CaixaJpaEntity> findFirstByPdvIdAndStatus(UUID pdvId, StatusCaixa status);
}
