package br.com.farmacia.domain.venda.entity;

import br.com.farmacia.domain.venda.enums.FormaPagamento;
import br.com.farmacia.domain.venda.enums.StatusPagamento;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade: Pagamento de uma venda.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Pagamento {

    private UUID           id;
    private Venda          venda;
    private FormaPagamento forma;
    private BigDecimal     valor;

    @Builder.Default
    private BigDecimal troco = BigDecimal.ZERO;

    private String         nsu;
    private String         autorizacao;

    @Builder.Default
    private StatusPagamento status = StatusPagamento.PENDENTE;

    private LocalDateTime  dataHora;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao pagamento");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    /** H-01: registra o troco calculado (totalPago - totalVenda) no pagamento em dinheiro. */
    public void registrarTroco(BigDecimal valorTroco) {
        if (valorTroco == null || valorTroco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Troco não pode ser negativo");
        }
        this.troco = valorTroco;
    }

    /** C-03: marca pagamento como estornado ao cancelar a venda correspondente. */
    public void cancelar() {
        this.status = StatusPagamento.ESTORNADO;
    }
}
