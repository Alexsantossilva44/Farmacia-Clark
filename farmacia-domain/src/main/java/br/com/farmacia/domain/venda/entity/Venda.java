package br.com.farmacia.domain.venda.entity;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.venda.enums.StatusVenda;
import br.com.farmacia.domain.venda.enums.TipoAtendimento;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade raiz do agregado Venda.
 *
 * <p>Métodos de domínio:</p>
 * <ul>
 *   <li>{@link #recalcularTotais()} — recalcula subtotal e total</li>
 *   <li>{@link #podeCancelar()} — venda só pode ser cancelada no mesmo dia</li>
 * </ul>
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"itens", "pagamentos"})
public class Venda {

    private UUID            id;
    private PDV             pdv;
    private Caixa           caixa;
    private Funcionario     funcionario;
    private Cliente         cliente;
    private Receita         receita;
    private LocalDateTime   dataHora;
    private String          numeroCupom;

    @Builder.Default
    private StatusVenda     status = StatusVenda.ABERTA;

    @Builder.Default
    private TipoAtendimento tipoAtendimento = TipoAtendimento.BALCAO;

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    private String          observacao;

    @Builder.Default
    private List<ItemVenda> itens = new ArrayList<>();

    @Builder.Default
    private List<Pagamento> pagamentos = new ArrayList<>();

    // ── Regras de Domínio ──────────────────────────────────────────────

    public void recalcularTotais() {
        this.subtotal = itens.stream()
            .map(ItemVenda::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.subtract(this.desconto);
    }

    public boolean podeCancelar() {
        return dataHora != null
            && dataHora.toLocalDate().isEqual(java.time.LocalDate.now())
            && status == StatusVenda.FINALIZADA;
    }

    /**
     * Atribui identidade ao agregado recém-criado (persistência).
     */
    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída à venda");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void associarCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public void associarReceita(Receita receita) {
        this.receita = receita;
    }

    /**
     * Finaliza a venda após validação de itens e pagamentos.
     */
    public void finalizar(String numeroCupom) {
        if (status != StatusVenda.ABERTA) {
            throw new IllegalStateException("Venda não está aberta para finalização");
        }
        if (numeroCupom == null || numeroCupom.isBlank()) {
            throw new IllegalArgumentException("Número do cupom é obrigatório");
        }
        this.numeroCupom = numeroCupom;
        this.status = StatusVenda.FINALIZADA;
    }

    /**
     * Cancela venda finalizada no dia corrente.
     */
    public void cancelar(String motivo) {
        if (!podeCancelar()) {
            throw new IllegalStateException(
                "Venda não pode ser cancelada. Status: " + status);
        }
        this.status = StatusVenda.CANCELADA;
        this.observacao = motivo;
    }
}
