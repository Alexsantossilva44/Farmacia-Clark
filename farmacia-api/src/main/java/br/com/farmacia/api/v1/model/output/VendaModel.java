package br.com.farmacia.api.v1.model;

import br.com.farmacia.domain.venda.enums.FormaPagamento;
import br.com.farmacia.domain.venda.enums.StatusPagamento;
import br.com.farmacia.domain.venda.enums.StatusVenda;
import br.com.farmacia.domain.venda.enums.TipoAtendimento;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de saída para Venda.
 *
 * <p><b>Heurística AlgaWorks</b>: o Model de saída nunca expõe dados
 * sensíveis do domínio (custo do produto, margem, dados bancários internos).
 * Apenas o que o cliente da API precisa ver para operar a interface.</p>
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Representação completa de uma venda finalizada")
public class VendaModel {

    private UUID id;

    @Schema(description = "Número do cupom fiscal", example = "CUP-1719000000-AB12CD")
    private String numeroCupom;

    private LocalDateTime dataHora;
    private StatusVenda status;
    private TipoAtendimento tipoAtendimento;

    @Schema(description = "Valor bruto antes de descontos", example = "45.90")
    private BigDecimal subtotal;

    @Schema(description = "Total de descontos aplicados", example = "5.00")
    private BigDecimal desconto;

    @Schema(description = "Valor final cobrado", example = "40.90")
    private BigDecimal total;

    private String observacao;

    private ClienteResumoModel cliente;
    private FuncionarioResumoModel funcionario;
    private PDVResumoModel pdv;
    private ReceitaResumoModel receita;

    private List<ItemVendaModel> itens;
    private List<PagamentoModel> pagamentos;

    // ── Sub-models ────────────────────────────────────────────────────────

    @Getter @Setter
    public static class ClienteResumoModel {
        private UUID id;
        private String nome;
        private String cpf;
    }

    @Getter @Setter
    public static class FuncionarioResumoModel {
        private UUID id;
        private String nome;
    }

    @Getter @Setter
    public static class PDVResumoModel {
        private UUID id;
        private String numero;
    }

    @Getter @Setter
    public static class ReceitaResumoModel {
        private UUID id;
        private String numeroReceita;
        private String tipo;
        private Boolean retida;
    }

    @Getter @Setter
    public static class ItemVendaModel {
        private UUID id;
        private Integer quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal desconto;
        private BigDecimal subtotal;
        private MedicamentoResumoModel medicamento;
        private LoteResumoModel lote;

        @Getter @Setter
        public static class MedicamentoResumoModel {
            private UUID id;
            private String nomeComercial;
            private String nomeGenerico;
            private String concentracao;
        }

        @Getter @Setter
        public static class LoteResumoModel {
            private UUID id;
            private String numeroLote;
            private java.time.LocalDate dataValidade;
        }
    }

    @Getter @Setter
    public static class PagamentoModel {
        private UUID id;
        private FormaPagamento forma;
        private BigDecimal valor;
        private BigDecimal troco;
        private StatusPagamento status;
        private LocalDateTime dataHora;
    }
}
