package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.enums.StatusLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link LoteJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface LoteJpaRepository extends JpaRepository<LoteJpaEntity, UUID> {

    Optional<LoteJpaEntity> findFirstByNumeroLote(String numeroLote);

    Optional<LoteJpaEntity> findByMedicamentoIdAndNumeroLote(UUID medicamentoId, String numeroLote);

    List<LoteJpaEntity> findByMedicamentoIdOrderByDataValidadeAsc(UUID medicamentoId);

    List<LoteJpaEntity> findByStatusAndDataValidadeBefore(StatusLote status, LocalDate data);

    /** Lotes disponíveis (ATIVO, com saldo) ordenados por validade ASC — FEFO. */
    @Query("""
        select l from LoteJpaEntity l
        where l.medicamentoId = :medicamentoId
          and l.status = br.com.farmacia.domain.estoque.enums.StatusLote.ATIVO
          and l.quantidadeAtual > 0
        order by l.dataValidade asc
        """)
    List<LoteJpaEntity> findLotesDisponivelFefo(@Param("medicamentoId") UUID medicamentoId);

    @Query("""
        select l from LoteJpaEntity l
        where l.status = :status
          and l.dataValidade <= :dataLimite
          and l.quantidadeAtual > 0
        order by l.dataValidade asc
        """)
    List<LoteJpaEntity> findLotesProximosVencer(@Param("status") StatusLote status,
                                                @Param("dataLimite") LocalDate dataLimite);

    long countByNotaFiscalId(UUID notaFiscalId);

    @Query("""
        select l.medicamentoId, coalesce(sum(l.quantidadeAtual), 0)
        from LoteJpaEntity l
        where l.status = br.com.farmacia.domain.estoque.enums.StatusLote.ATIVO
          and l.quantidadeAtual > 0
          and l.dataValidade >= CURRENT_DATE
        group by l.medicamentoId
        """)
    List<Object[]> sumDisponivelVendaGroupByMedicamento();
}
