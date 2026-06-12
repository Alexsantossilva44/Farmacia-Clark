package br.com.farmacia.application.financeiro.usecase;

import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta caixa aberto por PDV.
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ConsultarCaixaUseCase {

    private final CaixaRepository caixaRepository;

    @Transactional(readOnly = true)
    public Optional<Caixa> buscarCaixaAbertoPorPdv(UUID pdvId) {
        return caixaRepository.findCaixaAbertoPorPdv(pdvId);
    }
}
