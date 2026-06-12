package br.com.farmacia.api.v1.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CaixaModel {
    private UUID id;
    private UUID pdvId;
    private String pdvNumero;
    private UUID funcionarioId;
    private String funcionarioNome;
    private LocalDateTime abertura;
    private LocalDateTime fechamento;
    private BigDecimal saldoAbertura;
    private BigDecimal saldoFechamento;
    private BigDecimal totalVendas;
    private String status;
}
