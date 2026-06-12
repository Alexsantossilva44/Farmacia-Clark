package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ItemPedidoCompraInput {
    @NotNull private UUID medicamentoId;
    @NotNull @Min(1) private Integer quantidadeSolicitada;
    private BigDecimal precoUnitario;
}
