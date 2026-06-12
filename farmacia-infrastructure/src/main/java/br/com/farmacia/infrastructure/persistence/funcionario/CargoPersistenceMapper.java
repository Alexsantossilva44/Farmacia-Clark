package br.com.farmacia.infrastructure.persistence.funcionario;

import br.com.farmacia.domain.funcionario.entity.Cargo;

/**
 * Mapeamento manual entre {@link Cargo} (domínio) e {@link CargoJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class CargoPersistenceMapper {

    private CargoPersistenceMapper() {
    }

    public static CargoJpaEntity toJpa(Cargo c) {
        return CargoJpaEntity.builder()
            .id(c.getId())
            .nome(c.getNome())
            .descricao(c.getDescricao())
            .roleSistema(c.getRoleSistema())
            .build();
    }

    public static Cargo toDomain(CargoJpaEntity e) {
        return Cargo.builder()
            .id(e.getId())
            .nome(e.getNome())
            .descricao(e.getDescricao())
            .roleSistema(e.getRoleSistema())
            .build();
    }
}
