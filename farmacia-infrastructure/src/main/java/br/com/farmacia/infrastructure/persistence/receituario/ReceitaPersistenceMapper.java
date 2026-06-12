package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.receituario.entity.Prescritor;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.enums.StatusReceita;

/**
 * Mapeamento manual entre {@link Receita} (domínio) e {@link ReceitaJpaEntity}.
 *
 * <p>Prescritor, cliente e farmacêutico são referenciados por id (FK). O
 * prescritor é hidratado pelo adapter pois é lido durante a validação.</p>
 *
 * @author Alex Silva e Claude
 */
public final class ReceitaPersistenceMapper {

    private ReceitaPersistenceMapper() {
    }

    public static ReceitaJpaEntity toJpa(Receita r) {
        return ReceitaJpaEntity.builder()
            .id(r.getId())
            .numeroReceita(r.getNumeroReceita())
            .dataEmissao(r.getDataEmissao())
            .dataValidade(r.getDataValidade())
            .tipo(r.getTipo())
            .status(r.getStatus() != null ? r.getStatus() : StatusReceita.PENDENTE)
            .cid(r.getCid())
            .retida(r.getRetida() != null ? r.getRetida() : Boolean.FALSE)
            .imagemPath(r.getImagemPath())
            .motivoRejeicao(r.getMotivoRejeicao())
            .prescritorId(r.getPrescritor() != null ? r.getPrescritor().getId() : null)
            .clienteId(r.getCliente() != null ? r.getCliente().getId() : null)
            .farmaceuticoId(resolverFarmaceuticoId(r))
            .dataValidacao(r.getDataValidacao())
            .build();
    }

    public static Receita toDomain(ReceitaJpaEntity e, Prescritor prescritor) {
        return Receita.builder()
            .id(e.getId())
            .numeroReceita(e.getNumeroReceita())
            .dataEmissao(e.getDataEmissao())
            .dataValidade(e.getDataValidade())
            .tipo(e.getTipo())
            .status(e.getStatus())
            .cid(e.getCid())
            .retida(e.getRetida())
            .imagemPath(e.getImagemPath())
            .motivoRejeicao(e.getMotivoRejeicao())
            .prescritor(prescritor)
            .cliente(e.getClienteId() != null
                ? Cliente.builder().id(e.getClienteId()).build() : null)
            .farmaceuticoId(e.getFarmaceuticoId())
            .farmaceutico(e.getFarmaceuticoId() != null
                ? Farmaceutico.builder().id(e.getFarmaceuticoId()).build() : null)
            .dataValidacao(e.getDataValidacao())
            .build();
    }

    private static java.util.UUID resolverFarmaceuticoId(Receita r) {
        if (r.getFarmaceutico() != null && r.getFarmaceutico().getId() != null) {
            return r.getFarmaceutico().getId();
        }
        return r.getFarmaceuticoId();
    }
}
