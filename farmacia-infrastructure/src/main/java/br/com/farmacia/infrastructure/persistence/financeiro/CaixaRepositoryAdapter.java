package br.com.farmacia.infrastructure.persistence.financeiro;

import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.enums.StatusCaixa;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link CaixaRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class CaixaRepositoryAdapter implements CaixaRepository {

    private final CaixaJpaRepository jpaRepository;
    private final PdvJpaRepository pdvJpaRepository;

    @Override
    @Transactional
    public Caixa save(Caixa caixa) {
        if (caixa.getId() == null) {
            caixa.atribuirId(UUID.randomUUID());
        }
        CaixaJpaEntity salvo = jpaRepository.save(CaixaPersistenceMapper.toJpa(caixa));
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Caixa> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Caixa> findCaixaAbertoPorPdv(UUID pdvId) {
        return jpaRepository.findFirstByPdvIdAndStatus(pdvId, StatusCaixa.ABERTO)
            .map(this::toDomain);
    }

    private Caixa toDomain(CaixaJpaEntity e) {
        PDV pdv = e.getPdvId() == null ? null
            : pdvJpaRepository.findById(e.getPdvId())
                .map(PdvPersistenceMapper::toDomain)
                .orElse(PDV.builder().id(e.getPdvId()).build());
        return CaixaPersistenceMapper.toDomain(e, pdv);
    }
}
