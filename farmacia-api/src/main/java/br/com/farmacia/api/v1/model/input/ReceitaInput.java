package br.com.farmacia.api.v1.model.input;

import br.com.farmacia.domain.receituario.enums.TipoReceita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ReceitaInput {
    @NotBlank private String numeroReceita;
    @NotNull private TipoReceita tipo;
    private LocalDate dataEmissao;
    @NotNull private UUID prescritorId;
    private UUID clienteId;
    private String cid;
}
