package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.enums.StatusPedido;
import br.com.farmacia.domain.compra.exception.PedidoNaoEncontradoException;
import br.com.farmacia.domain.compra.exception.StatusInvalidoException;
import br.com.farmacia.domain.compra.repository.PedidoCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmarPedidoCompraUseCase {

    private final PedidoCompraRepository pedidoRepository;

    @Transactional
    public PedidoCompra executar(UUID pedidoId) {
        PedidoCompra pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (pedido.getStatus() != StatusPedido.RASCUNHO) {
            throw new StatusInvalidoException("Apenas pedidos em rascunho podem ser confirmados");
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new StatusInvalidoException("Pedido sem itens não pode ser confirmado");
        }

        pedido.confirmar();
        return pedidoRepository.save(pedido);
    }
}
