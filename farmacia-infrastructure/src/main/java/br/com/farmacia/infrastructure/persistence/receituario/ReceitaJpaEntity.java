package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.enums.TipoReceita;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code receitas} (migration V4).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "receitas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero_receita", length = 30)
    private String numeroReceita;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 25)
    private TipoReceita tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusReceita status;

    @Column(name = "cid", length = 10)
    private String cid;

    @Column(name = "retida", nullable = false)
    private Boolean retida;

    @Column(name = "imagem_path", length = 300)
    private String imagemPath;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    @Column(name = "prescritor_id")
    private UUID prescritorId;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "farmaceutico_id")
    private UUID farmaceuticoId;

    @Column(name = "data_validacao")
    private LocalDateTime dataValidacao;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
