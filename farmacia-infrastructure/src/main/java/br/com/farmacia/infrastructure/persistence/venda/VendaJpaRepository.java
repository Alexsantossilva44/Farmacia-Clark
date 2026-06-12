package br.com.farmacia.infrastructure.persistence.venda;

import br.com.farmacia.domain.venda.enums.StatusVenda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link VendaJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface VendaJpaRepository extends JpaRepository<VendaJpaEntity, UUID> {

    @Query("""
        select v from VendaJpaEntity v
        where (:dataInicio is null or v.dataHora >= :dataInicio)
          and (:dataFim    is null or v.dataHora <= :dataFim)
          and (:clienteId  is null or v.clienteId = :clienteId)
          and (:pdvId      is null or v.pdvId = :pdvId)
          and (:status     is null or v.status = :status)
        order by v.dataHora desc
        """)
    List<VendaJpaEntity> findWithFilters(@Param("dataInicio") LocalDateTime dataInicio,
                                         @Param("dataFim") LocalDateTime dataFim,
                                         @Param("clienteId") UUID clienteId,
                                         @Param("pdvId") UUID pdvId,
                                         @Param("status") StatusVenda status);

    // H-05: versão paginada — delega limit/offset ao BD em vez de carregar tudo em memória
    @Query("""
        select v from VendaJpaEntity v
        where (:dataInicio is null or v.dataHora >= :dataInicio)
          and (:dataFim    is null or v.dataHora <= :dataFim)
          and (:clienteId  is null or v.clienteId = :clienteId)
          and (:pdvId      is null or v.pdvId = :pdvId)
          and (:status     is null or v.status = :status)
        order by v.dataHora desc
        """)
    List<VendaJpaEntity> findWithFiltersPaginated(@Param("dataInicio") LocalDateTime dataInicio,
                                                   @Param("dataFim") LocalDateTime dataFim,
                                                   @Param("clienteId") UUID clienteId,
                                                   @Param("pdvId") UUID pdvId,
                                                   @Param("status") StatusVenda status,
                                                   Pageable pageable);

    @Query("""
        select count(v) from VendaJpaEntity v
        where (:dataInicio is null or v.dataHora >= :dataInicio)
          and (:dataFim    is null or v.dataHora <= :dataFim)
          and (:clienteId  is null or v.clienteId = :clienteId)
          and (:pdvId      is null or v.pdvId = :pdvId)
          and (:status     is null or v.status = :status)
        """)
    long countWithFilters(@Param("dataInicio") LocalDateTime dataInicio,
                           @Param("dataFim") LocalDateTime dataFim,
                           @Param("clienteId") UUID clienteId,
                           @Param("pdvId") UUID pdvId,
                           @Param("status") StatusVenda status);
}
