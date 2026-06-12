package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ItemPedidoCompraModel {
    private UUID id;
    private UUID medicamentoId;
    private String medicamentoNome;
    private Integer quantidadeSolicitada;
    private Integer quantidadeRecebida;
    private Integer quantidadePendente;
    private BigDecimal precoUnitario;
}
