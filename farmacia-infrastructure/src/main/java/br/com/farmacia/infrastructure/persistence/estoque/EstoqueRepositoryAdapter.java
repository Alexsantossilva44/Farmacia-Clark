package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import br.com.farmacia.domain.estoque.exception.EstoqueInsuficienteException;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoJpaEntity;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoJpaRepository;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter de saída que implementa a porta {@link EstoqueRepository},
 * controlando o saldo consolidado ({@code itens_estoque}) e a trilha de
 * movimentações ({@code movimentacoes_estoque}).
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class EstoqueRepositoryAdapter implements EstoqueRepository {

    private final ItemEstoqueJpaRepository itemRepository;
    private final MovimentacaoEstoqueJpaRepository movimentacaoRepository;
    private final MedicamentoJpaRepository medicamentoJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemEstoque> findByMedicamentoId(UUID medicamentoId) {
        return itemRepository.findByMedicamentoId(medicamentoId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ItemEstoque> findByMedicamentoIds(Collection<UUID> medicamentoIds) {
        if (medicamentoIds == null || medicamentoIds.isEmpty()) {
            return Map.of();
        }
        return itemRepository.findByMedicamentoIdIn(medicamentoIds).stream()
            .map(this::toDomain)
            .filter(item -> item.getMedicamento() != null && item.getMedicamento().getId() != null)
            .collect(Collectors.toMap(
                item -> item.getMedicamento().getId(),
                Function.identity(),
                (a, b) -> a
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemEstoque> findAll() {
        return itemRepository.findAll().stream()
            .map(this::toDomain)
            .sorted(java.util.Comparator.comparing(
                i -> i.getMedicamento() != null && i.getMedicamento().getNomeComercial() != null
                    ? i.getMedicamento().getNomeComercial()
                    : "",
                String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    @Transactional
    public void decrementarSaldo(UUID medicamentoId, int quantidade) {
        // H-15: orElseThrow substitui ifPresent — item ausente agora lança exceção em vez de operação silenciosa
        ItemEstoqueJpaEntity item = itemRepository.findByMedicamentoId(medicamentoId)
            .orElseThrow(() -> new br.com.farmacia.domain.estoque.exception.ItemEstoqueNaoEncontradoException(medicamentoId));
        int disponivel = item.getQuantidadeAtual() != null ? item.getQuantidadeAtual() : 0;
        int novoSaldo = disponivel - quantidade;
        if (novoSaldo < 0) {
            String nomeMedicamento = medicamentoJpaRepository.findById(medicamentoId)
                .map(MedicamentoJpaEntity::getNomeComercial)
                .orElse(medicamentoId.toString());
            throw new EstoqueInsuficienteException(nomeMedicamento, quantidade, disponivel);
        }
        item.setQuantidadeAtual(novoSaldo);
        item.setUltimaMovimentacao(LocalDateTime.now());
        itemRepository.save(item);
    }

    @Override
    @Transactional
    public void incrementarSaldo(UUID medicamentoId, int quantidade) {
        itemRepository.findByMedicamentoId(medicamentoId).ifPresent(item -> {
            item.setQuantidadeAtual(item.getQuantidadeAtual() + quantidade);
            item.setUltimaMovimentacao(LocalDateTime.now());
            itemRepository.save(item);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemEstoque> findItensAbaixoDoMinimo() {
        return itemRepository.findItensAbaixoDoMinimo().stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemEstoque> findItensComEstoqueZerado() {
        return itemRepository.findItensComEstoqueZerado().stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void salvarMovimentacao(MovimentacaoEstoque movimentacao) {
        if (movimentacao.getId() == null) {
            movimentacao.atribuirId(UUID.randomUUID());
        }
        movimentacaoRepository.save(MovimentacaoEstoquePersistenceMapper.toJpa(movimentacao));
    }

    @Override
    @Transactional
    public ItemEstoque salvar(ItemEstoque item) {
        if (item.getId() == null) {
            item.atribuirId(UUID.randomUUID());
        }
        ItemEstoqueJpaEntity salvo =
            itemRepository.save(ItemEstoquePersistenceMapper.toJpa(item));
        return toDomain(salvo);
    }

    private ItemEstoque toDomain(ItemEstoqueJpaEntity e) {
        Medicamento medicamento = e.getMedicamentoId() == null ? null
            : medicamentoJpaRepository.findById(e.getMedicamentoId())
                .map(MedicamentoPersistenceMapper::toDomain)
                .orElse(Medicamento.builder().id(e.getMedicamentoId()).build());
        return ItemEstoquePersistenceMapper.toDomain(e, medicamento);
    }
}
