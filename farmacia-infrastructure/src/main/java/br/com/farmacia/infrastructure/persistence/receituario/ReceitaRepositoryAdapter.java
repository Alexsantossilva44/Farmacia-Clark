package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.receituario.entity.Prescritor;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link ReceitaRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class ReceitaRepositoryAdapter implements ReceitaRepository {

    private final ReceitaJpaRepository jpaRepository;
    private final PrescritorJpaRepository prescritorJpaRepository;

    @Override
    @Transactional
    public Receita save(Receita receita) {
        if (receita.getId() == null) {
            receita.atribuirId(UUID.randomUUID());
        }
        ReceitaJpaEntity salvo = jpaRepository.save(ReceitaPersistenceMapper.toJpa(receita));
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receita> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receita> findByNumeroReceita(String numero) {
        return jpaRepository.findFirstByNumeroReceita(numero).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Receita getReferenceById(UUID id) {
        return Receita.builder().id(id).build();
    }

    private Receita toDomain(ReceitaJpaEntity e) {
        Prescritor prescritor = e.getPrescritorId() == null ? null
            : prescritorJpaRepository.findById(e.getPrescritorId())
                .map(PrescritorPersistenceMapper::toDomain)
                .orElse(Prescritor.builder().id(e.getPrescritorId()).build());
        return ReceitaPersistenceMapper.toDomain(e, prescritor);
    }
}
