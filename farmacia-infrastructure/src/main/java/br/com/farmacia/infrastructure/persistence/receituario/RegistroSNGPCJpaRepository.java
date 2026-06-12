package br.com.farmacia.infrastructure.persistence.receituario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link RegistroSNGPCJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface RegistroSNGPCJpaRepository extends JpaRepository<RegistroSNGPCJpaEntity, UUID> {

    @Query("""
        select r from RegistroSNGPCJpaEntity r
        where r.statusEnvio in (
              br.com.farmacia.domain.receituario.enums.StatusEnvio.PENDENTE,
              br.com.farmacia.domain.receituario.enums.StatusEnvio.ERRO)
          and r.tentativasEnvio < :maxTentativas
          and r.dataRegistro <= :limite
        order by r.dataRegistro asc
        """)
    List<RegistroSNGPCJpaEntity> findPendentesParaReprocessamento(
        @Param("limite") LocalDateTime limite,
        @Param("maxTentativas") int maxTentativas);
}
