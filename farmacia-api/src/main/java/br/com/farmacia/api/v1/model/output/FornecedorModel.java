package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class FornecedorModel {
    private UUID id;
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private Boolean ativo;
}
