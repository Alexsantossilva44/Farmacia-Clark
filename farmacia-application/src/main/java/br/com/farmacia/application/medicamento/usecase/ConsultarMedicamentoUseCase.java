package br.com.farmacia.application.medicamento.usecase;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Caso de Uso: Consultar Medicamentos.
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ConsultarMedicamentoUseCase {

    private final MedicamentoRepository medicamentoRepository;

    @Transactional(readOnly = true)
    public Medicamento buscarOuFalhar(UUID id) {
        return medicamentoRepository.findById(id)
            .orElseThrow(() -> new MedicamentoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public Page<Medicamento> listarPaginado(Pageable pageable, String busca) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        String filtro = busca != null && !busca.isBlank() ? busca.trim() : null;
        var content = medicamentoRepository.findAllOrdenados(offset, limit, filtro);
        long total = medicamentoRepository.contar(filtro);
        return new PageImpl<>(content, pageable, total);
    }
}
