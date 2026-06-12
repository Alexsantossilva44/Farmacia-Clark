package br.com.farmacia.application.venda.usecase;

import br.com.farmacia.domain.estoque.repository.EstoqueRepository;
import br.com.farmacia.domain.estoque.repository.LoteRepository;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository; // C-03: necessário para estorno financeiro
import br.com.farmacia.domain.venda.entity.Venda;
import br.com.farmacia.domain.venda.exception.VendaNaoPodeCancelarException;
import br.com.farmacia.domain.venda.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de Uso: Cancelar Venda com estorno de estoque.
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelarVendaUseCase {

    private final VendaRepository       vendaRepository;
    private final ConsultarVendaUseCase consultarUseCase;
    private final EstoqueRepository     estoqueRepository;
    private final LoteRepository        loteRepository;
    private final CaixaRepository       caixaRepository; // C-03: estorno do totalizador do caixa

    @Transactional
    public void executar(UUID vendaId, String motivo) {
        Venda venda = consultarUseCase.buscarOuFalhar(vendaId);

        if (!venda.podeCancelar()) {
            throw new VendaNaoPodeCancelarException(vendaId);
        }

        venda.getItens().forEach(item -> {
            estoqueRepository.incrementarSaldo(
                item.getMedicamento().getId(), item.getQuantidade());

            var lote = item.getLote();
            lote.restaurar(item.getQuantidade());
            loteRepository.save(lote);
        });

        // C-03: cancela pagamentos antes de cancelar a venda
        venda.getPagamentos().forEach(pagamento -> pagamento.cancelar());

        venda.cancelar(motivo);
        vendaRepository.save(venda);

        // C-03: estorna o valor da venda no totalizador do caixa
        if (venda.getCaixa() != null && venda.getCaixa().getId() != null) {
            caixaRepository.findById(venda.getCaixa().getId()).ifPresent(caixa -> {
                caixa.decrementarTotalVendas(venda.getTotal()); // reverte no totalizador do turno
                caixaRepository.save(caixa);
            });
        }

        log.info("Venda [{}] cancelada. Motivo: {}", vendaId, motivo);
    }
}
