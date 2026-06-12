package br.com.farmacia.api.v1.model.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PedidoCompraInput {
    @NotNull private UUID fornecedorId;
    private LocalDate dataPedido;
    private LocalDate dataEntregaPrevista;
    private String observacao;
    @NotEmpty @Valid private List<ItemPedidoCompraInput> itens;
}
