package br.com.farmacia.application.estoque.usecase;

import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import br.com.farmacia.domain.estoque.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListarMovimentacoesEstoqueUseCase {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> executar(UUID medicamentoId, TipoMovimentacao tipo, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        var content = movimentacaoRepository.buscar(medicamentoId, tipo, offset, limit);
        long total = movimentacaoRepository.contar(medicamentoId, tipo);
        return new PageImpl<>(content, pageable, total);
    }
}
