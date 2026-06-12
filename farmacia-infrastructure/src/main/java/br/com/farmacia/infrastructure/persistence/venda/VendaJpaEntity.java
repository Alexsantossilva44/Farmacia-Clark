package br.com.farmacia.infrastructure.persistence.venda;

import br.com.farmacia.domain.venda.enums.StatusVenda;
import br.com.farmacia.domain.venda.enums.TipoAtendimento;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA raiz do agregado Venda — tabela {@code vendas} (migration V5).
 *
 * <p>Os itens e pagamentos são persistidos em cascata por pertencerem ao
 * agregado. As referências a outros agregados (pdv, caixa, funcionário,
 * cliente, receita) são guardadas por id (FK).</p>
 *
 * @author Alex Silva e Claude
 */
@Entity
@Table(name = "vendas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendaJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pdv_id", nullable = false)
    private UUID pdvId;

    @Column(name = "caixa_id", nullable = false)
    private UUID caixaId;

    @Column(name = "funcionario_id", nullable = false)
    private UUID funcionarioId;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "receita_id")
    private UUID receitaId;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusVenda status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_atendimento", nullable = false, length = 20)
    private TipoAtendimento tipoAtendimento;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "desconto", nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "numero_cupom", length = 30)
    private String numeroCupom;

    @Column(name = "observacao")
    private String observacao;

    @Builder.Default
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVendaJpaEntity> itens = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagamentoJpaEntity> pagamentos = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
