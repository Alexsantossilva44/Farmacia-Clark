package br.com.farmacia.application.medicamento.usecase;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Caso de Uso: Excluir (inativar) Medicamento.
 * Segue heurística AlgaWorks: exclusão lógica, não física.
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ExcluirMedicamentoUseCase {

    private final MedicamentoRepository    medicamentoRepository;
    private final ConsultarMedicamentoUseCase consultarUseCase;

    @Transactional
    public void executar(UUID id) {
        Medicamento medicamento = consultarUseCase.buscarOuFalhar(id);
        medicamento.inativar();
        medicamentoRepository.save(medicamento);
    }
}
