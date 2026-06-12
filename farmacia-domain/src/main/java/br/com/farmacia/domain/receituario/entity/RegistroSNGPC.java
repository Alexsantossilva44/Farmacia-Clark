package br.com.farmacia.domain.receituario.entity;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.receituario.enums.StatusEnvio;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade: Registro de dispensação de medicamento controlado para o SNGPC.
 * Sistema Nacional de Gerenciamento de Produtos Controlados — ANVISA.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class RegistroSNGPC {

    private UUID          id;
    private Receita       receita;
    private Medicamento   medicamento;
    private Lote          lote;
    private String        compradorNome;
    private String        compradorCpf;
    private String        compradorRg;
    private Integer       quantidade;

    @Builder.Default
    private StatusEnvio   statusEnvio = StatusEnvio.PENDENTE;

    private LocalDateTime dataRegistro;
    private LocalDateTime dataEnvio;
    private String        numeroProtocolo;
    private String        retornoGoverno;

    @Builder.Default
    private Integer       tentativasEnvio = 0;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao registro SNGPC");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void iniciarEnvio() {
        tentativasEnvio = (tentativasEnvio != null ? tentativasEnvio : 0) + 1;
        statusEnvio = StatusEnvio.ENVIADO;
        dataEnvio = LocalDateTime.now();
    }

    public void confirmarEnvio(String protocolo) {
        statusEnvio = StatusEnvio.CONFIRMADO;
        numeroProtocolo = protocolo;
    }

    public void marcarErroDefinitivo() {
        statusEnvio = StatusEnvio.ERRO;
    }

    public void reagendarEnvio() {
        statusEnvio = StatusEnvio.PENDENTE;
    }
}
