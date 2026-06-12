package br.com.farmacia.application.financeiro.usecase;

import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.enums.StatusCaixa;
import br.com.farmacia.domain.financeiro.exception.CaixaAbertoNaoEncontradoException;
import br.com.farmacia.domain.financeiro.exception.CaixaNaoEstaAbertoException;
import br.com.farmacia.domain.financeiro.exception.PdvNaoEncontradoException;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import br.com.farmacia.domain.financeiro.repository.PdvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Caso de uso: fechar caixa aberto em um PDV.
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FecharCaixaUseCase {

    private final CaixaRepository caixaRepository;
    private final PdvRepository pdvRepository;

    @Transactional
    public Output executar(Input input) {
        Caixa caixa = caixaRepository.findCaixaAbertoPorPdv(input.pdvId())
            .orElseThrow(() -> new CaixaAbertoNaoEncontradoException(input.pdvId()));

        if (caixa.getStatus() != StatusCaixa.ABERTO) {
            throw new CaixaNaoEstaAbertoException(caixa.getId());
        }

        BigDecimal saldoEsperado = caixa.saldoEsperado();
        caixa.fechar(input.observacao());

        Caixa salvo = caixaRepository.save(caixa);

        PDV pdv = pdvRepository.findById(input.pdvId())
            .orElseThrow(() -> new PdvNaoEncontradoException(input.pdvId()));
        pdv.fechar();
        pdvRepository.save(pdv);

        log.info("Caixa {} fechado no PDV {}. Saldo: R$ {}", salvo.getId(), pdv.getNumero(), saldoEsperado);

        return new Output(salvo.getId(), pdv.getId(), pdv.getNumero(), saldoEsperado);
    }

    public record Input(UUID pdvId, String observacao) {}

    public record Output(UUID caixaId, UUID pdvId, String pdvNumero, BigDecimal saldoFechamento) {}
}
