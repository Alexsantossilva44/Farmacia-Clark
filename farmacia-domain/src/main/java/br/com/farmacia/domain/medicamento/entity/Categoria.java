package br.com.farmacia.domain.medicamento.entity;

import lombok.*;
import java.util.UUID;

/**
 * Entidade de domínio: Categoria de medicamento (árvore).
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Categoria {
    private UUID      id;
    private String    nome;
    private String    descricao;
    private Categoria categoriaPai;
    private Boolean   ativo;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída à categoria");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
