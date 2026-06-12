package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FecharCaixaInput {
    @NotNull private UUID pdvId;
    private String observacao;
}
