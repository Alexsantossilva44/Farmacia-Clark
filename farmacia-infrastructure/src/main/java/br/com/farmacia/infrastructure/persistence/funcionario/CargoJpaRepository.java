package br.com.farmacia.infrastructure.persistence.funcionario;

import br.com.farmacia.domain.funcionario.enums.RoleSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link CargoJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface CargoJpaRepository extends JpaRepository<CargoJpaEntity, UUID> {

    Optional<CargoJpaEntity> findByRoleSistema(RoleSistema roleSistema);
}
