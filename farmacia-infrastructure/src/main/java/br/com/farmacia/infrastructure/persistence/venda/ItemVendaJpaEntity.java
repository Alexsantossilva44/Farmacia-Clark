package br.com.farmacia.infrastructure.persistence.venda;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code itens_venda} (migration V5).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "itens_venda")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemVendaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    private VendaJpaEntity venda;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
