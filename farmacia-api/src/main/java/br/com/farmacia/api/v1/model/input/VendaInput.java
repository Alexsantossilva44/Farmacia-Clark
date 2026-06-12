package br.com.farmacia.api.v1.model.input;

import br.com.farmacia.domain.venda.enums.FormaPagamento;
import br.com.farmacia.domain.venda.enums.TipoAtendimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para realização de uma venda.
 *
 * <p><b>Heurística AlgaWorks</b>: validações em camadas.
 * {@code @Valid} em listas propaga a validação para os itens internos,
 * garantindo que erros de itens individuais apareçam no Problem Details
 * com o caminho correto (ex: {@code itens[0].quantidade}).</p>
 */
@Getter
@Setter
public class VendaInput {

    @NotNull(message = "PDV é obrigatório")
    private PDVIdInput pdv;

    @NotNull(message = "Funcionário é obrigatório")
    private FuncionarioIdInput funcionario;

    // Cliente é opcional (venda avulsa sem identificação)
    private ClienteIdInput cliente;

    // Receita obrigatória se algum item exigir
    private ReceitaIdInput receita;

    @NotEmpty(message = "A venda deve conter pelo menos um item")
    @Valid
    private List<ItemVendaInput> itens;

    @NotEmpty(message = "A venda deve ter pelo menos uma forma de pagamento")
    @Valid
    private List<PagamentoInput> pagamentos;

    // Obrigatório para medicamentos controlados e antimicrobianos
    private String compradorCpf;
    private String compradorNome;

    private TipoAtendimento tipoAtendimento = TipoAtendimento.BALCAO;
    private String observacao;

    // ── Sub-inputs de referência ──────────────────────────────────────────

    @Getter @Setter
    public static class PDVIdInput {
        @NotNull private UUID id;
    }

    @Getter @Setter
    public static class FuncionarioIdInput {
        @NotNull private UUID id;
    }

    @Getter @Setter
    public static class ClienteIdInput {
        @NotNull private UUID id;
    }

    @Getter @Setter
    public static class ReceitaIdInput {
        @NotNull private UUID id;
    }

    @Getter @Setter
    public static class ItemVendaInput {

        @NotNull(message = "Medicamento do item é obrigatório")
        private MedicamentoIdInput medicamento;

        @NotNull @Min(value = 1, message = "Quantidade mínima é 1")
        @Max(value = 999, message = "Quantidade máxima por item é 999")
        private Integer quantidade;

        @NotNull @DecimalMin(value = "0.01", message = "Preço unitário deve ser maior que zero")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal precoUnitario;

        @DecimalMin(value = "0.00")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal desconto = BigDecimal.ZERO;

        @Getter @Setter
        public static class MedicamentoIdInput {
            @NotNull private UUID id;
        }
    }

    @Getter @Setter
    public static class PagamentoInput {

        @NotNull(message = "Forma de pagamento é obrigatória")
        private FormaPagamento forma;

        @NotNull @DecimalMin(value = "0.01", message = "Valor do pagamento deve ser positivo")
        @Digits(integer = 10, fraction = 2)
        private BigDecimal valor;

        // Apenas para convênio
        private UUID convenioId;
    }
}
