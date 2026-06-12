package br.com.farmacia.infrastructure.persistence.compra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotaFiscalEntradaJpaRepository extends JpaRepository<NotaFiscalEntradaJpaEntity, UUID> {
    Optional<NotaFiscalEntradaJpaEntity> findByChaveAcesso(String chaveAcesso);
    List<NotaFiscalEntradaJpaEntity> findAllByOrderByDataEntradaDescCreatedAtDesc();
}
