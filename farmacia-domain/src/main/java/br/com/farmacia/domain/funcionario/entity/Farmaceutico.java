package br.com.farmacia.domain.funcionario.entity;

import lombok.*;
import java.util.UUID;

/**
 * Entidade: Farmacêutico (extensão de Funcionário).
 * Responsável pela validação de receitas controladas e Responsável Técnico.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Farmaceutico {

    private UUID       id;
    private Funcionario funcionario;
    private String     crf;
    private String     ufCrf;
    private String     especialidades;
    private Boolean    responsavelTecnico;

    @Builder.Default
    private Boolean ativo = true;

    public boolean temCrfValido() {
        return crf != null && !crf.isBlank();
    }

    public boolean isAtivo() {
        return Boolean.TRUE.equals(ativo);
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao farmacêutico");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
