package br.com.farmacia.domain.compra.repository;

import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.enums.StatusPedido;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoCompraRepository {
    PedidoCompra save(PedidoCompra pedido);
    Optional<PedidoCompra> findById(UUID id);
    List<PedidoCompra> findAllOrderByDataPedidoDesc();
    List<PedidoCompra> findByFornecedorIdAndStatusIn(UUID fornecedorId, List<StatusPedido> statuses);
}
