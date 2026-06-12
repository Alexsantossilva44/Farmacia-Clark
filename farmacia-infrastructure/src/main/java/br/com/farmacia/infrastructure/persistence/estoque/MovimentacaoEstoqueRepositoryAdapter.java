package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import br.com.farmacia.domain.estoque.repository.MovimentacaoEstoqueRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoJpaRepository;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MovimentacaoEstoqueRepositoryAdapter implements MovimentacaoEstoqueRepository {

    private final MovimentacaoEstoqueJpaRepository jpaRepository;
    private final MedicamentoJpaRepository medicamentoJpaRepository;
    private final LoteJpaRepository loteJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> buscar(
            UUID medicamentoId,
            TipoMovimentacao tipo,
            int offset,
            int limit) {
        int page = limit > 0 ? offset / limit : 0;
        return jpaRepository.buscarPorFiltro(
                medicamentoId,
                tipo,
                PageRequest.of(page, Math.max(limit, 1)))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contar(UUID medicamentoId, TipoMovimentacao tipo) {
        return jpaRepository.buscarPorFiltro(
                medicamentoId, tipo, PageRequest.of(0, 1)).getTotalElements();
    }

    private MovimentacaoEstoque toDomain(MovimentacaoEstoqueJpaEntity e) {
        Medicamento medicamento = medicamentoJpaRepository.findById(e.getMedicamentoId())
            .map(MedicamentoPersistenceMapper::toDomain)
            .orElse(Medicamento.builder().id(e.getMedicamentoId()).build());

        Lote lote = loteJpaRepository.findById(e.getLoteId())
            .map(le -> LotePersistenceMapper.toDomain(le, medicamento))
            .orElse(Lote.builder().id(e.getLoteId()).medicamento(medicamento).build());

        return MovimentacaoEstoquePersistenceMapper.toDomain(e, medicamento, lote);
    }
}
