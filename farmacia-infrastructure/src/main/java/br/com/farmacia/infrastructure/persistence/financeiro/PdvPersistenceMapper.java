package br.com.farmacia.infrastructure.persistence.financeiro;

import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.enums.StatusPDV;

import java.time.LocalDateTime;

/**
 * Mapeamento manual entre {@link PDV} (domínio) e {@link PdvJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class PdvPersistenceMapper {

    private PdvPersistenceMapper() {
    }

    public static PdvJpaEntity toJpa(PDV p) {
        return PdvJpaEntity.builder()
            .id(p.getId())
            .numero(p.getNumero())
            .descricao(p.getDescricao())
            .status(p.getStatus() != null ? p.getStatus() : StatusPDV.FECHADO)
            .createdAt(p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.now())
            .build();
    }

    public static PDV toDomain(PdvJpaEntity e) {
        return PDV.builder()
            .id(e.getId())
            .numero(e.getNumero())
            .descricao(e.getDescricao())
            .status(e.getStatus())
            .createdAt(e.getCreatedAt())
            .build();
    }
}
