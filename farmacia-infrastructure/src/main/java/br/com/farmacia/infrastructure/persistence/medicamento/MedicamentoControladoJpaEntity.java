package br.com.farmacia.infrastructure.persistence.medicamento;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code medicamentos_controlados} (migration V1).
 */
@Entity
@Table(name = "medicamentos_controlados")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoControladoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "medicamento_id", nullable = false, unique = true)
    private UUID medicamentoId;

    @Column(name = "portaria", nullable = false, length = 30)
    private String portaria;

    @Column(name = "lista", nullable = false, length = 10)
    private String lista;

    @Column(name = "quantidade_maxima_receita", nullable = false)
    private Integer quantidadeMaximaReceita;

    @Column(name = "validade_receita_dias", nullable = false)
    private Integer validadeReceitaDias;

    @Column(name = "psicootropico", nullable = false)
    private Boolean psicootropico;

    @Column(name = "entorpecente", nullable = false)
    private Boolean entorpecente;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
