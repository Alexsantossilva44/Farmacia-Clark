package br.com.farmacia.domain.funcionario.entity;

import br.com.farmacia.domain.funcionario.enums.RoleSistema;
import lombok.*;
import java.util.UUID;

/**
 * Entidade: Cargo do funcionário com role de segurança.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cargo {
    private UUID        id;
    private String      nome;
    private String      descricao;
    private RoleSistema roleSistema;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao cargo");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
