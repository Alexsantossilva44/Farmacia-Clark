package br.com.farmacia.domain.estoque.entity;

import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade: Registro de movimentação de estoque (trilha de auditoria).
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MovimentacaoEstoque {

    private UUID               id;
    private Lote               lote;
    private Medicamento        medicamento;
    private TipoMovimentacao   tipo;
    private Integer            quantidade;
    private Integer            saldoAnterior;
    private Integer            saldoPosterior;
    private UUID               referenciaId;
    private String             motivoAjuste;
    private LocalDateTime      dataHora;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída à movimentação de estoque");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
