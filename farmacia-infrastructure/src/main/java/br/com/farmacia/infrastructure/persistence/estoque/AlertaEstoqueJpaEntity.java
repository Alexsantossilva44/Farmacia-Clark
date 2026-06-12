package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code alertas_estoque} (migration V2).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "alertas_estoque")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaEstoqueJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    @Column(name = "lote_id")
    private UUID loteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoAlerta tipo;

    @Column(name = "mensagem", nullable = false)
    private String mensagem;

    @Column(name = "data_geracao", nullable = false)
    private LocalDateTime dataGeracao;

    @Column(name = "lido", nullable = false)
    private Boolean lido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusAlerta status;
}
