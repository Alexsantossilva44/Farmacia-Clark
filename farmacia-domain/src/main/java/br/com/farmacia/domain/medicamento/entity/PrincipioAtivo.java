package br.com.farmacia.domain.medicamento.entity;

import lombok.*;
import java.util.UUID;

/**
 * Entidade de domínio: Princípio Ativo (DCB — Denominação Comum Brasileira).
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PrincipioAtivo {
    private UUID   id;
    private String nome;
    private String dcb;   // Denominação Comum Brasileira
    private String cas;   // Chemical Abstracts Service

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao princípio ativo");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
