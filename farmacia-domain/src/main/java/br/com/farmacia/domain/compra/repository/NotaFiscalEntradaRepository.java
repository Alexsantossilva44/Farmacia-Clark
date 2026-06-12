package br.com.farmacia.domain.compra.repository;

import br.com.farmacia.domain.compra.entity.NotaFiscalEntrada;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotaFiscalEntradaRepository {
    NotaFiscalEntrada save(NotaFiscalEntrada nota);
    Optional<NotaFiscalEntrada> findById(UUID id);
    Optional<NotaFiscalEntrada> findByChaveAcesso(String chaveAcesso);
    List<NotaFiscalEntrada> findAllOrderByDataEntradaDesc();
    long contarLotesPorNota(UUID notaFiscalId);
}
