package br.com.farmacia.infrastructure.persistence.compra;

import br.com.farmacia.domain.compra.entity.ItemPedidoCompra;
import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoJpaRepository;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoPersistenceMapper;

import java.util.List;
import java.util.UUID;

public final class PedidoCompraPersistenceMapper {

    private PedidoCompraPersistenceMapper() {
    }

    public static PedidoCompraJpaEntity toJpa(PedidoCompra pedido) {
        PedidoCompraJpaEntity entity = PedidoCompraJpaEntity.builder()
            .id(pedido.getId())
            .fornecedorId(pedido.getFornecedorId())
            .dataPedido(pedido.getDataPedido())
            .dataEntregaPrevista(pedido.getDataEntregaPrevista())
            .status(pedido.getStatus())
            .valorTotal(pedido.getValorTotal())
            .observacao(pedido.getObservacao())
            .build();

        if (pedido.getItens() != null) {
            for (ItemPedidoCompra item : pedido.getItens()) {
                ItemPedidoCompraJpaEntity itemEntity = ItemPedidoCompraJpaEntity.builder()
                    .id(item.getId() != null ? item.getId() : UUID.randomUUID())
                    .pedidoCompra(entity)
                    .medicamentoId(item.getMedicamentoId())
                    .quantidadeSolicitada(item.getQuantidadeSolicitada())
                    .quantidadeRecebida(item.getQuantidadeRecebida() != null ? item.getQuantidadeRecebida() : 0)
                    .precoUnitario(item.getPrecoUnitario())
                    .build();
                entity.getItens().add(itemEntity);
            }
        }
        return entity;
    }

    public static PedidoCompra toDomain(
            PedidoCompraJpaEntity e,
            String fornecedorNome,
            MedicamentoJpaRepository medicamentoRepository) {
        List<ItemPedidoCompra> itens = e.getItens().stream()
            .map(item -> toItemDomain(item, medicamentoRepository))
            .toList();

        return PedidoCompra.builder()
            .id(e.getId())
            .fornecedorId(e.getFornecedorId())
            .fornecedorNome(fornecedorNome)
            .dataPedido(e.getDataPedido())
            .dataEntregaPrevista(e.getDataEntregaPrevista())
            .status(e.getStatus())
            .valorTotal(e.getValorTotal())
            .observacao(e.getObservacao())
            .createdAt(e.getCreatedAt())
            .itens(itens)
            .build();
    }

    private static ItemPedidoCompra toItemDomain(
            ItemPedidoCompraJpaEntity item,
            MedicamentoJpaRepository medicamentoRepository) {
        String nome = medicamentoRepository.findById(item.getMedicamentoId())
            .map(m -> MedicamentoPersistenceMapper.toDomain(m).getNomeComercial())
            .orElse(null);

        return ItemPedidoCompra.builder()
            .id(item.getId())
            .pedidoCompraId(item.getPedidoCompra().getId())
            .medicamentoId(item.getMedicamentoId())
            .medicamentoNome(nome)
            .quantidadeSolicitada(item.getQuantidadeSolicitada())
            .quantidadeRecebida(item.getQuantidadeRecebida())
            .precoUnitario(item.getPrecoUnitario())
            .build();
    }
}
