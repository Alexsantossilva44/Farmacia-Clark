package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.repository.PedidoCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarPedidosCompraUseCase {

    private final PedidoCompraRepository pedidoRepository;

    @Transactional(readOnly = true)
    public List<PedidoCompra> executar() {
        return pedidoRepository.findAllOrderByDataPedidoDesc();
    }
}
