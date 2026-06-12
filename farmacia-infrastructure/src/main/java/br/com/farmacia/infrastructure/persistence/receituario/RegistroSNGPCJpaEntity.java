package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.receituario.enums.StatusEnvio;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code registros_sngpc} (migration V4).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "registros_sngpc")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroSNGPCJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "receita_id", nullable = false)
    private UUID receitaId;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    @Column(name = "lote_id", nullable = false)
    private UUID loteId;

    @Column(name = "comprador_nome", nullable = false, length = 150)
    private String compradorNome;

    @Column(name = "comprador_cpf", nullable = false, length = 11)
    private String compradorCpf;

    @Column(name = "comprador_rg", length = 20)
    private String compradorRg;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_envio", nullable = false, length = 20)
    private StatusEnvio statusEnvio;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "numero_protocolo", length = 50)
    private String numeroProtocolo;

    @Column(name = "retorno_governo")
    private String retornoGoverno;

    @Column(name = "tentativas_envio", nullable = false)
    private Integer tentativasEnvio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
