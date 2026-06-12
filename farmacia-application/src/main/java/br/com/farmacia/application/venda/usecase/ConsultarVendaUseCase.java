package br.com.farmacia.application.venda.usecase;

import br.com.farmacia.domain.venda.entity.Venda;
import br.com.farmacia.domain.venda.exception.VendaNaoEncontradaException;
import br.com.farmacia.domain.venda.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Caso de Uso: Consultar Vendas.
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ConsultarVendaUseCase {

    private final VendaRepository vendaRepository;

    @Transactional(readOnly = true)
    public Venda buscarOuFalhar(UUID id) {
        return vendaRepository.findById(id)
            .orElseThrow(() -> new VendaNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public Page<Venda> listarComFiltro(LocalDate di, LocalDate df,
            UUID clienteId, UUID pdvId, String status, Pageable pageable) {
        // H-05: paginação delegada ao banco — não carrega todas as vendas em memória
        int offset = (int) pageable.getOffset();
        int limit  = pageable.getPageSize();
        List<Venda> paginadas = vendaRepository.findWithFilters(di, df, clienteId, pdvId, status, offset, limit);
        long total = vendaRepository.countWithFilters(di, df, clienteId, pdvId, status);
        return new PageImpl<>(paginadas, pageable, total);
    }
}
