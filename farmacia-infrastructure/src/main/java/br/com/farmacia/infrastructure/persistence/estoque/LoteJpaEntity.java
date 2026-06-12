package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.enums.StatusLote;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code lotes} (migration V2).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "lotes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    @Column(name = "nota_fiscal_id")
    private UUID notaFiscalId;

    @Column(name = "numero_lote", nullable = false, length = 30)
    private String numeroLote;

    @Column(name = "data_fabricacao")
    private LocalDate dataFabricacao;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Column(name = "quantidade_recebida", nullable = false)
    private Integer quantidadeRecebida;

    @Column(name = "quantidade_atual", nullable = false)
    private Integer quantidadeAtual;

    @Column(name = "preco_custo", precision = 10, scale = 2)
    private BigDecimal precoCusto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusLote status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
