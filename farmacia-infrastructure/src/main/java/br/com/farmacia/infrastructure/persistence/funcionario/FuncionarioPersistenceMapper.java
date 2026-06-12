package br.com.farmacia.infrastructure.persistence.funcionario;

import br.com.farmacia.domain.funcionario.entity.Cargo;
import br.com.farmacia.domain.funcionario.entity.Funcionario;

/**
 * Mapeamento manual entre {@link Funcionario} (domínio) e
 * {@link FuncionarioJpaEntity}.
 *
 * <p>O cargo é referenciado pelo id (FK). O adapter é responsável por
 * hidratar o {@link Cargo} completo a partir do repositório de cargos.</p>
 *
 * @author Alex Silva e Claude
 */
public final class FuncionarioPersistenceMapper {

    private FuncionarioPersistenceMapper() {
    }

    public static FuncionarioJpaEntity toJpa(Funcionario f) {
        return FuncionarioJpaEntity.builder()
            .id(f.getId())
            .nome(f.getNome())
            .cpf(f.getCpf())
            .email(f.getEmail())
            .senhaHash(f.getSenhaHash())
            .telefone(f.getTelefone())
            .cargoId(f.getCargo() != null ? f.getCargo().getId() : null)
            .dataAdmissao(f.getDataAdmissao())
            .dataDemissao(f.getDataDemissao())
            .ativo(f.getAtivo() != null ? f.getAtivo() : Boolean.TRUE)
            .build();
    }

    public static Funcionario toDomain(FuncionarioJpaEntity e, Cargo cargo) {
        return Funcionario.builder()
            .id(e.getId())
            .nome(e.getNome())
            .cpf(e.getCpf())
            .email(e.getEmail())
            .senhaHash(e.getSenhaHash())
            .telefone(e.getTelefone())
            .cargo(cargo)
            .dataAdmissao(e.getDataAdmissao())
            .dataDemissao(e.getDataDemissao())
            .ativo(e.getAtivo())
            .build();
    }
}
