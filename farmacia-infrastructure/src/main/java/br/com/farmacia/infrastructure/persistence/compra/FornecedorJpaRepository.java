package br.com.farmacia.infrastructure.persistence.compra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FornecedorJpaRepository extends JpaRepository<FornecedorJpaEntity, UUID> {
    Optional<FornecedorJpaEntity> findByCnpj(String cnpj);
    List<FornecedorJpaEntity> findByAtivoTrueOrderByRazaoSocialAsc();
}
