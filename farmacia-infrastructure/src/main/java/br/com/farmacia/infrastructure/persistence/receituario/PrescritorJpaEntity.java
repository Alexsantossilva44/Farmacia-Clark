package br.com.farmacia.infrastructure.persistence.receituario;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code prescritores} (migration V4).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "prescritores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescritorJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "crm", nullable = false, length = 15)
    private String crm;

    @Column(name = "uf_crm", nullable = false, length = 2)
    private String ufCrm;

    @Column(name = "especialidade", length = 80)
    private String especialidade;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
