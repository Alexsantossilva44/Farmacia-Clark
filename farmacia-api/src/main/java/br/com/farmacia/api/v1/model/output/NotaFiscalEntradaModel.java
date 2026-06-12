package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class NotaFiscalEntradaModel {
    private UUID id;
    private UUID pedidoCompraId;
    private UUID fornecedorId;
    private String fornecedorNome;
    private String numeroNota;
    private String serie;
    private String chaveAcesso;
    private LocalDate dataEmissao;
    private LocalDate dataEntrada;
    private BigDecimal valorTotal;
    private String status;
    private LocalDateTime createdAt;
    private Integer quantidadeItens;
}
