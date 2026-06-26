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
 * Entidade JPA para a tabela {@code fabricantes} (migration V1).
 *
 * <p>Usada para hidratar a referência de {@code Fabricante} ao reconstruir o
 * agregado {@code Medicamento} — assim o {@code MedicamentoModel} expõe dados
 * como razão social e nome fantasia, evitando referências apenas por id.</p>
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "fabricantes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FabricanteJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "razao_social", nullable = false, length = 80)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 80)
    private String nomeFantasia;

    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "autorizacao_anvisa", length = 30)
    private String autorizacaoAnvisa;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "ativo")
    private Boolean ativo;
}
