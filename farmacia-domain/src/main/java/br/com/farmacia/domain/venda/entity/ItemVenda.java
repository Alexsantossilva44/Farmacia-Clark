package br.com.farmacia.domain.venda.entity;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidade: Item individual de uma venda.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ItemVenda {

    private UUID        id;
    private Venda       venda;
    private Medicamento medicamento;
    private Lote        lote;
    private Integer     quantidade;
    private BigDecimal  precoUnitario;

    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    private BigDecimal subtotal;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao item de venda");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
