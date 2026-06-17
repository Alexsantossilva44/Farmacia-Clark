package br.com.farmacia.infrastructure.persistence.medicamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repositório Spring Data para {@link FabricanteJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface FabricanteJpaRepository extends JpaRepository<FabricanteJpaEntity, UUID> {

    boolean existsByCnpj(String cnpj);

    @Query("SELECT COUNT(f) FROM FabricanteJpaEntity f " +
           "WHERE LOWER(f.razaoSocial) = LOWER(:razaoSocial) " +
           "AND (f.ativo IS NULL OR f.ativo = TRUE)")
    long countByRazaoSocialAtivo(@Param("razaoSocial") String razaoSocial);
}
