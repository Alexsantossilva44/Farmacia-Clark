package br.com.farmacia.application.financeiro.usecase;

import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.enums.StatusCaixa;
import br.com.farmacia.domain.financeiro.enums.StatusPDV;
import br.com.farmacia.domain.financeiro.exception.CaixaJaAbertoException;
import org.springframework.dao.DataIntegrityViolationException;
import br.com.farmacia.domain.financeiro.exception.FuncionarioInvalidoException;
import br.com.farmacia.domain.financeiro.exception.PdvIndisponivelException;
import br.com.farmacia.domain.financeiro.exception.PdvNaoEncontradoException;
import br.com.farmacia.domain.financeiro.repository.CaixaRepository;
import br.com.farmacia.domain.financeiro.repository.PdvRepository;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.funcionario.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caso de uso: abrir caixa em um PDV para início de turno.
 *
 * <p>Regras:</p>
 * <ul>
 *   <li>O PDV deve existir e não estar bloqueado/manutenção.</li>
 *   <li>Não pode haver outro caixa aberto no mesmo PDV.</li>
 *   <li>O funcionário responsável deve estar ativo.</li>
 * </ul>
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbrirCaixaUseCase {

    private final PdvRepository pdvRepository;
    private final CaixaRepository caixaRepository;
    private final FuncionarioRepository funcionarioRepository;

    @Transactional
    public Output executar(Input input) {
        PDV pdv = pdvRepository.findById(input.pdvId())
            .orElseThrow(() -> new PdvNaoEncontradoException(input.pdvId()));

        if (pdv.getStatus() == StatusPDV.BLOQUEADO || pdv.getStatus() == StatusPDV.MANUTENCAO) {
            throw new PdvIndisponivelException(pdv.getNumero());
        }

        caixaRepository.findCaixaAbertoPorPdv(pdv.getId()).ifPresent(c -> {
            throw new CaixaJaAbertoException(pdv.getNumero());
        });

        Funcionario funcionario = funcionarioRepository.findById(input.funcionarioId())
            .filter(f -> Boolean.TRUE.equals(f.getAtivo()))
            .orElseThrow(() -> new FuncionarioInvalidoException(input.funcionarioId()));

        Caixa caixa = Caixa.builder()
            .pdv(pdv)
            .funcionario(funcionario)
            .abertura(LocalDateTime.now())
            .saldoAbertura(input.saldoAbertura() != null ? input.saldoAbertura() : BigDecimal.ZERO)
            .status(StatusCaixa.ABERTO)
            .build();

        // C-04: proteção no nível do banco contra dupla abertura em requisições concorrentes
        Caixa salvo;
        try {
            salvo = caixaRepository.save(caixa);
        } catch (DataIntegrityViolationException ex) {
            throw new CaixaJaAbertoException(pdv.getNumero()); // constraint unique disparada por race condition
        }

        pdv.abrir();
        pdvRepository.save(pdv);

        log.info("Caixa {} aberto no PDV {} por {}", salvo.getId(), pdv.getNumero(), funcionario.getEmail());

        return new Output(salvo.getId(), pdv.getId(), pdv.getNumero());
    }

    public record Input(UUID pdvId, UUID funcionarioId, BigDecimal saldoAbertura) {
    }

    public record Output(UUID caixaId, UUID pdvId, String pdvNumero) {
    }
}
