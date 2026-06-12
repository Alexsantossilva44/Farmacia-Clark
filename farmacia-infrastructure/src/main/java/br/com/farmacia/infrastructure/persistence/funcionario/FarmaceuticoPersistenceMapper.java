package br.com.farmacia.infrastructure.persistence.funcionario;

import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.funcionario.entity.Funcionario;

/**
 * Mapeamento manual entre {@link Farmaceutico} (domínio) e
 * {@link FarmaceuticoJpaEntity}.
 *
 * <p>O atributo {@code ativo} do domínio não possui coluna correspondente na
 * tabela {@code farmaceuticos}; assume-se ativo ao reidratar.</p>
 *
 * @author Alex Silva e Claude
 */
public final class FarmaceuticoPersistenceMapper {

    private FarmaceuticoPersistenceMapper() {
    }

    public static FarmaceuticoJpaEntity toJpa(Farmaceutico f) {
        return FarmaceuticoJpaEntity.builder()
            .id(f.getId())
            .funcionarioId(f.getFuncionario() != null ? f.getFuncionario().getId() : null)
            .crf(f.getCrf())
            .ufCrf(f.getUfCrf())
            .especialidades(f.getEspecialidades())
            .responsavelTecnico(f.getResponsavelTecnico() != null
                ? f.getResponsavelTecnico() : Boolean.FALSE)
            .build();
    }

    public static Farmaceutico toDomain(FarmaceuticoJpaEntity e) {
        return Farmaceutico.builder()
            .id(e.getId())
            .funcionario(e.getFuncionarioId() != null
                ? Funcionario.builder().id(e.getFuncionarioId()).build() : null)
            .crf(e.getCrf())
            .ufCrf(e.getUfCrf())
            .especialidades(e.getEspecialidades())
            .responsavelTecnico(e.getResponsavelTecnico())
            .ativo(Boolean.TRUE)
            .build();
    }
}
