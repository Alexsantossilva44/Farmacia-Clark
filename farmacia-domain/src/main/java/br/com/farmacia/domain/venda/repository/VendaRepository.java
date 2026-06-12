package br.com.farmacia.domain.venda.repository;

import br.com.farmacia.domain.venda.entity.Venda;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para persistência de vendas.
 * Domínio puro — sem Spring Data.
 * @author Alex Silva e Claude
 */
public interface VendaRepository {
    Venda save(Venda venda);
    Optional<Venda> findById(UUID id);
    List<Venda> findWithFilters(LocalDate dataInicio, LocalDate dataFim,
                                UUID clienteId, UUID pdvId, String status);

    // H-05: versão paginada no banco — evita carregar todas as vendas em memória
    List<Venda> findWithFilters(LocalDate dataInicio, LocalDate dataFim,
                                UUID clienteId, UUID pdvId, String status,
                                int offset, int limit);
    long countWithFilters(LocalDate dataInicio, LocalDate dataFim,
                          UUID clienteId, UUID pdvId, String status);
}
