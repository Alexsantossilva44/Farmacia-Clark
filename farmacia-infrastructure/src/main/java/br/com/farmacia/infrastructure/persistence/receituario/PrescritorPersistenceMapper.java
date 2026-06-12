package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.receituario.entity.Prescritor;

import java.time.LocalDateTime;

/**
 * Mapeamento manual entre {@link Prescritor} (domínio) e
 * {@link PrescritorJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class PrescritorPersistenceMapper {

    private PrescritorPersistenceMapper() {
    }

    public static PrescritorJpaEntity toJpa(Prescritor p) {
        return PrescritorJpaEntity.builder()
            .id(p.getId())
            .nome(p.getNome())
            .crm(p.getCrm())
            .ufCrm(p.getUfCrm())
            .especialidade(p.getEspecialidade())
            .email(p.getEmail())
            .ativo(p.getAtivo() != null ? p.getAtivo() : Boolean.TRUE)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static Prescritor toDomain(PrescritorJpaEntity e) {
        return Prescritor.builder()
            .id(e.getId())
            .nome(e.getNome())
            .crm(e.getCrm())
            .ufCrm(e.getUfCrm())
            .especialidade(e.getEspecialidade())
            .email(e.getEmail())
            .ativo(e.getAtivo())
            .build();
    }
}
