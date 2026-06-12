package br.com.farmacia.infrastructure.persistence.compra;

import br.com.farmacia.domain.compra.enums.StatusPedido;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos_compra")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoCompraJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "fornecedor_id", nullable = false)
    private UUID fornecedorId;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

    @Column(name = "data_entrega_prevista")
    private LocalDate dataEntregaPrevista;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPedido status;

    @Column(name = "valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "observacao")
    private String observacao;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pedidoCompra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemPedidoCompraJpaEntity> itens = new ArrayList<>();
}
