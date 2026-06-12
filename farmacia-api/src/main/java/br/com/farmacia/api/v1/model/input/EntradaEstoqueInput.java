package br.com.farmacia.api.v1.model.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class EntradaEstoqueInput {
    @NotNull private UUID medicamentoId;
    @NotBlank private String numeroLote;
    @NotNull private LocalDate dataValidade;
    private LocalDate dataFabricacao;
    @NotNull @Min(1) private Integer quantidade;
    private BigDecimal precoCusto;
    private Integer quantidadeMinima;
    private Integer quantidadeMaxima;
    private String observacao;
}
