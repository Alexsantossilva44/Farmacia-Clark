package br.com.farmacia.application.medicamento.usecase;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.exception.MedicamentoDuplicadoException;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de Uso: Cadastrar Medicamento.
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CadastrarMedicamentoUseCase {

    private final MedicamentoRepository medicamentoRepository;

    @Transactional
    public Medicamento executar(Medicamento medicamento) {
        if (medicamento.getCodigoEan() != null
                && medicamentoRepository.existsByCodigoEan(medicamento.getCodigoEan())) {
            throw new MedicamentoDuplicadoException(medicamento.getCodigoEan());
        }
        if (medicamentoRepository.existsByNomeComercial(medicamento.getNomeComercial())) {
            throw MedicamentoDuplicadoException.porNome(medicamento.getNomeComercial());
        }
        log.info("Cadastrando medicamento: {}", medicamento.getNomeComercial());
        return medicamentoRepository.save(medicamento);
    }
}
