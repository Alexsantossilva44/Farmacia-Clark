package br.com.farmacia.infrastructure.persistence.medicamento;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.entity.MedicamentoControlado;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapeamento manual entre {@link MedicamentoControlado} (domínio) e JPA.
 */
public final class MedicamentoControladoPersistenceMapper {

    private MedicamentoControladoPersistenceMapper() {
    }

    public static MedicamentoControladoJpaEntity toJpa(MedicamentoControlado ctrl, UUID medicamentoId) {
        return MedicamentoControladoJpaEntity.builder()
            .id(ctrl.getId())
            .medicamentoId(medicamentoId)
            .portaria(ctrl.getPortaria())
            .lista(ctrl.getLista())
            .quantidadeMaximaReceita(ctrl.getQuantidadeMaximaReceita())
            .validadeReceitaDias(ctrl.getValidadeReceitaDias())
            .psicootropico(ctrl.getPsicootropico() != null ? ctrl.getPsicootropico() : Boolean.FALSE)
            .entorpecente(ctrl.getEntorpecente() != null ? ctrl.getEntorpecente() : Boolean.FALSE)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static MedicamentoControlado toDomain(MedicamentoControladoJpaEntity e, Medicamento medicamento) {
        return MedicamentoControlado.builder()
            .id(e.getId())
            .medicamento(medicamento)
            .portaria(e.getPortaria())
            .lista(e.getLista())
            .quantidadeMaximaReceita(e.getQuantidadeMaximaReceita())
            .validadeReceitaDias(e.getValidadeReceitaDias())
            .psicootropico(e.getPsicootropico())
            .entorpecente(e.getEntorpecente())
            .build();
    }
}
