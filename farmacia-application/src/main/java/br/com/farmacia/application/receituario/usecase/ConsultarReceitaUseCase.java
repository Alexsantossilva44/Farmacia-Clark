package br.com.farmacia.application.receituario.usecase;

import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;
import br.com.farmacia.domain.receituario.exception.ReceitaPorNumeroNaoEncontradaException;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consulta receitas por id ou número.
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ConsultarReceitaUseCase {

    private final ReceitaRepository receitaRepository;

    @Transactional(readOnly = true)
    public Receita buscarOuFalhar(UUID id) {
        return receitaRepository.findById(id)
            .orElseThrow(() -> new ReceitaNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public Receita buscarPorNumeroOuFalhar(String numero) {
        return receitaRepository.findByNumeroReceita(numero)
            .orElseThrow(() -> new ReceitaPorNumeroNaoEncontradaException(numero));
    }
}
