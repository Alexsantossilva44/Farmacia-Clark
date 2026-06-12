package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class AbrirCaixaInput {
    @NotNull private UUID pdvId;
    @NotNull private UUID funcionarioId;
    private BigDecimal saldoAbertura;
}
