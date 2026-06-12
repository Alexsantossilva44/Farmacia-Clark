package br.com.farmacia.infrastructure.persistence.compra;

import br.com.farmacia.domain.compra.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PedidoCompraJpaRepository extends JpaRepository<PedidoCompraJpaEntity, UUID> {

    List<PedidoCompraJpaEntity> findAllByOrderByDataPedidoDescCreatedAtDesc();

    @Query("""
        SELECT DISTINCT p FROM PedidoCompraJpaEntity p
        LEFT JOIN FETCH p.itens
        ORDER BY p.dataPedido DESC, p.createdAt DESC
        """)
    List<PedidoCompraJpaEntity> findAllWithItensOrderByDataPedidoDesc();

    @Query("""
        SELECT p FROM PedidoCompraJpaEntity p
        WHERE p.fornecedorId = :fornecedorId
          AND p.status IN :statuses
        ORDER BY p.dataPedido DESC
        """)
    List<PedidoCompraJpaEntity> findByFornecedorAndStatusIn(
        @Param("fornecedorId") UUID fornecedorId,
        @Param("statuses") List<StatusPedido> statuses
    );

    @Query("""
        SELECT DISTINCT p FROM PedidoCompraJpaEntity p
        LEFT JOIN FETCH p.itens
        WHERE p.id = :id
        """)
    java.util.Optional<PedidoCompraJpaEntity> findByIdWithItens(@Param("id") UUID id);
}
