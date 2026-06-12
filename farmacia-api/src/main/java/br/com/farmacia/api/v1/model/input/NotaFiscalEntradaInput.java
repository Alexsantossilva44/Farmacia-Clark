package br.com.farmacia.api.v1.model.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class NotaFiscalEntradaInput {
    @NotNull private UUID fornecedorId;
    @NotBlank private String numeroNota;
    private String serie;
    @NotBlank private String chaveAcesso;
    @NotNull private LocalDate dataEmissao;
    private LocalDate dataEntrada;
    private BigDecimal valorTotal;
    private UUID pedidoCompraId;
    @NotEmpty @Valid private List<ItemNotaFiscalInput> itens;
}
