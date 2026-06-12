package br.com.farmacia.domain.receituario.entity;

import lombok.*;
import java.util.UUID;

/**
 * Entidade de domínio: Médico/Dentista prescritor.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Prescritor {

    private UUID    id;
    private String  nome;
    private String  crm;
    private String  ufCrm;
    private String  especialidade;
    private String  email;
    private Boolean ativo;

    public boolean temCrmValido() {
        return crm != null && !crm.isBlank();
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao prescritor");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
