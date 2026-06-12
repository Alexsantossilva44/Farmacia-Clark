package br.com.farmacia.api.v1.model.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ValidarReceitaInput {
    @NotEmpty @Valid
    private List<ItemValidacaoInput> itens;

    @Getter @Setter
    public static class ItemValidacaoInput {
        @NotNull private UUID medicamentoId;
        @NotNull @Min(1) private Integer quantidade;
    }
}
