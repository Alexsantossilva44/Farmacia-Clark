package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PedidoCompraModel {
    private UUID id;
    private UUID fornecedorId;
    private String fornecedorNome;
    private LocalDate dataPedido;
    private LocalDate dataEntregaPrevista;
    private String status;
    private BigDecimal valorTotal;
    private String observacao;
    private LocalDateTime createdAt;
    private Integer quantidadePendente;
    private List<ItemPedidoCompraModel> itens;
}
