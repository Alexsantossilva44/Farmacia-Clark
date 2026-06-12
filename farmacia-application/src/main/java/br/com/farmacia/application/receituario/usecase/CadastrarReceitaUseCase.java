package br.com.farmacia.application.receituario.usecase;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.exception.ClienteNaoEncontradoException;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import br.com.farmacia.domain.receituario.entity.Prescritor;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.enums.TipoReceita;
import br.com.farmacia.domain.receituario.exception.NumeroReceitaDuplicadoException;
import br.com.farmacia.domain.receituario.exception.PrescritorNaoEncontradoException;
import br.com.farmacia.domain.receituario.repository.PrescritorRepository;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cadastra receita médica com status PENDENTE para validação farmacêutica.
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CadastrarReceitaUseCase {

    private final ReceitaRepository receitaRepository;
    private final ClienteRepository clienteRepository;
    private final PrescritorRepository prescritorRepository;

    @Transactional
    public Receita executar(Input input) {
        if (receitaRepository.findByNumeroReceita(input.numeroReceita()).isPresent()) {
            throw new NumeroReceitaDuplicadoException(input.numeroReceita());
        }

        Prescritor prescritor = prescritorRepository.findById(input.prescritorId())
            .orElseThrow(() -> new PrescritorNaoEncontradoException(input.prescritorId()));

        Cliente cliente = null;
        if (input.clienteId() != null) {
            cliente = clienteRepository.findById(input.clienteId())
                .orElseThrow(() -> new ClienteNaoEncontradoException(input.clienteId()));
        }

        LocalDate emissao = input.dataEmissao() != null ? input.dataEmissao() : LocalDate.now();
        LocalDate validade = emissao.plusDays(input.tipo().getValidadeDias());

        Receita receita = Receita.builder()
            .numeroReceita(input.numeroReceita())
            .tipo(input.tipo())
            .dataEmissao(emissao)
            .dataValidade(validade)
            .status(StatusReceita.PENDENTE)
            .cid(input.cid())
            .prescritor(prescritor)
            .cliente(cliente)
            .retida(input.tipo() == TipoReceita.AZUL
                || input.tipo() == TipoReceita.AMARELA
                || input.tipo() == TipoReceita.BRANCA_ESPECIAL)
            .build();

        Receita salva = receitaRepository.save(receita);
        log.info("Receita [{}] cadastrada — tipo {}", salva.getNumeroReceita(), salva.getTipo());
        return salva;
    }

    public record Input(
        String numeroReceita,
        TipoReceita tipo,
        LocalDate dataEmissao,
        UUID prescritorId,
        UUID clienteId,
        String cid
    ) {}
}
