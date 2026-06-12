package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.AlertaEstoque;
import br.com.farmacia.domain.estoque.enums.StatusAlerta;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import br.com.farmacia.domain.estoque.repository.AlertaEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link AlertaEstoqueRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class AlertaEstoqueRepositoryAdapter implements AlertaEstoqueRepository {

    private final AlertaEstoqueJpaRepository jpaRepository;

    @Override
    @Transactional
    public AlertaEstoque save(AlertaEstoque alerta) {
        if (alerta.getId() == null) {
            alerta.atribuirId(UUID.randomUUID());
        }
        AlertaEstoqueJpaEntity salvo =
            jpaRepository.save(AlertaEstoquePersistenceMapper.toJpa(alerta));
        return AlertaEstoquePersistenceMapper.toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeAlertaAberto(UUID loteId, TipoAlerta tipo) {
        return jpaRepository.existsByLoteIdAndTipoAndStatus(loteId, tipo, StatusAlerta.ABERTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeAlertaAbertoPorMedicamento(UUID medicamentoId, TipoAlerta tipo) {
        return jpaRepository.existsByMedicamentoIdAndTipoAndStatus(
            medicamentoId, tipo, StatusAlerta.ABERTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaEstoque> findByLoteIdAndTipo(UUID loteId, TipoAlerta tipo) {
        return jpaRepository.findByLoteIdAndTipo(loteId, tipo).stream()
            .map(AlertaEstoquePersistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaEstoque> findByMedicamentoId(UUID medicamentoId) {
        return jpaRepository.findByMedicamentoId(medicamentoId).stream()
            .map(AlertaEstoquePersistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaEstoque> findByMedicamentoIdAndTipo(UUID medicamentoId, TipoAlerta tipo) {
        return jpaRepository.findByMedicamentoIdAndTipo(medicamentoId, tipo).stream()
            .map(AlertaEstoquePersistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaEstoque> findByStatus(StatusAlerta status) {
        return jpaRepository.findByStatus(status).stream()
            .map(AlertaEstoquePersistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByLoteIdAndTipo(UUID loteId, TipoAlerta tipo) {
        return jpaRepository.countByLoteIdAndTipo(loteId, tipo);
    }

    @Override
    @Transactional
    public void deleteAllByLoteId(UUID loteId) {
        jpaRepository.deleteByLoteId(loteId);
    }

    @Override
    @Transactional
    public void deleteAllByMedicamentoNomeAndTipo(String nome, TipoAlerta tipo) {
        jpaRepository.deleteByMedicamentoNomeAndTipo(nome, tipo);
    }

    @Override
    @Transactional
    public void deleteAllByMedicamentoId(UUID medicamentoId) {
        jpaRepository.deleteByMedicamentoId(medicamentoId);
    }
}
