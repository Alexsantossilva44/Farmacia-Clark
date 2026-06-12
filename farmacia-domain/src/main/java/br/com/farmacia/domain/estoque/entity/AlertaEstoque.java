package br.com.farmacia.domain.estoque.entity;

import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade: Alerta automático de estoque gerado pelos schedulers.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AlertaEstoque {

    private UUID          id;
    private Medicamento   medicamento;
    private Lote          lote;
    private TipoAlerta    tipo;
    private String        mensagem;
    private LocalDateTime dataGeracao;
    private Boolean       lido;
    private StatusAlerta  status;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao alerta de estoque");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
