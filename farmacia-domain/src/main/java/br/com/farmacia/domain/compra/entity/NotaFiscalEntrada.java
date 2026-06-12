package br.com.farmacia.domain.compra.entity;

import br.com.farmacia.domain.compra.enums.StatusNota;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotaFiscalEntrada {

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
    private StatusNota status;
    private LocalDateTime createdAt;
    private int quantidadeItens;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída à nota fiscal");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void finalizarConferencia(int quantidadeItens, StatusNota statusConferencia) {
        this.quantidadeItens = quantidadeItens;
        this.status = statusConferencia;
    }
}
