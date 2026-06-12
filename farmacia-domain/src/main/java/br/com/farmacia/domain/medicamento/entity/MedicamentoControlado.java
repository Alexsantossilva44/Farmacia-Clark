package br.com.farmacia.domain.medicamento.entity;

import lombok.*;
import java.util.UUID;

/**
 * Extensão de domínio: informações regulatórias de medicamento controlado.
 * Portaria ANVISA 344/98.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MedicamentoControlado {
    private UUID        id;
    private Medicamento medicamento;
    private String      portaria;
    private String      lista;
    private Integer     quantidadeMaximaReceita;
    private Integer     validadeReceitaDias;
    private Boolean     psicootropico;
    private Boolean     entorpecente;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao medicamento controlado");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void vincularMedicamento(Medicamento medicamento) {
        this.medicamento = medicamento;
    }
}
