package br.com.farmacia.domain.financeiro.repository;

import br.com.farmacia.domain.financeiro.entity.PDV;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para PDVs (pontos de venda).
 *
 * @author Alex Silva e Claude
 */
public interface PdvRepository {

    PDV save(PDV pdv);

    Optional<PDV> findById(UUID id);

    Optional<PDV> findByNumero(String numero);
}
