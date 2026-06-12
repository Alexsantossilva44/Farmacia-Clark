package br.com.farmacia.domain.medicamento.entity;

import lombok.*;
import java.util.UUID;

/**
 * Entidade de domínio: Fabricante do medicamento.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class Fabricante {
    private UUID   id;
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String autorizacaoAnvisa;
    private String email;
    private String telefone;
    private Boolean ativo;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao fabricante");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
