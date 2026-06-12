package br.com.farmacia.infrastructure.persistence.medicamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link MedicamentoJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface MedicamentoJpaRepository extends JpaRepository<MedicamentoJpaEntity, UUID> {

    Optional<MedicamentoJpaEntity> findFirstByNomeComercial(String nomeComercial);

    Optional<MedicamentoJpaEntity> findByCodigoEan(String codigoEan);

    Optional<MedicamentoJpaEntity> findByCodigoAnvisa(String codigoAnvisa);

    boolean existsByCodigoEan(String codigoEan);

    Page<MedicamentoJpaEntity> findAllByOrderByNomeComercialAsc(Pageable pageable);

    @Query("""
        select m from MedicamentoJpaEntity m
        where lower(m.nomeComercial) like lower(concat('%', :busca, '%'))
            or (m.nomeGenerico is not null and lower(m.nomeGenerico) like lower(concat('%', :busca, '%')))
            or (m.codigoEan is not null and m.codigoEan like concat('%', :busca, '%'))
        order by m.nomeComercial asc
        """)
    Page<MedicamentoJpaEntity> buscarOrdenados(@Param("busca") String busca, Pageable pageable);

    Page<MedicamentoJpaEntity> findByAtivoTrueOrderByNomeComercialAsc(Pageable pageable);

    @Query("""
        select m from MedicamentoJpaEntity m
        where m.ativo = true
          and lower(m.nomeComercial) like lower(concat('%', :nome, '%'))
        order by m.nomeComercial asc
        """)
    Page<MedicamentoJpaEntity> buscarAtivosOrdenados(@Param("nome") String nome, Pageable pageable);
}
