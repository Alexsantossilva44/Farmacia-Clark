package br.com.farmacia.api.v1.model.input;

import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AjusteSaldoInput {
    @NotNull private UUID medicamentoId;
    @NotNull private UUID loteId;
    @NotNull private TipoMovimentacao tipo;
    @NotNull @Min(1) private Integer quantidade;
    @NotBlank @Size(min = 10, max = 500) private String motivo;
}
