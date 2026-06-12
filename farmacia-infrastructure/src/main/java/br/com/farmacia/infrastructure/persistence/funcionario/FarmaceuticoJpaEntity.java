package br.com.farmacia.infrastructure.persistence.funcionario;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code farmaceuticos} (migration V3).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "farmaceuticos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmaceuticoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "funcionario_id", nullable = false, unique = true)
    private UUID funcionarioId;

    @Column(name = "crf", nullable = false, length = 15)
    private String crf;

    @Column(name = "uf_crf", nullable = false, length = 2)
    private String ufCrf;

    @Column(name = "especialidades")
    private String especialidades;

    @Column(name = "responsavel_tecnico", nullable = false)
    private Boolean responsavelTecnico;
}
