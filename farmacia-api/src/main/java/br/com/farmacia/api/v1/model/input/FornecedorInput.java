package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FornecedorInput {
    @NotBlank
    private String razaoSocial;

    private String nomeFantasia;

    @NotBlank private String cnpj;
}
