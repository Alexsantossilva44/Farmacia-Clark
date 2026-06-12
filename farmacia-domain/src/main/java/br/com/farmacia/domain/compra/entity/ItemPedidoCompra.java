package br.com.farmacia.domain.compra.entity;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoCompra {

    private UUID id;
    private UUID pedidoCompraId;
    private UUID medicamentoId;
    private String medicamentoNome;
    private Integer quantidadeSolicitada;
    private Integer quantidadeRecebida;
    private BigDecimal precoUnitario;

    public int quantidadePendente() {
        int solicitada = quantidadeSolicitada != null ? quantidadeSolicitada : 0;
        int recebida = quantidadeRecebida != null ? quantidadeRecebida : 0;
        return Math.max(0, solicitada - recebida);
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao item do pedido");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void vincularPedido(UUID pedidoId) {
        this.pedidoCompraId = pedidoId;
    }

    public void inicializarQuantidadeRecebida() {
        if (quantidadeRecebida == null) {
            quantidadeRecebida = 0;
        }
    }

    public void registrarRecebimento(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade recebida deve ser positiva");
        }
        int recebidaAtual = quantidadeRecebida != null ? quantidadeRecebida : 0;
        quantidadeRecebida = recebidaAtual + quantidade;
    }
}
