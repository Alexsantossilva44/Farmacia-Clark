package br.com.farmacia.infrastructure.persistence.estoque;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositório Spring Data para {@link MovimentacaoEstoqueJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface MovimentacaoEstoqueJpaRepository
        extends JpaRepository<MovimentacaoEstoqueJpaEntity, UUID> {

    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM MovimentacaoEstoqueJpaEntity m
        WHERE (:medicamentoId IS NULL OR m.medicamentoId = :medicamentoId)
          AND (:tipo IS NULL OR m.tipo = :tipo)
        ORDER BY m.dataHora DESC
        """)
    org.springframework.data.domain.Page<MovimentacaoEstoqueJpaEntity> buscarPorFiltro(
        @org.springframework.data.repository.query.Param("medicamentoId") UUID medicamentoId,
        @org.springframework.data.repository.query.Param("tipo") br.com.farmacia.domain.estoque.enums.TipoMovimentacao tipo,
        org.springframework.data.domain.Pageable pageable
    );
}
