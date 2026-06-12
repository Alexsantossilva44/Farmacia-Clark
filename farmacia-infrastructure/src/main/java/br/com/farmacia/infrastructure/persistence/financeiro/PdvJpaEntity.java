package br.com.farmacia.infrastructure.persistence.financeiro;

import br.com.farmacia.domain.financeiro.enums.StatusPDV;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code pdvs} (migration V5).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "pdvs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdvJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero", nullable = false, length = 10, unique = true)
    private String numero;

    @Column(name = "descricao", length = 80)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusPDV status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
