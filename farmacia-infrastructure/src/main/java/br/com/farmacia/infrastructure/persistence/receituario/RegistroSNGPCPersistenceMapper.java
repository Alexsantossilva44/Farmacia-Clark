package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.entity.RegistroSNGPC;
import br.com.farmacia.domain.receituario.enums.StatusEnvio;

import java.time.LocalDateTime;

/**
 * Mapeamento manual entre {@link RegistroSNGPC} (domínio) e
 * {@link RegistroSNGPCJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class RegistroSNGPCPersistenceMapper {

    private RegistroSNGPCPersistenceMapper() {
    }

    public static RegistroSNGPCJpaEntity toJpa(RegistroSNGPC r) {
        return RegistroSNGPCJpaEntity.builder()
            .id(r.getId())
            .receitaId(r.getReceita() != null ? r.getReceita().getId() : null)
            .medicamentoId(r.getMedicamento() != null ? r.getMedicamento().getId() : null)
            .loteId(r.getLote() != null ? r.getLote().getId() : null)
            .compradorNome(r.getCompradorNome())
            .compradorCpf(r.getCompradorCpf())
            .compradorRg(r.getCompradorRg())
            .quantidade(r.getQuantidade())
            .statusEnvio(r.getStatusEnvio() != null ? r.getStatusEnvio() : StatusEnvio.PENDENTE)
            .dataRegistro(r.getDataRegistro() != null ? r.getDataRegistro() : LocalDateTime.now())
            .dataEnvio(r.getDataEnvio())
            .numeroProtocolo(r.getNumeroProtocolo())
            .retornoGoverno(r.getRetornoGoverno())
            .tentativasEnvio(r.getTentativasEnvio() != null ? r.getTentativasEnvio() : 0)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static RegistroSNGPC toDomain(RegistroSNGPCJpaEntity e) {
        return RegistroSNGPC.builder()
            .id(e.getId())
            .receita(e.getReceitaId() != null
                ? Receita.builder().id(e.getReceitaId()).build() : null)
            .medicamento(e.getMedicamentoId() != null
                ? Medicamento.builder().id(e.getMedicamentoId()).build() : null)
            .lote(e.getLoteId() != null
                ? Lote.builder().id(e.getLoteId()).build() : null)
            .compradorNome(e.getCompradorNome())
            .compradorCpf(e.getCompradorCpf())
            .compradorRg(e.getCompradorRg())
            .quantidade(e.getQuantidade())
            .statusEnvio(e.getStatusEnvio())
            .dataRegistro(e.getDataRegistro())
            .dataEnvio(e.getDataEnvio())
            .numeroProtocolo(e.getNumeroProtocolo())
            .retornoGoverno(e.getRetornoGoverno())
            .tentativasEnvio(e.getTentativasEnvio())
            .build();
    }
}
