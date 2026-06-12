package br.com.farmacia.domain.financeiro.repository;

import br.com.farmacia.domain.financeiro.entity.Caixa;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para caixa.
 *
 * @author Alex Silva e Claude
 */
public interface CaixaRepository {
    Caixa save(Caixa caixa);
    Optional<Caixa> findById(UUID id);
    Optional<Caixa> findCaixaAbertoPorPdv(UUID pdvId);
}
