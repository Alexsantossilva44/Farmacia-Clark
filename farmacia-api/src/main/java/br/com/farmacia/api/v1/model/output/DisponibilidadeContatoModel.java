package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisponibilidadeContatoModel {
    private boolean telefoneDisponivel;
    private boolean emailDisponivel;
}
