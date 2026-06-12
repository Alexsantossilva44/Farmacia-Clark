package br.com.farmacia.domain.compra.repository;

import br.com.farmacia.domain.compra.entity.Fornecedor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FornecedorRepository {
    Fornecedor save(Fornecedor fornecedor);
    Optional<Fornecedor> findById(UUID id);
    Optional<Fornecedor> findByCnpj(String cnpj);
    List<Fornecedor> findAllAtivos();
}
