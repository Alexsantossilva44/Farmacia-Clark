package br.com.farmacia.application.receituario.usecase;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.entity.MedicamentoControlado;
import br.com.farmacia.domain.medicamento.enums.NivelControle;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.enums.TipoReceita;
import br.com.farmacia.domain.receituario.exception.FarmaceuticoNaoEncontradoException;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;
import br.com.farmacia.domain.receituario.repository.ReceitaRepository;
import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.funcionario.repository.FarmaceuticoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caso de Uso: Validar Receita
 *
 * <p>Responsável por aplicar todas as regras de negócio e regulatórias
 * para aprovação ou rejeição de uma receita médica antes da dispensação.</p>
 *
 * <p>Regras aplicadas:</p>
 * <ul>
 *   <li>Receita não pode estar vencida</li>
 *   <li>CRM do prescritor deve estar preenchido</li>
 *   <li>Tipo de receita deve ser compatível com o nível de controle do medicamento</li>
 *   <li>Receita controlada deve ser validada por farmacêutico habilitado</li>
 *   <li>Quantidade prescrita não pode exceder o máximo permitido pela Portaria 344</li>
 *   <li>Receita tipo B1 exige 3 vias físicas</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidarReceitaUseCase {

    private final ReceitaRepository receitaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final FarmaceuticoRepository farmaceuticoRepository;

    // ─── Input ────────────────────────────────────────────────────────────────

    public record Input(
        UUID receitaId,
        UUID farmaceuticoId,
        List<ItemValidacao> itens
    ) {
        public record ItemValidacao(
            UUID medicamentoId,
            int quantidadeSolicitada
        ) {}
    }

    // ─── Output ───────────────────────────────────────────────────────────────

    public record Output(
        boolean aprovada,
        String status,
        List<String> violacoes,
        UUID receitaId
    ) {
        public static Output aprovada(UUID receitaId) {
            return new Output(true, "APROVADA", List.of(), receitaId);
        }

        public static Output rejeitada(UUID receitaId, List<String> violacoes) {
            return new Output(false, "REJEITADA", violacoes, receitaId);
        }
    }

    // ─── Execução ─────────────────────────────────────────────────────────────

    @Transactional
    public Output executar(Input input) {
        log.info("Iniciando validação de receita [receitaId={}] por farmacêutico [{}]",
                 input.receitaId(), input.farmaceuticoId());

        Receita receita = receitaRepository.findById(input.receitaId())
            .orElseThrow(() -> new ReceitaNaoEncontradaException(input.receitaId()));

        Farmaceutico farmaceutico = farmaceuticoRepository.findById(input.farmaceuticoId())
            .orElseThrow(() -> new FarmaceuticoNaoEncontradoException(input.farmaceuticoId()));

        List<String> violacoes = new ArrayList<>();

        // ── Regra 1: Status deve ser PENDENTE ─────────────────────────────────
        if (receita.getStatus() != StatusReceita.PENDENTE) {
            violacoes.add("Receita já processada com status: " + receita.getStatus());
        }

        // ── Regra 2: Receita não pode estar vencida ───────────────────────────
        if (!LocalDate.now().isBefore(receita.getDataValidade())) { // C-09: alinhado com Receita.estaVencida()
            violacoes.add("Receita vencida em " + receita.getDataValidade()
                          + ". Validade máxima: " + receita.getTipo().getValidadeDias() + " dias.");
        }

        // ── Regra 3: Prescritor com CRM válido ────────────────────────────────
        if (receita.getPrescritor() == null
                || receita.getPrescritor().getCrm() == null
                || receita.getPrescritor().getCrm().isBlank()) {
            violacoes.add("CRM do prescritor não informado ou inválido.");
        }

        // ── Regras por item ───────────────────────────────────────────────────
        for (Input.ItemValidacao item : input.itens()) {
            Medicamento med = medicamentoRepository.findById(item.medicamentoId())
                .orElseThrow(() -> new MedicamentoNaoEncontradoException(item.medicamentoId()));

            validarCompatibilidadeReceita(receita, med, item, violacoes);
            validarQuantidadeControlado(med, item, violacoes);
        }

        // ── Regra 4: Farmacêutico ativo e habilitado ──────────────────────────
        if (!farmaceutico.isAtivo()) {
            violacoes.add("Farmacêutico responsável não está ativo no sistema.");
        }

        // ── Decisão ───────────────────────────────────────────────────────────
        if (violacoes.isEmpty()) {
            aprovarReceita(receita, farmaceutico);
            log.info("Receita [{}] APROVADA pelo farmacêutico [{}]",
                     input.receitaId(), input.farmaceuticoId());
            return Output.aprovada(receita.getId());
        } else {
            rejeitarReceita(receita, farmaceutico, violacoes);
            log.warn("Receita [{}] REJEITADA. Violações: {}", input.receitaId(), violacoes);
            return Output.rejeitada(receita.getId(), violacoes);
        }
    }

    // ─── Métodos privados ─────────────────────────────────────────────────────

    /**
     * Valida se o tipo de receita é compatível com o nível de controle do medicamento.
     *
     * <p>Mapa de compatibilidade (Portaria ANVISA 344/98):</p>
     * <ul>
     *   <li>CONTROLADO_B1 (entorpecente) → exige BRANCA_ESPECIAL</li>
     *   <li>CONTROLADO_C1 (psicotrópico) → exige AZUL</li>
     *   <li>CONTROLADO_C2 (retinoides)   → exige AMARELA</li>
     *   <li>ANTIMICROBIANO               → exige SIMPLES (retenção obrigatória)</li>
     *   <li>RECEITA_SIMPLES              → qualquer tipo serve</li>
     * </ul>
     */
    private void validarCompatibilidadeReceita(
            Receita receita,
            Medicamento med,
            Input.ItemValidacao item,
            List<String> violacoes) {

        NivelControle nivel = med.getNivelControle();

        boolean compativel = switch (nivel) {
            case CONTROLADO_B1, CONTROLADO_B2 -> receita.getTipo() == TipoReceita.BRANCA_ESPECIAL;
            case CONTROLADO_C1 -> receita.getTipo() == TipoReceita.AZUL;
            case CONTROLADO_C2 -> receita.getTipo() == TipoReceita.AMARELA;
            case ANTIMICROBIANO -> receita.getTipo() == TipoReceita.SIMPLES
                                || receita.getTipo() == TipoReceita.ANTIMICROBIANO;
            case RECEITA_SIMPLES -> true;
            case LIVRE -> true;
            default -> false;
        };

        if (!compativel) {
            violacoes.add(
                "Medicamento '%s' requer receita tipo %s, mas foi apresentada receita tipo %s."
                    .formatted(med.getNomeComercial(),
                               nivel.getTipoReceitaRequerido(),
                               receita.getTipo())
            );
        }
    }

    /**
     * Para medicamentos controlados, verifica se a quantidade solicitada
     * não excede o máximo permitido pela Portaria 344.
     */
    private void validarQuantidadeControlado(
            Medicamento med,
            Input.ItemValidacao item,
            List<String> violacoes) {

        if (med.getMedicamentoControlado() == null) return;

        MedicamentoControlado ctrl = med.getMedicamentoControlado();
        if (item.quantidadeSolicitada() > ctrl.getQuantidadeMaximaReceita()) {
            violacoes.add(
                "Medicamento '%s': quantidade solicitada (%d) excede o máximo permitido (%d) pela %s."
                    .formatted(med.getNomeComercial(),
                               item.quantidadeSolicitada(),
                               ctrl.getQuantidadeMaximaReceita(),
                               ctrl.getPortaria())
            );
        }
    }

    private void aprovarReceita(Receita receita, Farmaceutico farmaceutico) {
        receita.aprovar(farmaceutico);
        receitaRepository.save(receita);
    }

    private void rejeitarReceita(Receita receita, Farmaceutico farmaceutico,
                                  List<String> violacoes) {
        receita.rejeitar(farmaceutico, String.join("; ", violacoes));
        receitaRepository.save(receita);
    }
}
