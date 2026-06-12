package br.com.farmacia.domain.funcionario.entity;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade de domínio: Funcionário da farmácia.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "senhaHash")
public class Funcionario {

    private UUID      id;
    private String    nome;
    private String    cpf;
    private String    email;
    private String    senhaHash;
    private String    telefone;
    private Cargo     cargo;
    private LocalDate dataAdmissao;
    private LocalDate dataDemissao;

    @Builder.Default
    private Boolean   ativo = true;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao funcionário");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
