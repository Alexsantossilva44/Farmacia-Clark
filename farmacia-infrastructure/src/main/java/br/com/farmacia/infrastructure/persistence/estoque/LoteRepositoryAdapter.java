package br.com.farmacia.infrastructure.persistence.estoque;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoJpaRepository;
import br.com.farmacia.infrastructure.persistence.medicamento.MedicamentoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link LoteRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class LoteRepositoryAdapter implements LoteRepository {

    private final LoteJpaRepository jpaRepository;
    private final MedicamentoJpaRepository medicamentoJpaRepository;

    @Override
    @Transactional
    public Lote save(Lote lote) {
        if (lote.getId() == null) {
            lote.atribuirId(UUID.randomUUID());
        }
        LoteJpaEntity salvo = jpaRepository.save(LotePersistenceMapper.toJpa(lote));
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lote> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lote> findByMedicamentoIdAndNumeroLote(UUID medicamentoId, String numeroLote) {
        return jpaRepository.findByMedicamentoIdAndNumeroLote(medicamentoId, numeroLote)
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lote> findByNumeroLote(String numeroLote) {
        return jpaRepository.findFirstByNumeroLote(numeroLote).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> findLotesDisponivelFefo(UUID medicamentoId) {
        return jpaRepository.findLotesDisponivelFefo(medicamentoId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> findByMedicamentoIdOrderByDataValidadeAsc(UUID medicamentoId) {
        return jpaRepository.findByMedicamentoIdOrderByDataValidadeAsc(medicamentoId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> findByStatusAndDataValidadeBefore(StatusLote status, LocalDate data) {
        return jpaRepository.findByStatusAndDataValidadeBefore(status, data).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> findLotesProximosVencer(StatusLote status, LocalDate dataLimite) {
        return jpaRepository.findLotesProximosVencer(status, dataLimite).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Integer> somarDisponivelVendaPorMedicamento() {
        Map<UUID, Integer> mapa = new HashMap<>();
        for (Object[] row : jpaRepository.sumDisponivelVendaGroupByMedicamento()) {
            mapa.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return mapa;
    }

    @Override
    @Transactional(readOnly = true)
    public Lote getReferenceById(UUID id) {
        return Lote.builder().id(id).build();
    }

    private Lote toDomain(LoteJpaEntity e) {
        Medicamento medicamento = e.getMedicamentoId() == null ? null
            : medicamentoJpaRepository.findById(e.getMedicamentoId())
                .map(MedicamentoPersistenceMapper::toDomain)
                .orElse(Medicamento.builder().id(e.getMedicamentoId()).build());
        return LotePersistenceMapper.toDomain(e, medicamento);
    }
}
