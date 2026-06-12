package br.com.farmacia.api.exceptionhandler;

import br.com.farmacia.domain.cliente.exception.ClienteDadosInvalidosException;
import br.com.farmacia.domain.cliente.exception.ClienteNaoEncontradoException;
import br.com.farmacia.domain.cliente.exception.CpfClienteDuplicadoException;
import br.com.farmacia.domain.cliente.exception.EmailClienteDuplicadoException;
import br.com.farmacia.domain.cliente.exception.TelefoneClienteDuplicadoException;
import br.com.farmacia.domain.compra.exception.ChaveDuplicadaException;
import br.com.farmacia.domain.compra.exception.ChaveInvalidaException;
import br.com.farmacia.domain.compra.exception.CnpjDuplicadoException;
import br.com.farmacia.domain.compra.exception.CnpjInvalidoException;
import br.com.farmacia.domain.compra.exception.FornecedorNaoEncontradoException;
import br.com.farmacia.domain.compra.exception.ItensObrigatoriosException;
import br.com.farmacia.domain.compra.exception.NotaInvalidaException;
import br.com.farmacia.domain.compra.exception.PedidoFornecedorIncompativelException;
import br.com.farmacia.domain.compra.exception.PedidoInvalidoException;
import br.com.farmacia.domain.compra.exception.PedidoNaoEncontradoException;
import br.com.farmacia.domain.compra.exception.StatusInvalidoException;
import br.com.farmacia.domain.estoque.exception.EstoqueInsuficienteException;
import br.com.farmacia.domain.estoque.exception.ItemEstoqueNaoEncontradoException;
import br.com.farmacia.domain.estoque.exception.LoteMedicamentoIncompativelException;
import br.com.farmacia.domain.estoque.exception.LoteNaoEncontradoException;
import br.com.farmacia.domain.estoque.exception.LoteVencidoException;
import br.com.farmacia.domain.estoque.exception.MotivoInvalidoException;
import br.com.farmacia.domain.estoque.exception.MotivoObrigatorioException;
import br.com.farmacia.domain.estoque.exception.ParametroInvalidoException;
import br.com.farmacia.domain.estoque.exception.QuantidadeInvalidaException;
import br.com.farmacia.domain.estoque.exception.SaldoConsolidadoInsuficienteException;
import br.com.farmacia.domain.estoque.exception.SaldoLoteInsuficienteException;
import br.com.farmacia.domain.estoque.exception.TipoAjusteInvalidoException;
import br.com.farmacia.domain.financeiro.exception.CaixaAbertoNaoEncontradoException;
import br.com.farmacia.domain.financeiro.exception.CaixaFechadoException;
import br.com.farmacia.domain.financeiro.exception.CaixaJaAbertoException;
import br.com.farmacia.domain.financeiro.exception.CaixaNaoEstaAbertoException;
import br.com.farmacia.domain.financeiro.exception.FuncionarioInvalidoException;
import br.com.farmacia.domain.financeiro.exception.PdvIndisponivelException;
import br.com.farmacia.domain.financeiro.exception.PdvNaoEncontradoException;
import br.com.farmacia.domain.medicamento.exception.MedicamentoDuplicadoException;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.exception.PrecoAcimaPMCException;
import br.com.farmacia.domain.receituario.exception.FarmaceuticoNaoEncontradoException;
import br.com.farmacia.domain.receituario.exception.FarmaceuticoNaoVinculadoException;
import br.com.farmacia.domain.receituario.exception.NumeroReceitaDuplicadoException;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoAprovadaException;
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;
import br.com.farmacia.domain.receituario.exception.ReceitaObrigatoriaException;
import br.com.farmacia.domain.receituario.exception.ReceitaPorNumeroNaoEncontradaException;
import br.com.farmacia.domain.venda.exception.CpfObrigatorioException;
import br.com.farmacia.domain.venda.exception.PagamentoInsuficienteException;
import br.com.farmacia.domain.venda.exception.VendaNaoEncontradaException;
import br.com.farmacia.domain.venda.exception.VendaNaoPodeCancelarException;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tratamento global de exceções seguindo RFC 7807 — Problem Details for HTTP APIs.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String MSG_ERRO_GENERICA_USUARIO =
        "Ocorreu um erro interno no sistema. Tente novamente ou contate o suporte.";

    private final MessageSource messageSource;

    public ApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        return handleValidationInternal(ex, ex.getBindingResult(), headers,
                                        HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    // ─── Venda ────────────────────────────────────────────────────────────────

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<Problem> handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/estoque-insuficiente",
            "Estoque insuficiente", ex.getUserMessage());
    }

    @ExceptionHandler(PrecoAcimaPMCException.class)
    public ResponseEntity<Problem> handlePrecoAcimaPMC(PrecoAcimaPMCException ex) {
        return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/preco-acima-pmc",
            "Preço acima do PMC",
            "O preço informado excede o Preço Máximo ao Consumidor (PMC) definido pela ANVISA.");
    }

    @ExceptionHandler(ReceitaObrigatoriaException.class)
    public ResponseEntity<Problem> handleReceitaObrigatoria(ReceitaObrigatoriaException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type("https://farmacia.com.br/receita-obrigatoria")
                .title("Receita obrigatória")
                .detail(ex.getMessage())
                .userMessage("Este medicamento exige receita médica para ser dispensado.")
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    @ExceptionHandler(CaixaFechadoException.class)
    public ResponseEntity<Problem> handleCaixaFechado(CaixaFechadoException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/caixa-fechado",
            "Caixa não está aberto",
            "O caixa do PDV selecionado não está aberto. Realize a abertura antes de iniciar uma venda.");
    }

    @ExceptionHandler(PagamentoInsuficienteException.class)
    public ResponseEntity<Problem> handlePagamentoInsuficiente(PagamentoInsuficienteException ex) {
        return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/pagamento-insuficiente",
            "Pagamento insuficiente", "O valor total dos pagamentos não cobre o total da venda.");
    }

    @ExceptionHandler(CpfObrigatorioException.class)
    public ResponseEntity<Problem> handleCpfObrigatorio(CpfObrigatorioException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type("https://farmacia.com.br/cpf-obrigatorio")
                .title("CPF obrigatório")
                .detail(ex.getMessage())
                .userMessage("O CPF do comprador é obrigatório para a dispensação deste tipo de medicamento.")
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    @ExceptionHandler(VendaNaoEncontradaException.class)
    public ResponseEntity<Problem> handleVendaNaoEncontrada(VendaNaoEncontradaException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/venda-nao-encontrada",
            "Venda não encontrada", "A venda informada não foi localizada.");
    }

    @ExceptionHandler(VendaNaoPodeCancelarException.class)
    public ResponseEntity<Problem> handleVendaNaoPodeCancelar(VendaNaoPodeCancelarException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/venda-nao-cancelavel",
            "Venda não pode ser cancelada", ex.getMessage());
    }

    // ─── PDV / Caixa ──────────────────────────────────────────────────────────

    @ExceptionHandler(PdvNaoEncontradoException.class)
    public ResponseEntity<Problem> handlePdvNaoEncontrado(PdvNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/pdv-nao-encontrado",
            "PDV não encontrado", "O ponto de venda informado não foi localizado.");
    }

    @ExceptionHandler(PdvIndisponivelException.class)
    public ResponseEntity<Problem> handlePdvIndisponivel(PdvIndisponivelException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/pdv-indisponivel",
            "PDV indisponível", ex.getMessage());
    }

    @ExceptionHandler(CaixaJaAbertoException.class)
    public ResponseEntity<Problem> handleCaixaJaAberto(CaixaJaAbertoException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/caixa-ja-aberto",
            "Caixa já aberto", "Já existe um caixa aberto neste PDV.");
    }

    @ExceptionHandler(CaixaAbertoNaoEncontradoException.class)
    public ResponseEntity<Problem> handleCaixaAbertoNaoEncontrado(CaixaAbertoNaoEncontradoException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/caixa-fechado",
            "Caixa fechado", "Não há caixa aberto para fechamento neste PDV.");
    }

    @ExceptionHandler(CaixaNaoEstaAbertoException.class)
    public ResponseEntity<Problem> handleCaixaNaoEstaAberto(CaixaNaoEstaAbertoException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/caixa-fechado",
            "Caixa fechado", ex.getMessage());
    }

    @ExceptionHandler(FuncionarioInvalidoException.class)
    public ResponseEntity<Problem> handleFuncionarioInvalido(FuncionarioInvalidoException ex) {
        return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/funcionario-invalido",
            "Funcionário inválido", ex.getMessage());
    }

    // ─── Medicamento ────────────────────────────────────────────────────────────

    @ExceptionHandler(MedicamentoNaoEncontradoException.class)
    public ResponseEntity<Problem> handleMedicamentoNaoEncontrado(MedicamentoNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/medicamento-nao-encontrado",
            "Medicamento não encontrado", "O medicamento informado não foi localizado no sistema.");
    }

    @ExceptionHandler(MedicamentoDuplicadoException.class)
    public ResponseEntity<Problem> handleMedicamentoDuplicado(MedicamentoDuplicadoException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/medicamento-duplicado",
            "Medicamento duplicado", ex.getMessage());
    }

    // ─── Receituário ────────────────────────────────────────────────────────────

    @ExceptionHandler(ReceitaNaoEncontradaException.class)
    public ResponseEntity<Problem> handleReceitaNaoEncontrada(ReceitaNaoEncontradaException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/receita-nao-encontrada",
            "Receita não encontrada", "A receita informada não foi localizada no sistema.");
    }

    @ExceptionHandler(ReceitaPorNumeroNaoEncontradaException.class)
    public ResponseEntity<Problem> handleReceitaPorNumeroNaoEncontrada(
            ReceitaPorNumeroNaoEncontradaException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/receita-nao-encontrada",
            "Receita não encontrada", "A receita informada não foi localizada no sistema.");
    }

    @ExceptionHandler(ReceitaNaoAprovadaException.class)
    public ResponseEntity<Problem> handleReceitaNaoAprovada(ReceitaNaoAprovadaException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/receita-nao-aprovada",
            "Receita não aprovada",
            "A receita ainda não foi validada por um farmacêutico ou foi rejeitada.");
    }

    @ExceptionHandler(NumeroReceitaDuplicadoException.class)
    public ResponseEntity<Problem> handleNumeroReceitaDuplicado(NumeroReceitaDuplicadoException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/receita-duplicada",
            "Receita duplicada", "Já existe receita cadastrada com este número.");
    }

    @ExceptionHandler(FarmaceuticoNaoEncontradoException.class)
    public ResponseEntity<Problem> handleFarmaceuticoNaoEncontrado(FarmaceuticoNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/farmaceutico-nao-encontrado",
            "Farmacêutico não encontrado", ex.getMessage());
    }

    @ExceptionHandler(FarmaceuticoNaoVinculadoException.class)
    public ResponseEntity<Problem> handleFarmaceuticoNaoVinculado(FarmaceuticoNaoVinculadoException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type("https://farmacia.com.br/farmaceutico-nao-vinculado")
                .title("Farmacêutico não habilitado")
                .detail(ex.getMessage())
                .userMessage("Seu usuário não possui registro de farmacêutico para validar receitas.")
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    // ─── Estoque ────────────────────────────────────────────────────────────────

    @ExceptionHandler(ItemEstoqueNaoEncontradoException.class)
    public ResponseEntity<Problem> handleItemEstoqueNaoEncontrado(ItemEstoqueNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/estoque-nao-encontrado",
            "Estoque não encontrado", "Não há registro de estoque para este medicamento.");
    }

    @ExceptionHandler(LoteNaoEncontradoException.class)
    public ResponseEntity<Problem> handleLoteNaoEncontrado(LoteNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/lote-nao-encontrado",
            "Lote não encontrado", "O lote informado não foi localizado.");
    }

    @ExceptionHandler({
        QuantidadeInvalidaException.class,
        LoteVencidoException.class,
        ParametroInvalidoException.class,
        MotivoObrigatorioException.class,
        MotivoInvalidoException.class,
        TipoAjusteInvalidoException.class,
        LoteMedicamentoIncompativelException.class
    })
    public ResponseEntity<Problem> handleRegraEstoqueInvalida(RuntimeException ex) {
        return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/estoque-invalido",
            "Operação de estoque inválida", ex.getMessage());
    }

    @ExceptionHandler({
        SaldoLoteInsuficienteException.class,
        SaldoConsolidadoInsuficienteException.class
    })
    public ResponseEntity<Problem> handleSaldoAjusteInsuficiente(RuntimeException ex) {
        return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/estoque-insuficiente",
            "Saldo insuficiente", ex.getMessage());
    }

    // ─── Compras ────────────────────────────────────────────────────────────────

    @ExceptionHandler({
        ChaveInvalidaException.class,
        ItensObrigatoriosException.class,
        NotaInvalidaException.class,
        PedidoFornecedorIncompativelException.class,
        CnpjInvalidoException.class,
        StatusInvalidoException.class,
        PedidoInvalidoException.class
    })
    public ResponseEntity<Problem> handleCompraInvalida(RuntimeException ex) {
        return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/compra-invalida",
            "Dados de compra inválidos", ex.getMessage());
    }

    @ExceptionHandler(FornecedorNaoEncontradoException.class)
    public ResponseEntity<Problem> handleFornecedorCompraNaoEncontrado(FornecedorNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/fornecedor-nao-encontrado",
            "Fornecedor não encontrado", "O fornecedor informado não foi localizado.");
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<Problem> handlePedidoNaoEncontrado(PedidoNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/pedido-nao-encontrado",
            "Pedido não encontrado", "O pedido de compra informado não foi localizado.");
    }

    @ExceptionHandler(ChaveDuplicadaException.class)
    public ResponseEntity<Problem> handleChaveNfeDuplicada(ChaveDuplicadaException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/nfe-duplicada",
            "NF-e já registrada", ex.getMessage());
    }

    @ExceptionHandler(CnpjDuplicadoException.class)
    public ResponseEntity<Problem> handleCnpjFornecedorDuplicado(CnpjDuplicadoException ex) {
        return conflict(ex.getMessage(), "https://farmacia.com.br/cnpj-duplicado",
            "CNPJ já cadastrado", ex.getMessage());
    }

    // ─── Cliente ──────────────────────────────────────────────────────────────

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ResponseEntity<Problem> handleClienteNaoEncontrado(ClienteNaoEncontradoException ex) {
        return notFound(ex.getMessage(), "https://farmacia.com.br/cliente-nao-encontrado",
            "Cliente não encontrado", "O cliente informado não foi localizado.");
    }

    @ExceptionHandler(CpfClienteDuplicadoException.class)
    public ResponseEntity<Problem> handleCpfDuplicado(CpfClienteDuplicadoException ex) {
        return conflict(
            "CPF já está em uso.",
            "https://farmacia.com.br/cpf-duplicado",
            "CPF já cadastrado",
            "Já existe um cliente com este CPF.");
    }

    @ExceptionHandler(TelefoneClienteDuplicadoException.class)
    public ResponseEntity<Problem> handleTelefoneDuplicado(TelefoneClienteDuplicadoException ex) {
        return conflict(
            "Telefone já está em uso.",
            "https://farmacia.com.br/telefone-duplicado",
            "Telefone já cadastrado",
            "Já existe um cliente com este telefone.");
    }

    @ExceptionHandler(EmailClienteDuplicadoException.class)
    public ResponseEntity<Problem> handleEmailDuplicado(EmailClienteDuplicadoException ex) {
        return conflict(
            "E-mail já está em uso.",
            "https://farmacia.com.br/email-duplicado",
            "E-mail já cadastrado",
            "Já existe um cliente com este e-mail.");
    }

    /**
     * Fallback quando a validação em memória perde a corrida e o UNIQUE (V7) barra o INSERT/UPDATE.
     * detail/userMessage genéricos — sem ecoar PII do constraint do PostgreSQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Problem> handleDataIntegrity(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage()
            : "";
        String causeLower = cause != null ? cause.toLowerCase() : "";

        if (causeLower.contains("uk_clientes_telefone")) {
            return conflict(
                "Telefone já está em uso.",
                "https://farmacia.com.br/telefone-duplicado",
                "Telefone já cadastrado",
                "Já existe um cliente com este telefone.");
        }
        if (causeLower.contains("uk_clientes_email")) {
            return conflict(
                "E-mail já está em uso.",
                "https://farmacia.com.br/email-duplicado",
                "E-mail já cadastrado",
                "Já existe um cliente com este e-mail.");
        }
        if (causeLower.contains("clientes") && causeLower.contains("cpf")) {
            return conflict(
                "CPF já está em uso.",
                "https://farmacia.com.br/cpf-duplicado",
                "CPF já cadastrado",
                "Já existe um cliente com este CPF.");
        }

        log.warn("Violação de integridade não mapeada: {}", cause);
        return conflict(
            "Registro duplicado.",
            "https://farmacia.com.br/conflito-dados",
            "Conflito de dados",
            "Não foi possível salvar — registro duplicado ou referência inválida.");
    }

    @ExceptionHandler(ClienteDadosInvalidosException.class)
    public ResponseEntity<Problem> handleClienteDadosInvalidos(ClienteDadosInvalidosException ex) {
        return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/cliente-dados-invalidos",
            "Dados do cliente inválidos", ex.getMessage());
    }

    // ─── Segurança ──────────────────────────────────────────────────────────────

    @ExceptionHandler(br.com.farmacia.infrastructure.security.TokenService.CredenciaisInvalidasException.class)
    public ResponseEntity<Problem> handleCredenciaisInvalidas(
            br.com.farmacia.infrastructure.security.TokenService.CredenciaisInvalidasException ex) {

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type("https://farmacia.com.br/credenciais-invalidas")
                .title("Credenciais inválidas")
                .detail(ex.getMessage())
                .userMessage("E-mail ou senha incorretos. Verifique e tente novamente.")
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Problem> handleAccessDenied(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type("https://farmacia.com.br/acesso-negado")
                .title("Acesso negado")
                .detail("Acesso negado") // H-10/H-14: mensagem interna do Spring não exposta ao cliente
                .userMessage("Você não tem permissão para executar esta operação.")
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUncaught(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type("https://farmacia.com.br/erro-interno")
                .title("Erro interno do sistema")
                .detail(MSG_ERRO_GENERICA_USUARIO)
                .userMessage(MSG_ERRO_GENERICA_USUARIO)
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    private ResponseEntity<Object> handleValidationInternal(
            Exception ex,
            BindingResult bindingResult,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {

        List<Problem.Field> fields = bindingResult.getAllErrors().stream()
            .map(error -> {
                String name = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
                String message = messageSource.getMessage(error, LocaleContextHolder.getLocale());
                return Problem.Field.builder().name(name).userMessage(message).build();
            })
            .collect(Collectors.toList());

        Problem problem = Problem.builder()
            .status(status.value())
            .type("https://farmacia.com.br/dados-invalidos")
            .title("Dados inválidos")
            .detail("Um ou mais campos estão inválidos. Corrija e tente novamente.")
            .userMessage("Um ou mais campos estão inválidos. Preencha corretamente e tente novamente.")
            .timestamp(OffsetDateTime.now())
            .fields(fields)
            .build();

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private ResponseEntity<Problem> notFound(String detail, String type, String title, String userMessage) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type(type)
                .title(title)
                .detail(detail)
                .userMessage(userMessage)
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    private ResponseEntity<Problem> conflict(String detail, String type, String title, String userMessage) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type(type)
                .title(title)
                .detail(detail)
                .userMessage(userMessage)
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    private ResponseEntity<Problem> unprocessableEntity(String detail, String type, String title, String userMessage) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(
            Problem.builder()
                .status(status.value())
                .type(type)
                .title(title)
                .detail(detail)
                .userMessage(userMessage)
                .timestamp(OffsetDateTime.now())
                .build()
        );
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Problem {

        private Integer status;
        private String type;
        private String title;
        private String detail;
        private String userMessage;
        private OffsetDateTime timestamp;
        private List<Field> fields;

        @Getter
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Field {
            private String name;
            private String userMessage;
        }
    }
}
