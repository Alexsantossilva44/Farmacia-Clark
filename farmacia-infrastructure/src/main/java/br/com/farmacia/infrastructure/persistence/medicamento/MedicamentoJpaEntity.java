package br.com.farmacia.infrastructure.persistence.medicamento;

import br.com.farmacia.domain.medicamento.enums.FormaFarmaceutica;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.enums.TipoMedicamento;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA para a tabela {@code medicamentos} (migration V1).
 *
 * <p>Adapter de persistência — não é a entidade de domínio. O mapeamento
 * domínio ↔ JPA é feito por {@link MedicamentoPersistenceMapper}, mantendo
 * o domínio livre de anotações de framework.</p>
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "medicamentos")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo_ean", length = 13, unique = true)
    private String codigoEan;

    @Column(name = "codigo_anvisa", length = 15, unique = true)
    private String codigoAnvisa;

    @Column(name = "nome_comercial", nullable = false, length = 80)
    private String nomeComercial;

    @Column(name = "nome_generico", length = 80)
    private String nomeGenerico;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMedicamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_farmaceutica", length = 30)
    private FormaFarmaceutica formaFarmaceutica;

    @Column(name = "concentracao", length = 50)
    private String concentracao;

    @Column(name = "apresentacao", length = 100)
    private String apresentacao;

    @Column(name = "classe_terapeutica", length = 100)
    private String classeTerapeutica;

    @Column(name = "requer_receita", nullable = false)
    private Boolean requerReceita;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_controle", nullable = false, length = 25)
    private NivelControle nivelControle;

    @Column(name = "preco_maximo_consumidor", precision = 10, scale = 2)
    private BigDecimal precoMaximoConsumidor;

    @Column(name = "fabricante_id")
    private UUID fabricanteId;

    @Column(name = "categoria_id")
    private UUID categoriaId;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
