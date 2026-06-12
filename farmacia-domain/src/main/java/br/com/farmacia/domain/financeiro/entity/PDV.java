package br.com.farmacia.domain.financeiro.entity;

import br.com.farmacia.domain.financeiro.enums.StatusPDV;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de domínio: PDV (Ponto de Venda).
 *
 * <p>Representa um terminal físico de atendimento. Um PDV pode ter, no máximo,
 * um {@link Caixa} aberto por vez.</p>
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class PDV {

    private UUID          id;
    private String        numero;
    private String        descricao;

    @Builder.Default
    private StatusPDV     status = StatusPDV.FECHADO;

    private LocalDateTime createdAt;

    // ── Regras de Domínio ──────────────────────────────────────────────

    public boolean isOperavel() {
        return status == StatusPDV.ABERTO;
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao PDV");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void abrir() {
        this.status = StatusPDV.ABERTO;
    }

    public void fechar() {
        this.status = StatusPDV.FECHADO;
    }
}
