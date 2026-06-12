package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.exception.PedidoNaoEncontradoException;
import br.com.farmacia.domain.compra.repository.PedidoCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultarPedidoCompraUseCase {

    private final PedidoCompraRepository pedidoRepository;

    @Transactional(readOnly = true)
    public PedidoCompra executar(UUID id) {
        return pedidoRepository.findById(id)
            .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }
}
