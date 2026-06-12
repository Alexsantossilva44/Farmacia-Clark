package br.com.farmacia.infrastructure.persistence.venda;

import br.com.farmacia.domain.venda.enums.FormaPagamento;
import br.com.farmacia.domain.venda.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code pagamentos} (migration V5).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    private VendaJpaEntity venda;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma", nullable = false, length = 20)
    private FormaPagamento forma;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "troco", nullable = false, precision = 12, scale = 2)
    private BigDecimal troco;

    @Column(name = "nsu", length = 30)
    private String nsu;

    @Column(name = "autorizacao", length = 30)
    private String autorizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPagamento status;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}
