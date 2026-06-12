package br.com.farmacia.application.estoque.usecase;

import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.exception.ItemEstoqueNaoEncontradoException;
import br.com.farmacia.domain.estoque.exception.ParametroInvalidoException;
import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Atualiza parâmetros de controle (mínimo/máximo) de um item de estoque.
 */
@Service
@RequiredArgsConstructor
public class AtualizarItemEstoqueUseCase {

    private final EstoqueRepository estoqueRepository;

    @Transactional
    public ItemEstoque executar(UUID medicamentoId, Input input) {
        ItemEstoque item = estoqueRepository.findByMedicamentoId(medicamentoId)
            .orElseThrow(() -> new ItemEstoqueNaoEncontradoException(medicamentoId));

        if (input.quantidadeMinima() != null && input.quantidadeMinima() < 1) { // M-06: zero é operacionalmente inválido
            throw new ParametroInvalidoException("Quantidade mínima deve ser pelo menos 1");
        }
        if (input.quantidadeMaxima() != null && input.quantidadeMaxima() <= 0) {
            throw new ParametroInvalidoException("Quantidade máxima deve ser maior que zero");
        }

        item.atualizarLimites(input.quantidadeMinima(), input.quantidadeMaxima());

        if (item.getQuantidadeMinima() != null && item.getQuantidadeMaxima() != null
                && item.getQuantidadeMinima() > item.getQuantidadeMaxima()) {
            throw new ParametroInvalidoException("Quantidade mínima não pode exceder a máxima");
        }

        return estoqueRepository.salvar(item);
    }

    public record Input(Integer quantidadeMinima, Integer quantidadeMaxima) {}
}
