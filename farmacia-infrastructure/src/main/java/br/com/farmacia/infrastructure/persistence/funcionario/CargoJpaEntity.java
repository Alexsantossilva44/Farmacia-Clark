package br.com.farmacia.infrastructure.persistence.funcionario;

import br.com.farmacia.domain.funcionario.enums.RoleSistema;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code cargos} (migration V3).
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "cargos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 60, unique = true)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_sistema", nullable = false, length = 40)
    private RoleSistema roleSistema;
}
