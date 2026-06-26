package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * DTO de cadastro de fabricante (Cadastros → Fabricantes).
 *
 * <p>Alteração: {@code cnpj} passou a ser obrigatório ({@code @NotBlank}), alinhado à
 * coluna {@code fabricantes.cnpj CHAR(14) NOT NULL UNIQUE} (migration V1).</p>
 */
@Getter
@Setter
public class FabricanteInput {
    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 80)
    private String razaoSocial;

    @Size(max = 80)
    private String nomeFantasia;

    /** CNPJ com 14 dígitos — obrigatório no banco; aceita até 18 chars com máscara na API. */
    @NotBlank(message = "CNPJ é obrigatório")
    @CNPJ(message = "CNPJ inválido.")
    @Size(max = 18)
    private String cnpj;
}
