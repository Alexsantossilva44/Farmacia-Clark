package br.com.farmacia.api.v1.model;

import br.com.farmacia.domain.medicamento.enums.FormaFarmaceutica;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.enums.TipoMedicamento;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de saída para medicamento.
 *
 * <p><b>Heurística AlgaWorks</b>: o Model expõe exatamente o que o cliente
 * precisa ver — nunca campos de controle interno, senhas, ou entidades inteiras.
 * Objetos aninhados são representados por sub-models resumidos.</p>
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Representação de um medicamento cadastrado")
public class MedicamentoModel {

    @Schema(description = "Identificador único do medicamento", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Código de barras EAN-13", example = "7891234567890")
    private String codigoEan;

    @Schema(description = "Código ANVISA do produto", example = "123456789012345")
    private String codigoAnvisa;

    @Schema(description = "Nome comercial/marca", example = "Rivotril")
    private String nomeComercial;

    @Schema(description = "Nome genérico (DCB)", example = "Clonazepam")
    private String nomeGenerico;

    private TipoMedicamento tipo;
    private FormaFarmaceutica formaFarmaceutica;
    private String concentracao;
    private String apresentacao;
    private String classeTerapeutica;
    private Boolean requerReceita;
    private NivelControle nivelControle;

    @Schema(description = "Preço Máximo ao Consumidor (ANVISA)", example = "45.90")
    private BigDecimal precoMaximoConsumidor;

    private Boolean ativo;

    private FabricanteResumoModel fabricante;
    private CategoriaResumoModel categoria;
    private List<PrincipioAtivoResumoModel> principiosAtivos;

    // ── Sub-models resumidos (evita N+1 e over-fetching) ─────────────────────

    @Getter
    @Setter
    public static class FabricanteResumoModel {
        private UUID id;
        private String razaoSocial;
        private String nomeFantasia;
    }

    @Getter
    @Setter
    public static class CategoriaResumoModel {
        private UUID id;
        private String nome;
    }

    @Getter
    @Setter
    public static class PrincipioAtivoResumoModel {
        private UUID id;
        private String nome;
        private String dcb;
    }
}
