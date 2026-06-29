package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrescritorInput {
    @NotBlank(message = "Lembre-se: Campo Obrigatório.")
    @Size(max = 150)
    @Pattern(
        regexp = "^[\\p{L}]+(?: [\\p{L}]+)+$",
        message = "Nome deve conter apenas letras e um espaço entre nome e sobrenome")
    private String nome;

    @NotBlank(message = "Lembre-se: Campo Obrigatório.")
    @Size(max = 15)
    private String crm;

    @NotBlank(message = "Lembre-se: Campo Obrigatório.")
    @Size(min = 2, max = 2)
    private String ufCrm;

    @NotBlank(message = "Lembre-se: Campo Obrigatório.")
    @Size(max = 80)
    private String especialidade;

    @Size(max = 120)
    private String email;
}
