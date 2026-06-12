package br.com.farmacia.infrastructure.persistence.compra;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itens_pedido_compra")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoCompraJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_compra_id", nullable = false)
    private PedidoCompraJpaEntity pedidoCompra;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    @Column(name = "quantidade_solicitada", nullable = false)
    private Integer quantidadeSolicitada;

    @Column(name = "quantidade_recebida", nullable = false)
    private Integer quantidadeRecebida;

    @Column(name = "preco_unitario", precision = 10, scale = 2)
    private BigDecimal precoUnitario;
}
