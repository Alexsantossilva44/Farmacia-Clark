package br.com.farmacia.api.v1.model;

import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.enums.TipoReceita;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ReceitaModel {
    private UUID id;
    private String numeroReceita;
    private LocalDate dataEmissao;
    private LocalDate dataValidade;
    private TipoReceita tipo;
    private StatusReceita status;
    private String cid;
    private Boolean retida;
    private UUID clienteId;
    private String clienteNome;
    private UUID prescritorId;
    private String prescritorNome;
    private String prescritorCrm;
    private LocalDateTime dataValidacao;
    private String motivoRejeicao;
}
