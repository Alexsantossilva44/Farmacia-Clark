package br.com.farmacia.infrastructure.persistence.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data para {@link ClienteJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {

    Optional<ClienteJpaEntity> findByCpf(String cpf);

    @Query(
        value = """
            SELECT * FROM clientes
            WHERE telefone IS NOT NULL
              AND REGEXP_REPLACE(telefone, '[^0-9]', '', 'g') = :digits
            LIMIT 1
            """,
        nativeQuery = true)
    Optional<ClienteJpaEntity> findByTelefoneDigits(@Param("digits") String digits);

    @Query(
        value = """
            SELECT * FROM clientes
            WHERE email IS NOT NULL
              AND LOWER(TRIM(email)) = LOWER(TRIM(:email))
            LIMIT 1
            """,
        nativeQuery = true)
    Optional<ClienteJpaEntity> findByEmailNormalizado(@Param("email") String email);

    @Query(
        value = """
            SELECT * FROM clientes
            WHERE unaccent(LOWER(nome)) LIKE unaccent(LOWER(CONCAT('%', :nome, '%')))
            ORDER BY nome
            """,
        nativeQuery = true)
    List<ClienteJpaEntity> findByNomeContendo(@Param("nome") String nome);
}
