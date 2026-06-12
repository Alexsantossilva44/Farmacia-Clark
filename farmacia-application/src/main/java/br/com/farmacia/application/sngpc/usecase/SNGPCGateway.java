package br.com.farmacia.application.sngpc.usecase;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.receituario.entity.Receita;
import lombok.Builder;

/**
 * Porta de saída (Port) para envio de dados ao SNGPC do governo.
 * A implementação concreta fica em farmacia-infrastructure.
 *
 * @author Alex Silva e Claude
 */
public interface SNGPCGateway {

    Response enviar(Request request);

    @Builder
    record Request(
        Receita    receita,
        Medicamento medicamento,
        Lote       lote,
        String     compradorNome,
        String     compradorCpf,
        int        quantidade
    ) {}

    record Response(
        String protocolo,
        String mensagem,
        boolean sucesso
    ) {}
}
