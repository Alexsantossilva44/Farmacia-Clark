package br.com.farmacia.infrastructure.persistence.estoque;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link ItemEstoqueJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface ItemEstoqueJpaRepository extends JpaRepository<ItemEstoqueJpaEntity, UUID> {

    Optional<ItemEstoqueJpaEntity> findByMedicamentoId(UUID medicamentoId);

    List<ItemEstoqueJpaEntity> findByMedicamentoIdIn(Collection<UUID> medicamentoIds);

    @Query("select i from ItemEstoqueJpaEntity i where i.quantidadeAtual < i.quantidadeMinima")
    List<ItemEstoqueJpaEntity> findItensAbaixoDoMinimo();

    @Query("select i from ItemEstoqueJpaEntity i where i.quantidadeAtual = 0")
    List<ItemEstoqueJpaEntity> findItensComEstoqueZerado();
}
