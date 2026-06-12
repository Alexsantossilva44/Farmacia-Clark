package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaInput {
    @NotBlank(message = "Nome da categoria é obrigatório")
    @Size(max = 100)
    private String nome;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 500)
    private String descricao;
}
