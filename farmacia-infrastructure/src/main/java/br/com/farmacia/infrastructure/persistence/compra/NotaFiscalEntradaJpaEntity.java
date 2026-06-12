package br.com.farmacia.infrastructure.persistence.compra;

import br.com.farmacia.domain.compra.enums.StatusNota;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notas_fiscais_entrada")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotaFiscalEntradaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "fornecedor_id", nullable = false)
    private UUID fornecedorId;

    @Column(name = "pedido_compra_id")
    private UUID pedidoCompraId;

    @Column(name = "numero_nota", nullable = false, length = 20)
    private String numeroNota;

    @Column(name = "serie", length = 5)
    private String serie;

    @Column(name = "chave_acesso", unique = true, length = 44)
    private String chaveAcesso;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;

    @Column(name = "valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusNota status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
