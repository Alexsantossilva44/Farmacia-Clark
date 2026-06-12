package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code movimentacoes_estoque} (migration V2).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "movimentacoes_estoque")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoqueJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimentacao tipo;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "saldo_anterior", nullable = false)
    private Integer saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false)
    private Integer saldoPosterior;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "motivo_ajuste")
    private String motivoAjuste;

    @Column(name = "funcionario_id")
    private UUID funcionarioId;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}
