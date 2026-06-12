package br.com.farmacia.infrastructure.persistence.medicamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code categorias} (migration V1).
 *
 * <p>Usada para hidratar a referência de {@code Categoria} ao reconstruir o
 * agregado {@code Medicamento}.</p>
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "categorias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "categoria_pai_id")
    private UUID categoriaPaiId;

    @Column(name = "ativo")
    private Boolean ativo;
}
