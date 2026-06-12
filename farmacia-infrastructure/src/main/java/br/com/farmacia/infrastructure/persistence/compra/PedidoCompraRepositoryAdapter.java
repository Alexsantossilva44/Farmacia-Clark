package br.com.farmacia.infrastructure.persistence.compra;

import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.enums.StatusPedido;
import br.com.farmacia.domain.compra.repository.PedidoCompraRepository;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PedidoCompraRepositoryAdapter implements PedidoCompraRepository {

    private final PedidoCompraJpaRepository jpaRepository;
    private final FornecedorJpaRepository fornecedorJpaRepository;
    private final MedicamentoJpaRepository medicamentoJpaRepository;

    @Override
    @Transactional
    public PedidoCompra save(PedidoCompra pedido) {
        if (pedido.getId() == null) {
            pedido.atribuirId(UUID.randomUUID());
        }
        for (var item : pedido.getItens()) {
            if (item.getId() == null) {
                item.atribuirId(UUID.randomUUID());
            }
            item.vincularPedido(pedido.getId());
            if (item.getQuantidadeRecebida() == null) {
                item.inicializarQuantidadeRecebida();
            }
        }
        PedidoCompraJpaEntity salvo = jpaRepository.save(PedidoCompraPersistenceMapper.toJpa(pedido));
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PedidoCompra> findById(UUID id) {
        return jpaRepository.findByIdWithItens(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoCompra> findAllOrderByDataPedidoDesc() {
        return jpaRepository.findAllWithItensOrderByDataPedidoDesc().stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoCompra> findByFornecedorIdAndStatusIn(UUID fornecedorId, List<StatusPedido> statuses) {
        return jpaRepository.findByFornecedorAndStatusIn(fornecedorId, statuses).stream()
            .map(this::toDomainSummary)
            .toList();
    }

    private PedidoCompra toDomain(PedidoCompraJpaEntity e) {
        String fornecedorNome = fornecedorJpaRepository.findById(e.getFornecedorId())
            .map(FornecedorJpaEntity::getRazaoSocial)
            .orElse(null);
        return PedidoCompraPersistenceMapper.toDomain(e, fornecedorNome, medicamentoJpaRepository);
    }

    private PedidoCompra toDomainSummary(PedidoCompraJpaEntity e) {
        String fornecedorNome = fornecedorJpaRepository.findById(e.getFornecedorId())
            .map(FornecedorJpaEntity::getRazaoSocial)
            .orElse(null);
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
            .build();
    }
}
