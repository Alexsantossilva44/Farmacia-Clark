package br.com.farmacia.infrastructure.persistence.cliente;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code clientes} (migration V3).
 *
 * <p>O endereço (Value Object {@code EnderecoVO}) é persistido de forma
 * achatada em colunas próprias. O mapeamento domínio ↔ JPA é feito por
 * {@link ClientePersistenceMapper}.</p>
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Nome completo — VARCHAR(100) desde migration V6 (antes 150 em V3). */
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "cpf", length = 11, unique = true)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "sexo", length = 1)
    private String sexo;

    /** Dígitos normalizados (10–11); UNIQUE via uk_clientes_telefone (V7). */
    @Column(name = "telefone", length = 20, unique = true)
    private String telefone;

    /** Minúsculas (ClienteValidacao.normalizarEmail); UNIQUE via uk_clientes_email (V7). */
    @Column(name = "email", length = 120, unique = true)
    private String email;

    @Column(name = "logradouro", length = 200)
    private String logradouro;

    @Column(name = "numero", length = 10)
    private String numero;

    @Column(name = "complemento", length = 50)
    private String complemento;

    @Column(name = "bairro", length = 80)
    private String bairro;

    @Column(name = "cidade", length = 80)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "alergias")
    private String alergias;

    @Column(name = "observacoes")
    private String observacoes;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;
}
