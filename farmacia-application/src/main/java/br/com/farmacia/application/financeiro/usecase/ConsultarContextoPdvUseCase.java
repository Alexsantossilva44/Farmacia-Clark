package br.com.farmacia.application.financeiro.usecase;

import br.com.farmacia.domain.financeiro.exception.PdvNaoEncontradoException;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import br.com.farmacia.domain.financeiro.repository.PdvRepository;
import br.com.farmacia.domain.financeiro.entity.PDV;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de Uso: Consultar contexto operacional de um PDV.
 *
 * <p>Retorna dados do PDV e indica se há caixa aberto —
 * informações necessárias para o front-end montar a tela de vendas.</p>
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ConsultarContextoPdvUseCase {

    private final PdvRepository   pdvRepository;
    private final CaixaRepository caixaRepository;

    public record Output(UUID pdvId, String numero, String descricao, String status, boolean caixaAberto) {}

    @Transactional(readOnly = true)
    public Output executar(String numeroPdv) {
        // H-11: lógica de domínio encapsulada no use case — controller não acessa repositórios diretamente
        PDV pdv = pdvRepository.findByNumero(numeroPdv)
            .orElseThrow(() -> new PdvNaoEncontradoException(numeroPdv));

        boolean caixaAberto = caixaRepository.findCaixaAbertoPorPdv(pdv.getId()).isPresent();

        return new Output(pdv.getId(), pdv.getNumero(), pdv.getDescricao(),
                          pdv.getStatus().name(), caixaAberto);
    }
}
