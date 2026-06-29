package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrescritorInput {
    @NotBlank(message = "Nome do prescritor é obrigatório")
    @Size(max = 150)
    @Pattern(
        regexp = "^[\\p{L}]+(?: [\\p{L}]+)+$",
        message = "Nome deve conter apenas letras e um espaço entre nome e sobrenome")
    private String nome;

    @NotBlank(message = "CRM é obrigatório")
    @Size(max = 15)
    private String crm;

    @NotBlank(message = "UF do CRM é obrigatória")
    @Size(min = 2, max = 2)
    private String ufCrm;

    @NotBlank(message = "Especialidade é obrigatória")
    @Size(max = 80)
    private String especialidade;

    @Size(max = 120)
    private String email;
}
