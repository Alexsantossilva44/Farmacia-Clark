package br.com.farmacia.infrastructure.persistence.compra;

import br.com.farmacia.domain.compra.entity.NotaFiscalEntrada;
import br.com.farmacia.domain.compra.repository.NotaFiscalEntradaRepository;
import br.com.farmacia.infrastructure.persistence.estoque.LoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotaFiscalEntradaRepositoryAdapter implements NotaFiscalEntradaRepository {

    private final NotaFiscalEntradaJpaRepository jpaRepository;
    private final FornecedorJpaRepository fornecedorJpaRepository;
    private final LoteJpaRepository loteJpaRepository;

    @Override
    @Transactional
    public NotaFiscalEntrada save(NotaFiscalEntrada nota) {
        if (nota.getId() == null) {
            nota.atribuirId(UUID.randomUUID());
        }
        return toDomain(jpaRepository.save(toJpa(nota)), nota.getFornecedorNome());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotaFiscalEntrada> findById(UUID id) {
        return jpaRepository.findById(id).map(e -> {
            String nome = fornecedorJpaRepository.findById(e.getFornecedorId())
                .map(FornecedorJpaEntity::getRazaoSocial)
                .orElse(null);
            return toDomain(e, nome);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotaFiscalEntrada> findByChaveAcesso(String chaveAcesso) {
        return jpaRepository.findByChaveAcesso(chaveAcesso).map(e -> {
            String nome = fornecedorJpaRepository.findById(e.getFornecedorId())
                .map(FornecedorJpaEntity::getRazaoSocial)
                .orElse(null);
            return toDomain(e, nome);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotaFiscalEntrada> findAllOrderByDataEntradaDesc() {
        return jpaRepository.findAllByOrderByDataEntradaDescCreatedAtDesc().stream()
            .map(e -> {
                String nome = fornecedorJpaRepository.findById(e.getFornecedorId())
                    .map(FornecedorJpaEntity::getRazaoSocial)
                    .orElse(null);
                return toDomain(e, nome);
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarLotesPorNota(UUID notaFiscalId) {
        return loteJpaRepository.countByNotaFiscalId(notaFiscalId);
    }

    private NotaFiscalEntrada toDomain(NotaFiscalEntradaJpaEntity e, String fornecedorNome) {
        return NotaFiscalEntrada.builder()
            .id(e.getId())
            .pedidoCompraId(e.getPedidoCompraId())
            .fornecedorId(e.getFornecedorId())
            .fornecedorNome(fornecedorNome)
            .numeroNota(e.getNumeroNota())
            .serie(e.getSerie())
            .chaveAcesso(e.getChaveAcesso())
            .dataEmissao(e.getDataEmissao())
            .dataEntrada(e.getDataEntrada())
            .valorTotal(e.getValorTotal())
            .status(e.getStatus())
            .createdAt(e.getCreatedAt())
            .quantidadeItens((int) loteJpaRepository.countByNotaFiscalId(e.getId()))
            .build();
    }

    private NotaFiscalEntradaJpaEntity toJpa(NotaFiscalEntrada n) {
        return NotaFiscalEntradaJpaEntity.builder()
            .id(n.getId())
            .pedidoCompraId(n.getPedidoCompraId())
            .fornecedorId(n.getFornecedorId())
            .numeroNota(n.getNumeroNota())
            .serie(n.getSerie())
            .chaveAcesso(n.getChaveAcesso())
            .dataEmissao(n.getDataEmissao())
            .dataEntrada(n.getDataEntrada())
            .valorTotal(n.getValorTotal())
            .status(n.getStatus())
            .build();
    }
}
