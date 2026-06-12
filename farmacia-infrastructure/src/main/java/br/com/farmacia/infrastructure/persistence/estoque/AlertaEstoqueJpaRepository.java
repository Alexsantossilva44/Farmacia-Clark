package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link AlertaEstoqueJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface AlertaEstoqueJpaRepository extends JpaRepository<AlertaEstoqueJpaEntity, UUID> {

    boolean existsByLoteIdAndTipoAndStatus(UUID loteId, TipoAlerta tipo, StatusAlerta status);

    boolean existsByMedicamentoIdAndTipoAndStatus(UUID medicamentoId, TipoAlerta tipo, StatusAlerta status);

    List<AlertaEstoqueJpaEntity> findByLoteIdAndTipo(UUID loteId, TipoAlerta tipo);

    List<AlertaEstoqueJpaEntity> findByMedicamentoId(UUID medicamentoId);

    List<AlertaEstoqueJpaEntity> findByMedicamentoIdAndTipo(UUID medicamentoId, TipoAlerta tipo);

    List<AlertaEstoqueJpaEntity> findByStatus(StatusAlerta status);

    long countByLoteIdAndTipo(UUID loteId, TipoAlerta tipo);

    void deleteByLoteId(UUID loteId);

    void deleteByMedicamentoId(UUID medicamentoId);

    @Modifying
    @Query("""
        delete from AlertaEstoqueJpaEntity a
        where a.tipo = :tipo
          and a.medicamentoId in (
              select m.id from MedicamentoJpaEntity m where m.nomeComercial = :nome
          )
        """)
    void deleteByMedicamentoNomeAndTipo(@Param("nome") String nome, @Param("tipo") TipoAlerta tipo);
}
