package br.com.farmacia.application.medicamento.usecase;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de Uso: Atualizar Medicamento.
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class AtualizarMedicamentoUseCase {

    private final MedicamentoRepository medicamentoRepository;

    @Transactional
    public Medicamento executar(Medicamento medicamento) {
        return medicamentoRepository.save(medicamento);
    }
}
