package br.com.farmacia.infrastructure.persistence.estoque;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code itens_estoque} (migration V2).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "itens_estoque")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemEstoqueJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "medicamento_id", nullable = false, unique = true)
    private UUID medicamentoId;

    @Column(name = "quantidade_atual", nullable = false)
    private Integer quantidadeAtual;

    @Column(name = "quantidade_minima", nullable = false)
    private Integer quantidadeMinima;

    @Column(name = "quantidade_maxima", nullable = false)
    private Integer quantidadeMaxima;

    @Column(name = "ultima_movimentacao")
    private LocalDateTime ultimaMovimentacao;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
