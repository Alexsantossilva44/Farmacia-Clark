package br.com.farmacia.domain.estoque.entity;

import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade de domínio: Lote de medicamento.
 *
 * <p>Contém as regras FEFO (First Expired, First Out):</p>
 * <ul>
 *   <li>{@link #estaVencido()} — não deve ser dispensado</li>
 *   <li>{@link #venceEm(int)} — gera alertas proporcionais</li>
 *   <li>{@link #temEstoque()} — verifica disponibilidade</li>
 * </ul>
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "medicamento")
public class Lote {

    private UUID        id;
    private UUID        notaFiscalId;
    private String      numeroLote;
    private LocalDate   dataFabricacao;
    private LocalDate   dataValidade;
    private Integer     quantidadeRecebida;
    private Integer     quantidadeAtual;
    private BigDecimal  precoCusto;

    @Builder.Default
    private StatusLote status = StatusLote.ATIVO;

    private Medicamento medicamento;

    // ── Regras de Domínio ──────────────────────────────────────────────

    public boolean estaVencido() {
        return !LocalDate.now().isBefore(this.dataValidade); // C-06: inclui o próprio dia de vencimento
    }

    public boolean venceEm(int dias) {
        return LocalDate.now().plusDays(dias).isAfter(this.dataValidade);
    }

    public boolean temEstoque() {
        return this.quantidadeAtual != null && this.quantidadeAtual > 0;
    }

    public boolean estaDisponivel() {
        return status == StatusLote.ATIVO && temEstoque() && !estaVencido();
    }

    public long diasParaVencer() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), this.dataValidade);
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao lote");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    /**
     * Consome unidades do lote (venda / saída).
     */
    public void consumir(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        if (quantidadeAtual == null || quantidadeAtual < quantidade) {
            throw new IllegalStateException("Saldo insuficiente no lote");
        }
        quantidadeAtual -= quantidade;
        if (quantidadeAtual == 0) {
            status = StatusLote.ESGOTADO;
        }
    }

    /**
     * Restaura unidades ao lote (cancelamento de venda).
     */
    public void restaurar(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        quantidadeAtual = (quantidadeAtual != null ? quantidadeAtual : 0) + quantidade;
        status = StatusLote.ATIVO;
    }

    /**
     * Incrementa saldo por ajuste positivo ou entrada adicional.
     */
    public void incrementarSaldo(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        quantidadeAtual = (quantidadeAtual != null ? quantidadeAtual : 0) + quantidade;
        quantidadeRecebida = (quantidadeRecebida != null ? quantidadeRecebida : 0) + quantidade;
        if (status == StatusLote.ESGOTADO) {
            status = StatusLote.ATIVO;
        }
    }

    /**
     * Decrementa saldo por ajuste negativo.
     */
    public void decrementarSaldo(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        int novoSaldo = quantidadeAtual - quantidade;
        if (novoSaldo < 0) {
            throw new IllegalStateException("Saldo insuficiente no lote");
        }
        quantidadeAtual = novoSaldo;
        if (novoSaldo == 0) {
            status = StatusLote.ESGOTADO;
        }
    }

    /**
     * Registra entrada adicional em lote existente (NF-e / reposição).
     */
    public void registrarEntradaAdicional(int quantidade, UUID notaFiscalId,
                                          LocalDate novaDataValidade, BigDecimal precoCusto) {
        if (notaFiscalId != null) {
            this.notaFiscalId = notaFiscalId;
        }
        incrementarSaldo(quantidade);
        // H-08: extensão de dataValidade removida — a data original deve ser mantida
        // para garantir rastreabilidade FEFO; nunca recuar a validade de um lote.
        if (precoCusto != null) {
            this.precoCusto = precoCusto;
        }
    }

    /**
     * Bloqueia lote vencido, zerando saldo e retornando quantidade bloqueada.
     */
    public int bloquearPorVencimento() {
        int bloqueada = quantidadeAtual != null ? quantidadeAtual : 0;
        status = StatusLote.VENCIDO;
        quantidadeAtual = 0;
        return bloqueada;
    }
}
