package br.com.farmacia.api.v1.model.input;

import br.com.farmacia.domain.medicamento.enums.FormaFarmaceutica;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.enums.TipoMedicamento;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para criação e atualização de medicamento.
 *
 * <p><b>Heurística AlgaWorks</b>: DTOs de entrada se chamam {@code XxxInput}
 * e DTOs de saída se chamam {@code XxxModel}. Nunca expor a entidade
 * de domínio diretamente na API.</p>
 */
@Getter
@Setter
public class MedicamentoInput {

    @Pattern(regexp = "\\d{13}", message = "EAN deve conter exatamente 13 dígitos numéricos")
    private String codigoEan;

    @Size(max = 15, message = "Código ANVISA deve ter no máximo 15 caracteres")
    private String codigoAnvisa;

    @NotBlank(message = "Nome comercial é obrigatório")
    @Size(max = 80, message = "Nome comercial deve ter no máximo 80 caracteres")
    private String nomeComercial;

    @Size(max = 80, message = "Nome genérico deve ter no máximo 80 caracteres")
    private String nomeGenerico;

    @NotNull(message = "Tipo do medicamento é obrigatório")
    private TipoMedicamento tipo;

    private FormaFarmaceutica formaFarmaceutica;

    @Size(max = 50, message = "Concentração deve ter no máximo 50 caracteres")
    private String concentracao;

    @Size(max = 100)
    private String apresentacao;

    @Size(max = 100)
    private String classeTerapeutica;

    @NotNull(message = "Informe se o medicamento requer receita")
    private Boolean requerReceita;

    @NotNull(message = "Nível de controle é obrigatório")
    private NivelControle nivelControle;

    @NotNull(message = "PMC é obrigatório")
    @DecimalMin(value = "0.01", message = "PMC deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "PMC inválido")
    private BigDecimal precoMaximoConsumidor;

    @NotNull(message = "Fabricante é obrigatório")
    private FabricanteIdInput fabricante;

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaIdInput categoria;

    private List<PrincipioAtivoIdInput> principiosAtivos;

    // ── IDs de referência (padrão AlgaWorks: objetos com id, não UUID solto) ──

    @Getter
    @Setter
    public static class FabricanteIdInput {
        @NotNull(message = "ID do fabricante é obrigatório")
        private UUID id;
    }

    @Getter
    @Setter
    public static class CategoriaIdInput {
        @NotNull(message = "ID da categoria é obrigatório")
        private UUID id;
    }

    @Getter
    @Setter
    public static class PrincipioAtivoIdInput {
        @NotNull
        private UUID id;
    }
}
