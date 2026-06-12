package br.com.farmacia.infrastructure.persistence.venda;

import br.com.farmacia.domain.venda.entity.ItemVenda;
import br.com.farmacia.domain.venda.entity.Pagamento;
import br.com.farmacia.domain.venda.entity.Venda;
import br.com.farmacia.domain.venda.enums.StatusVenda;
import br.com.farmacia.domain.venda.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link VendaRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class VendaRepositoryAdapter implements VendaRepository {

    private final VendaJpaRepository jpaRepository;

    @Override
    @Transactional
    public Venda save(Venda venda) {
        garantirIds(venda);
        jpaRepository.save(VendaPersistenceMapper.toJpa(venda));
        // Retorna o agregado de domínio já hidratado (ids garantidos), preservando
        // as associações completas necessárias ao fluxo pós-venda (ex.: SNGPC).
        return venda;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Venda> findById(UUID id) {
        return jpaRepository.findById(id).map(VendaPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venda> findWithFilters(LocalDate dataInicio, LocalDate dataFim,
                                       UUID clienteId, UUID pdvId, String status) {
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        StatusVenda statusVenda = (status != null && !status.isBlank())
            ? StatusVenda.valueOf(status.trim().toUpperCase()) : null;

        return jpaRepository.findWithFilters(inicio, fim, clienteId, pdvId, statusVenda).stream()
            .map(VendaPersistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venda> findWithFilters(LocalDate dataInicio, LocalDate dataFim,
                                       UUID clienteId, UUID pdvId, String status,
                                       int offset, int limit) {
        // H-05: paginação no banco via Pageable — substitui o subList em memória
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        StatusVenda statusVenda = (status != null && !status.isBlank())
            ? StatusVenda.valueOf(status.trim().toUpperCase()) : null;
        PageRequest pageable = PageRequest.of(offset / limit, limit);
        return jpaRepository.findWithFiltersPaginated(inicio, fim, clienteId, pdvId, statusVenda, pageable).stream()
            .map(VendaPersistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithFilters(LocalDate dataInicio, LocalDate dataFim,
                                  UUID clienteId, UUID pdvId, String status) {
        // H-05: contagem no banco para o totalElements do Page
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        StatusVenda statusVenda = (status != null && !status.isBlank())
            ? StatusVenda.valueOf(status.trim().toUpperCase()) : null;
        return jpaRepository.countWithFilters(inicio, fim, clienteId, pdvId, statusVenda);
    }

    private void garantirIds(Venda venda) {
        if (venda.getId() == null) {
            venda.atribuirId(UUID.randomUUID());
        }
        if (venda.getItens() != null) {
            for (ItemVenda item : venda.getItens()) {
                if (item.getId() == null) {
                    item.atribuirId(UUID.randomUUID());
                }
            }
        }
        if (venda.getPagamentos() != null) {
            for (Pagamento pagamento : venda.getPagamentos()) {
                if (pagamento.getId() == null) {
                    pagamento.atribuirId(UUID.randomUUID());
                }
            }
        }
    }
}
