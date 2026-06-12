package br.com.farmacia.domain.financeiro.entity;

import br.com.farmacia.domain.financeiro.enums.StatusCaixa;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade raiz do agregado Caixa.
 *
 * <p>Controla o movimento financeiro de um {@link PDV} durante um turno
 * de operação (da abertura ao fechamento).</p>
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "funcionario")
public class Caixa {

    private UUID          id;
    private PDV           pdv;
    private Funcionario   funcionario;

    private LocalDateTime abertura;
    private LocalDateTime fechamento;

    @Builder.Default
    private BigDecimal    saldoAbertura = BigDecimal.ZERO;

    private BigDecimal    saldoFechamento;

    @Builder.Default
    private BigDecimal    totalVendas = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal    totalEntradas = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal    totalSaidas = BigDecimal.ZERO;

    @Builder.Default
    private StatusCaixa   status = StatusCaixa.ABERTO;

    private String        observacao;

    // ── Regras de Domínio ──────────────────────────────────────────────

    public boolean isAberto() {
        return status == StatusCaixa.ABERTO;
    }

    /**
     * Saldo esperado em caixa: abertura + vendas + entradas - saídas.
     */
    public BigDecimal saldoEsperado() {
        return saldoAbertura
            .add(totalVendas)
            .add(totalEntradas)
            .subtract(totalSaidas);
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao caixa");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    /**
     * Acumula o total de uma venda aprovada no caixa corrente.
     * Chamado por {@code RealizarVendaUseCase} após cada venda finalizada.
     */
    public void incrementarTotalVendas(BigDecimal valor) { // H-02: registra venda no totalizador do caixa
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor de venda deve ser não-negativo");
        }
        this.totalVendas = this.totalVendas.add(valor);
    }

    /**
     * Reverte o total de uma venda cancelada do caixa corrente.
     * Chamado por {@code CancelarVendaUseCase} no estorno financeiro.
     */
    public void decrementarTotalVendas(BigDecimal valor) { // C-03: estorna venda cancelada do totalizador
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor de estorno deve ser não-negativo");
        }
        this.totalVendas = this.totalVendas.subtract(valor).max(BigDecimal.ZERO);
    }

    /**
     * Encerra turno de caixa com saldo calculado.
     */
    public void fechar(String observacaoFechamento) {
        if (status != StatusCaixa.ABERTO) {
            throw new IllegalStateException("Caixa não está aberto para fechamento");
        }
        this.fechamento = LocalDateTime.now();
        this.saldoFechamento = saldoEsperado();
        this.status = StatusCaixa.FECHADO;
        if (observacaoFechamento != null && !observacaoFechamento.isBlank()) {
            this.observacao = observacaoFechamento;
        }
    }
}
