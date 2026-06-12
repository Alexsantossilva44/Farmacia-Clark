package br.com.farmacia.application.estoque.usecase;

import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lista medicamentos ativos com saldo consolidado, paginado no banco.
 * Medicamentos sem registro em estoque aparecem com quantidade zero.
 */
@Service
@RequiredArgsConstructor
public class ListarEstoqueUseCase {

    private static final int QTD_MIN_PADRAO = 5;
    private static final int QTD_MAX_PADRAO = 500;

    private final EstoqueRepository estoqueRepository;
    private final MedicamentoRepository medicamentoRepository;

    @Transactional(readOnly = true)
    public Page<ItemEstoque> executar(Pageable pageable, String busca) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        String filtro = busca != null && !busca.isBlank() ? busca.trim() : null;

        List<Medicamento> medicamentos = medicamentoRepository.findAtivosOrdenados(offset, limit, filtro);
        long total = medicamentoRepository.contarAtivos(filtro);

        List<UUID> ids = medicamentos.stream().map(Medicamento::getId).toList();
        Map<UUID, ItemEstoque> porMedicamento = estoqueRepository.findByMedicamentoIds(ids);

        List<ItemEstoque> content = medicamentos.stream()
            .map(med -> {
                ItemEstoque existente = porMedicamento.get(med.getId());
                if (existente != null) {
                    return existente;
                }
                return ItemEstoque.builder()
                    .medicamento(med)
                    .quantidadeAtual(0)
                    .quantidadeMinima(QTD_MIN_PADRAO)
                    .quantidadeMaxima(QTD_MAX_PADRAO)
                    .build();
            })
            .toList();

        return new PageImpl<>(content, pageable, total);
    }
}
