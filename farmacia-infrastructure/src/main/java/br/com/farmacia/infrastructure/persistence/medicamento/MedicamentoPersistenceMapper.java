package br.com.farmacia.infrastructure.persistence.medicamento;

import br.com.farmacia.domain.medicamento.entity.Categoria;
import br.com.farmacia.domain.medicamento.entity.Fabricante;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.entity.MedicamentoControlado;

/**
 * Mapeamento manual entre a entidade de domínio {@link Medicamento} e a
 * entidade de persistência {@link MedicamentoJpaEntity}.
 *
 * <p>Optou-se por mapeamento explícito (em vez de ModelMapper/MapStruct) na
 * fronteira de persistência para deixar claro o que é persistido e manter o
 * domínio sem dependências de framework. As associações {@code fabricante} e
 * {@code categoria} são persistidas apenas pelo {@code id} (FK); os agregados
 * {@code principiosAtivos} e {@code medicamentoControlado} ainda não são
 * tratados nesta fatia de referência.</p>
 *
 * @author Alex Silva e Claude
 */
public final class MedicamentoPersistenceMapper {

    private MedicamentoPersistenceMapper() {
    }

    public static MedicamentoJpaEntity toJpa(Medicamento m) {
        return MedicamentoJpaEntity.builder()
            .id(m.getId())
            .codigoEan(m.getCodigoEan())
            .codigoAnvisa(m.getCodigoAnvisa())
            .nomeComercial(m.getNomeComercial())
            .nomeGenerico(m.getNomeGenerico())
            .tipo(m.getTipo())
            .formaFarmaceutica(m.getFormaFarmaceutica())
            .concentracao(m.getConcentracao())
            .apresentacao(m.getApresentacao())
            .classeTerapeutica(m.getClasseTerapeutica())
            .requerReceita(m.getRequerReceita() != null ? m.getRequerReceita() : Boolean.FALSE)
            .nivelControle(m.getNivelControle())
            .precoMaximoConsumidor(m.getPrecoMaximoConsumidor())
            .fabricanteId(m.getFabricante() != null ? m.getFabricante().getId() : null)
            .categoriaId(m.getCategoria() != null ? m.getCategoria().getId() : null)
            .ativo(m.getAtivo() != null ? m.getAtivo() : Boolean.TRUE)
            .build();
    }

    public static Medicamento toDomain(MedicamentoJpaEntity e) {
        Fabricante fabricante = e.getFabricanteId() != null
            ? Fabricante.builder().id(e.getFabricanteId()).build() : null;
        Categoria categoria = e.getCategoriaId() != null
            ? Categoria.builder().id(e.getCategoriaId()).build() : null;
        return toDomain(e, fabricante, categoria);
    }

    /**
     * Variante que recebe as referências já hidratadas de {@code fabricante} e
     * {@code categoria} (carregadas pelo adapter), permitindo expor dados como
     * razão social e nome da categoria no {@code MedicamentoModel}.
     */
    public static Medicamento toDomain(MedicamentoJpaEntity e,
                                       Fabricante fabricante,
                                       Categoria categoria) {
        return toDomain(e, fabricante, categoria, null);
    }

    public static Medicamento toDomain(MedicamentoJpaEntity e,
                                       Fabricante fabricante,
                                       Categoria categoria,
                                       MedicamentoControlado controlado) {
        Medicamento med = Medicamento.builder()
            .id(e.getId())
            .codigoEan(e.getCodigoEan())
            .codigoAnvisa(e.getCodigoAnvisa())
            .nomeComercial(e.getNomeComercial())
            .nomeGenerico(e.getNomeGenerico())
            .tipo(e.getTipo())
            .formaFarmaceutica(e.getFormaFarmaceutica())
            .concentracao(e.getConcentracao())
            .apresentacao(e.getApresentacao())
            .classeTerapeutica(e.getClasseTerapeutica())
            .requerReceita(e.getRequerReceita())
            .nivelControle(e.getNivelControle())
            .precoMaximoConsumidor(e.getPrecoMaximoConsumidor())
            .fabricante(fabricante)
            .categoria(categoria)
            .ativo(e.getAtivo())
            .build();
        if (controlado != null) {
            med.associarMedicamentoControlado(controlado);
        }
        return med;
    }

    public static Fabricante toDomain(FabricanteJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Fabricante.builder()
            .id(e.getId())
            .razaoSocial(e.getRazaoSocial())
            .nomeFantasia(e.getNomeFantasia())
            .cnpj(e.getCnpj())
            .autorizacaoAnvisa(e.getAutorizacaoAnvisa())
            .email(e.getEmail())
            .telefone(e.getTelefone())
            .ativo(e.getAtivo())
            .build();
    }

    public static Categoria toDomain(CategoriaJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Categoria.builder()
            .id(e.getId())
            .nome(e.getNome())
            .descricao(e.getDescricao())
            .ativo(e.getAtivo())
            .build();
    }
}
