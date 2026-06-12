package br.com.farmacia.domain.compra.entity;

import br.com.farmacia.domain.compra.enums.StatusPedido;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoCompra {

    private UUID id;
    private UUID fornecedorId;
    private String fornecedorNome;
    private LocalDate dataPedido;
    private LocalDate dataEntregaPrevista;
    private StatusPedido status;
    private BigDecimal valorTotal;
    private String observacao;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<ItemPedidoCompra> itens = new ArrayList<>();

    public int quantidadePendenteTotal() {
        return itens.stream()
            .mapToInt(i -> Math.max(0, i.getQuantidadeSolicitada() - i.getQuantidadeRecebida()))
            .sum();
    }

    public boolean totalmenteRecebido() {
        return !itens.isEmpty() && itens.stream()
            .allMatch(i -> i.getQuantidadeRecebida() >= i.getQuantidadeSolicitada());
    }

    public boolean parcialmenteRecebido() {
        return itens.stream().anyMatch(i -> i.getQuantidadeRecebida() > 0);
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao pedido");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void confirmar() {
        if (status != StatusPedido.RASCUNHO) {
            throw new IllegalStateException("Pedido não pode ser confirmado no status: " + status);
        }
        this.status = StatusPedido.CONFIRMADO;
    }

    /**
     * Atualiza status conforme progresso de recebimento dos itens.
     */
    public void atualizarStatusRecebimento() {
        if (totalmenteRecebido()) {
            status = StatusPedido.RECEBIDO;
        } else if (parcialmenteRecebido()) {
            status = StatusPedido.PARCIALMENTE_RECEBIDO;
        }
    }
}
