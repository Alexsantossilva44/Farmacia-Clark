package br.com.farmacia.infrastructure.persistence.financeiro;

import br.com.farmacia.domain.financeiro.enums.StatusCaixa;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code caixas} (migration V5).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "caixas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaixaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pdv_id", nullable = false)
    private UUID pdvId;

    @Column(name = "funcionario_id", nullable = false)
    private UUID funcionarioId;

    @Column(name = "abertura", nullable = false)
    private LocalDateTime abertura;

    @Column(name = "fechamento")
    private LocalDateTime fechamento;

    @Column(name = "saldo_abertura", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoAbertura;

    @Column(name = "saldo_fechamento", precision = 12, scale = 2)
    private BigDecimal saldoFechamento;

    @Column(name = "total_vendas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVendas;

    @Column(name = "total_entradas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEntradas;

    @Column(name = "total_saidas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSaidas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusCaixa status;

    @Column(name = "observacao")
    private String observacao;
}
