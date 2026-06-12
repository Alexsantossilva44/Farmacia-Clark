# mudanca_01 — Refatoração DDD: Entidades, Exceções e Desacoplamento de Camadas

> **Objetivo deste documento:** registrar, de forma didática, todas as alterações feitas na refatoração arquitetural do projeto Farmacia. Use este material para estudar **QA**, **DDD (Domain-Driven Design)** e **testes de regressão**.

---

## Sumário

1. [Contexto e problema original](#1-contexto-e-problema-original)
2. [Visão geral da solução](#2-visão-geral-da-solução)
3. [Inventário completo de arquivos alterados](#3-inventário-completo-de-arquivos-alterados)
4. [Padrão 1 — Remoção de `@Setter` nas entidades](#4-padrão-1--remoção-de-setter-nas-entidades)
5. [Padrão 2 — Métodos de domínio (comportamento nas entidades)](#5-padrão-2--métodos-de-domínio-comportamento-nas-entidades)
6. [Padrão 3 — Exceções migradas para o domínio](#6-padrão-3--exceções-migradas-para-o-domínio)
7. [Padrão 4 — Use Cases sem inner classes](#7-padrão-4--use-cases-sem-inner-classes)
8. [Padrão 5 — ApiExceptionHandler desacoplado](#8-padrão-5--apiexceptionhandler-desacoplado)
9. [Padrão 6 — Controllers sem regra de negócio](#9-padrão-6--controllers-sem-regra-de-negócio)
10. [Padrão 7 — Infraestrutura com `atribuirId()`](#10-padrão-7--infraestrutura-com-atribuirid)
11. [Alterações nos testes (QA)](#11-alterações-nos-testes-qa)
12. [Matriz de testes manuais sugerida](#12-matriz-de-testes-manuais-sugerida)
13. [Como validar tecnicamente](#13-como-validar-tecnicamente)
14. [Glossário para estudo](#14-glossário-para-estudo)
15. [Correções pós-auditoria (mudanca_01.1)](#15-correções-pós-auditoria-mudanca_011)

---

## 1. Contexto e problema original

O projeto segue arquitetura **hexagonal / DDD** com módulos:

```
farmacia-domain      → regras de negócio puras
farmacia-application → casos de uso (orquestração)
farmacia-infrastructure → banco, mensageria, adapters
farmacia-api         → REST, controllers, exception handler
```

### Problemas identificados (antes da mudança)

| # | Problema | Impacto em QA |
|---|----------|---------------|
| 1 | **26 entidades** com `@Setter` público do Lombok | Qualquer camada podia alterar estado sem validação |
| 2 | **~52 exceções** como `static class` dentro de Use Cases | API acoplada à implementação; difícil testar isoladamente |
| 3 | Exceções de negócio dentro de **Controllers** | Regra de negócio na camada errada |
| 4 | Use Cases chamando `lote.setQuantidadeAtual()`, `receita.setStatus()` etc. | Regras espalhadas; regressão silenciosa |

### O que já estava correto (referência)

- `Medicamento.java` — `@Getter` only + métodos `atualizar()`, `inativar()`, `atribuirId()`
- `Venda.java` — `finalizar()`, `cancelar()`, `recalcularTotais()`
- `RealizarVendaUseCase` — já usava exceções de domínio para venda

**Esta refatoração estendeu esse padrão para todo o projeto.**

---

## 2. Visão geral da solução

### Três pilares aplicados

```
┌─────────────────────────────────────────────────────────────┐
│  ANTES                          DEPOIS                      │
├─────────────────────────────────────────────────────────────┤
│  lote.setQuantidadeAtual(n)  →  lote.consumir(n)            │
│  receita.setStatus(APROVADA) →  receita.aprovar(farmaceutico)│
│  UseCase.XxxException        →  domain.XxxException         │
│  entity.setId(uuid)          →  entity.atribuirId(uuid)   │
│  @Getter @Setter             →  @Getter + métodos domínio   │
└─────────────────────────────────────────────────────────────┘
```

### Fluxo de uma exceção de negócio (depois)

```
Use Case lança DomainException
        ↓
ApiExceptionHandler captura pelo tipo da classe
        ↓
Retorna HTTP Problem Details (RFC 7807) com status, title, userMessage
        ↓
Front-end exibe mensagem amigável ao usuário
```

---

## 3. Inventário completo de arquivos alterados

### 3.1 Módulo `farmacia-domain` — Entidades (26 arquivos)

Todas perderam `@Setter`. As que tinham mutação nos use cases ganharam métodos de domínio.

| Entidade | Arquivo | Métodos novos / relevantes |
|----------|---------|---------------------------|
| Lote | `estoque/entity/Lote.java` | `consumir`, `restaurar`, `incrementarSaldo`, `decrementarSaldo`, `registrarEntradaAdicional`, `bloquearPorVencimento`, `atribuirId` |
| Receita | `receituario/entity/Receita.java` | `marcarComoUtilizada`, `aprovar`, `rejeitar`, `atribuirId` |
| Caixa | `financeiro/entity/Caixa.java` | `fechar`, `atribuirId` |
| PDV | `financeiro/entity/PDV.java` | `abrir`, `fechar`, `atribuirId` |
| Cliente | `cliente/entity/Cliente.java` | `atualizar`, `atribuirId` |
| ItemEstoque | `estoque/entity/ItemEstoque.java` | `incrementarSaldo`, `atualizarLimites`, `atribuirId` |
| PedidoCompra | `compra/entity/PedidoCompra.java` | `confirmar`, `atualizarStatusRecebimento`, `atribuirId` |
| ItemPedidoCompra | `compra/entity/ItemPedidoCompra.java` | `vincularPedido`, `inicializarQuantidadeRecebida`, `registrarRecebimento`, `atribuirId` |
| NotaFiscalEntrada | `compra/entity/NotaFiscalEntrada.java` | `finalizarConferencia`, `atribuirId` |
| RegistroSNGPC | `receituario/entity/RegistroSNGPC.java` | `iniciarEnvio`, `confirmarEnvio`, `marcarErroDefinitivo`, `reagendarEnvio`, `atribuirId` |
| MedicamentoControlado | `medicamento/entity/MedicamentoControlado.java` | `vincularMedicamento`, `atribuirId` |
| Pagamento | `venda/entity/Pagamento.java` | `atribuirId` |
| ItemVenda | `venda/entity/ItemVenda.java` | `atribuirId` |
| Fornecedor | `compra/entity/Fornecedor.java` | `atribuirId` |
| Farmaceutico | `funcionario/entity/Farmaceutico.java` | `atribuirId` |
| Funcionario | `funcionario/entity/Funcionario.java` | `atribuirId` |
| Cargo | `funcionario/entity/Cargo.java` | `atribuirId` |
| Prescritor | `receituario/entity/Prescritor.java` | `atribuirId` |
| AlertaEstoque | `estoque/entity/AlertaEstoque.java` | `atribuirId` |
| MovimentacaoEstoque | `estoque/entity/MovimentacaoEstoque.java` | `atribuirId` |
| Fabricante | `medicamento/entity/Fabricante.java` | `atribuirId` |
| Categoria | `medicamento/entity/Categoria.java` | `atribuirId` |
| PrincipioAtivo | `medicamento/entity/PrincipioAtivo.java` | `atribuirId` |
| DivergenciaConferencia | `compra/entity/DivergenciaConferencia.java` | apenas removeu `@Setter` (sem campo `id`) |
| Medicamento | `medicamento/entity/Medicamento.java` | **sem alteração** (já era referência) |
| Venda | `venda/entity/Venda.java` | **sem alteração** (já era referência) |

### 3.2 Módulo `farmacia-domain` — Exceções (39 arquivos novos + 13 existentes)

**Base comum:**

```java
// farmacia-domain/.../shared/exception/DomainException.java
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
```

**Novas exceções criadas (39):**

| Pacote | Classes |
|--------|---------|
| `cliente/exception/` | `CpfClienteDuplicadoException`, `TelefoneClienteDuplicadoException`, `EmailClienteDuplicadoException` |
| `cliente/exception/` | `ClienteDadosInvalidosException` — **mudanca_01.1:** passa a estender `DomainException` |
| `medicamento/exception/` | `MedicamentoDuplicadoException` |
| `venda/exception/` | `VendaNaoEncontradaException`, `VendaNaoPodeCancelarException` |
| `estoque/exception/` | `LoteNaoEncontradoException`, `LoteMedicamentoIncompativelException`, `QuantidadeInvalidaException`, `MotivoObrigatorioException`, `MotivoInvalidoException`, `TipoAjusteInvalidoException`, `LoteVencidoException`, `SaldoLoteInsuficienteException`, `SaldoConsolidadoInsuficienteException`, `ItemEstoqueNaoEncontradoException`, `ParametroInvalidoException` |
| `compra/exception/` | `CnpjInvalidoException`, `CnpjDuplicadoException`, `FornecedorNaoEncontradoException`, `ItensObrigatoriosException`, `PedidoNaoEncontradoException`, `StatusInvalidoException`, `PedidoInvalidoException`, `ChaveDuplicadaException`, `ChaveInvalidaException`, `NotaInvalidaException`, `PedidoFornecedorIncompativelException` |
| `financeiro/exception/` | `PdvNaoEncontradoException`, `PdvIndisponivelException`, `CaixaJaAbertoException`, `FuncionarioInvalidoException`, `CaixaAbertoNaoEncontradoException`, `CaixaNaoEstaAbertoException` |
| `receituario/exception/` | `NumeroReceitaDuplicadoException`, `PrescritorNaoEncontradoException`, `ReceitaPorNumeroNaoEncontradaException`, `FarmaceuticoNaoEncontradoException`, `FarmaceuticoNaoVinculadoException` |

**Exemplo de exceção com dois construtores (busca por UUID ou número):**

```java
// PdvNaoEncontradoException.java
public class PdvNaoEncontradoException extends DomainException {
    public PdvNaoEncontradoException(UUID id) {
        super("PDV não encontrado: " + id);
    }
    public PdvNaoEncontradoException(String numero) {
        super("PDV não encontrado: " + numero);
    }
}
```

### 3.3 Módulo `farmacia-application` — Use Cases e Services (24 arquivos)

| Arquivo | O que mudou |
|---------|-------------|
| `venda/usecase/RealizarVendaUseCase.java` | `lote.consumir()`, `receita.marcarComoUtilizada()` |
| `venda/usecase/CancelarVendaUseCase.java` | `lote.restaurar()`, import `VendaNaoPodeCancelarException` |
| `venda/usecase/ConsultarVendaUseCase.java` | import `VendaNaoEncontradaException`, removeu inner class |
| `receituario/usecase/ValidarReceitaUseCase.java` | `receita.aprovar()`, `receita.rejeitar()`, imports domínio |
| `receituario/usecase/CadastrarReceitaUseCase.java` | imports domínio, removeu 3 inner classes |
| `receituario/usecase/ConsultarReceitaUseCase.java` | imports domínio |
| `financeiro/usecase/AbrirCaixaUseCase.java` | `pdv.abrir()`, imports domínio, removeu 4 inner classes |
| `financeiro/usecase/FecharCaixaUseCase.java` | `caixa.fechar()`, `pdv.fechar()`, imports domínio |
| `estoque/usecase/RegistrarAjusteSaldoUseCase.java` | `lote.incrementarSaldo/decrementarSaldo`, `item.incrementarSaldo`, removeu 10 inner classes |
| `estoque/usecase/RegistrarEntradaEstoqueUseCase.java` | `lote.registrarEntradaAdicional()`, removeu inner classes |
| `estoque/usecase/ConsultarEstoqueUseCase.java` | import `ItemEstoqueNaoEncontradoException` |
| `estoque/usecase/AtualizarItemEstoqueUseCase.java` | `item.atualizarLimites()` |
| `cliente/usecase/AtualizarClienteUseCase.java` | `cliente.atualizar()` |
| `cliente/usecase/CadastrarClienteUseCase.java` | import `CpfClienteDuplicadoException` |
| `cliente/usecase/ConsultarClienteUseCase.java` | import `ClienteNaoEncontradoException` |
| `cliente/ClienteContatoService.java` | imports `Telefone/EmailClienteDuplicadoException` |
| `medicamento/usecase/ConsultarMedicamentoUseCase.java` | import `MedicamentoNaoEncontradoException` |
| `medicamento/usecase/CadastrarMedicamentoUseCase.java` | import `MedicamentoDuplicadoException` |
| `compra/usecase/CadastrarFornecedorUseCase.java` | imports compra exceptions |
| `compra/usecase/CriarPedidoCompraUseCase.java` | imports domínio |
| `compra/usecase/ConfirmarPedidoCompraUseCase.java` | `pedido.confirmar()` |
| `compra/usecase/ConferirNotaComPedidoUseCase.java` | `itemPedido.registrarRecebimento()`, `pedido.atualizarStatusRecebimento()` |
| `compra/usecase/RegistrarNotaFiscalEntradaUseCase.java` | `nota.finalizarConferencia()` |
| `compra/usecase/ConsultarPedidoCompraUseCase.java` | import `PedidoNaoEncontradoException` |

### 3.4 Módulo `farmacia-api` (3 arquivos)

| Arquivo | O que mudou |
|---------|-------------|
| `exceptionhandler/ApiExceptionHandler.java` | Reescrito: imports só de `domain.*.exception`, handlers para todas as exceções |
| `v1/controller/PdvController.java` | Removeu `PdvNaoEncontradoException` interna; usa `domain.financeiro.exception` |
| `v1/controller/ReceitaController.java` | Removeu `FarmaceuticoNaoVinculadoException` interna; usa `domain.receituario.exception` |

### 3.5 Módulo `farmacia-infrastructure` (20 arquivos)

**17 Repository Adapters** — padrão `setId` → `atribuirId`:

- `ClienteRepositoryAdapter`
- `MedicamentoRepositoryAdapter` (+ `ctrl.vincularMedicamento()`)
- `EstoqueRepositoryAdapter`
- `VendaRepositoryAdapter`
- `PrescritorRepositoryAdapter`
- `FarmaceuticoRepositoryAdapter`
- `RegistroSNGPCRepositoryAdapter`
- `FornecedorRepositoryAdapter`
- `AlertaEstoqueRepositoryAdapter`
- `NotaFiscalEntradaRepositoryAdapter`
- `LoteRepositoryAdapter`
- `CaixaRepositoryAdapter`
- `ReceitaRepositoryAdapter`
- `PedidoCompraRepositoryAdapter` (+ `vincularPedido`, `inicializarQuantidadeRecebida`)
- `FuncionarioRepositoryAdapter`
- `PdvRepositoryAdapter`

**Outros arquivos de infra:**

| Arquivo | Mudança |
|---------|---------|
| `bootstrap/DevOperacionalSeed.java` | Seed usa `ItemEstoque.builder().id(...)` e `Lote.builder().id(...)` em vez de setters |
| `scheduler/AlertaVencimentoScheduler.java` | `lote.bloquearPorVencimento()` |
| `messaging/listener/RabbitSngpcEventPublisher.java` | `registro.iniciarEnvio()`, `confirmarEnvio()`, etc. |
| `persistence/estoque/EstoqueRepositoryAdapter.java` | **mudanca_01.1:** `decrementarSaldo()` lança `EstoqueInsuficienteException` em saldo negativo |

### 3.6 Módulo `farmacia-api` — Testes (5 arquivos)

| Arquivo | Mudança |
|---------|---------|
| `qa/seed/IntegracaoTestSeed.java` | Builder pattern para ItemEstoque e Lote |
| `qa/bdd/steps/AlertasEstoqueVencimentoSteps.java` | Usa seed helper em vez de setter |
| `qa/bdd/steps/VendaMedicamentoControladoSteps.java` | `Recepta.builder()` helper, seed para lote |
| `qa/unit/ValidarReceitaUseCaseTest.java` | Builder para status; imports exceções domínio |
| `qa/webmvc/MedicamentoControllerWebMvcTest.java` | Import `MedicamentoNaoEncontradoException` do domínio |

---

## 4. Padrão 1 — Remoção de `@Setter` nas entidades

### ANTES (problemático)

```java
@Getter
@Setter          // ← qualquer camada podia fazer lote.setQuantidadeAtual(0)
@Builder
public class Lote {
    private Integer quantidadeAtual;
    private StatusLote status;
}
```

### DEPOIS (correto em DDD)

```java
@Getter          // ← só leitura externa
@Builder
public class Lote {
    private Integer quantidadeAtual;
    private StatusLote status;

    // Mutação só por métodos com regra de negócio
    public void consumir(int quantidade) { ... }
}
```

### Por que isso importa para QA?

- **Antes:** um bug em qualquer camada podia zerar estoque sem passar por validação.
- **Depois:** toda alteração de saldo passa por `consumir()`, `restaurar()`, etc., que têm guards (`quantidade <= 0`, saldo insuficiente).
- **Como testar:** tentar caminhos que esgotam lote, cancelam venda, ajustam saldo — o comportamento deve ser **previsível e centralizado**.

---

## 5. Padrão 2 — Métodos de domínio (comportamento nas entidades)

### 5.1 Lote — ciclo de vida do estoque

**Arquivo:** `farmacia-domain/.../estoque/entity/Lote.java`

| Método | Quando é chamado | Regra embutida |
|--------|------------------|----------------|
| `consumir(qtd)` | Venda (`RealizarVendaUseCase`) | Valida qtd > 0 e saldo; se saldo = 0 → `StatusLote.ESGOTADO` |
| `restaurar(qtd)` | Cancelamento (`CancelarVendaUseCase`) | Devolve unidades; status volta para `ATIVO` |
| `incrementarSaldo(qtd)` | Ajuste positivo, entrada | Incrementa `quantidadeAtual` e `quantidadeRecebida` |
| `decrementarSaldo(qtd)` | Ajuste negativo | Valida saldo; esgota se chegar a 0 |
| `registrarEntradaAdicional(...)` | Entrada em lote existente | Vincula NF, atualiza validade/preco |
| `bloquearPorVencimento()` | Scheduler de vencimento | Zera saldo, status `VENCIDO`, retorna qtd bloqueada |
| `atribuirId(uuid)` | Persistência (adapter) | Só atribui id uma vez |

**Código — consumo na venda (use case):**

```java
// RealizarVendaUseCase.java (DEPOIS)
int saldoAnterior = lote.getQuantidadeAtual();
lote.consumir(qtdLote);                    // ← regra dentro da entidade
loteRepository.save(lote);
```

**Código — ANTES (problemático):**

```java
lote.setQuantidadeAtual(saldoAnterior - qtdLote);
if (lote.getQuantidadeAtual() == 0) {
    lote.setStatus(StatusLote.ESGOTADO);
}
```

**Cancelamento de venda:**

```java
// CancelarVendaUseCase.java (DEPOIS)
lote.restaurar(item.getQuantidade());
loteRepository.save(lote);
```

---

### 5.2 Receita — fluxo de validação e uso

**Arquivo:** `farmacia-domain/.../receituario/entity/Receita.java`

| Método | Quando | Regra |
|--------|--------|-------|
| `aprovar(farmaceutico)` | `ValidarReceitaUseCase` sem violações | Status `APROVADA`, registra farmacêutico e data; se tipo Azul/Amarela/Branca Especial → `retida = true` |
| `rejeitar(farmaceutico, motivo)` | Validação com violações | Status `REJEITADA`, grava motivo |
| `marcarComoUtilizada()` | Venda com receita | Só aceita se status = `APROVADA` → `UTILIZADA` |

**Código — validação (use case):**

```java
// ValidarReceitaUseCase.java (DEPOIS)
private void aprovarReceita(Receita receita, Farmaceutico farmaceutico) {
    receita.aprovar(farmaceutico);
    receitaRepository.save(receita);
}

private void rejeitarReceita(Receita receita, Farmaceutico farmaceutico, List<String> violacoes) {
    receita.rejeitar(farmaceutico, String.join("; ", violacoes));
    receitaRepository.save(receita);
}
```

**Código — venda utiliza receita:**

```java
// RealizarVendaUseCase.java (DEPOIS)
receita.marcarComoUtilizada();
receitaRepository.save(receita);
venda.associarReceita(receita);
```

**Cenários de QA para Receita:**

| Cenário | Resultado esperado |
|---------|-------------------|
| Validar receita pendente sem violações | Status `APROVADA`, retida se controlada |
| Validar receita já processada | Violação: "Receita já processada" |
| Validar receita vencida | Violação com data de validade |
| Vender com receita não aprovada | `ReceitaNaoAprovadaException` ou erro na finalização |
| Vender com receita aprovada | Status muda para `UTILIZADA` |

---

### 5.3 Caixa e PDV

**Caixa.fechar(observacao):**

```java
public void fechar(String observacaoFechamento) {
    if (status != StatusCaixa.ABERTO) {
        throw new IllegalStateException("Caixa não está aberto para fechamento");
    }
    this.fechamento = LocalDateTime.now();
    this.saldoFechamento = saldoEsperado();
    this.status = StatusCaixa.FECHADO;
    if (observacaoFechamento != null && !observacaoFechamento.isBlank()) {
        this.observacao = observacaoFechamento;
    }
}
```

**FecharCaixaUseCase (DEPOIS):**

```java
caixa.fechar(input.observacao());
pdv.fechar();
```

---

### 5.4 Cliente, ItemEstoque, Compras

**Cliente.atualizar(...)** — recebe campos já validados pelo use case (`ClienteValidacao`).

**ItemEstoque:**

```java
item.incrementarSaldo(quantidade);   // entrada / ajuste positivo
item.atualizarLimites(min, max);     // AtualizarItemEstoqueUseCase
```

**PedidoCompra:**

```java
pedido.confirmar();                          // só de RASCUNHO → CONFIRMADO
pedido.atualizarStatusRecebimento();         // após conferência NF
itemPedido.registrarRecebimento(quantidade);
nota.finalizarConferencia(qtdItens, status);
```

---

### 5.5 RegistroSNGPC — envio assíncrono

**RabbitSngpcEventPublisher (DEPOIS):**

```java
registro.iniciarEnvio();                              // tentativas++, ENVIADO, dataEnvio
registro.confirmarEnvio("SNGPC-" + timestamp);        // CONFIRMADO + protocolo
// em falha:
registro.marcarErroDefinitivo();   // após MAX_TENTATIVAS
registro.reagendarEnvio();         // volta PENDENTE para retry
```

---

## 6. Padrão 3 — Exceções migradas para o domínio

### ANTES — inner class no Use Case

```java
// Dentro de ValidarReceitaUseCase.java
public static class ReceitaNaoEncontradaException extends RuntimeException {
    public ReceitaNaoEncontradaException(UUID id) {
        super("Receita não encontrada: " + id);
    }
}
```

**Problemas:**
- API importava `ValidarReceitaUseCase.ReceitaNaoEncontradaException`
- Teste acoplado à classe de aplicação
- Mesma exceção duplicada em vários use cases

### DEPOIS — classe no domínio

```java
// farmacia-domain/.../receituario/exception/ReceitaNaoEncontradaException.java
public class ReceitaNaoEncontradaException extends DomainException {
    public ReceitaNaoEncontradaException(UUID id) {
        super("Receita não encontrada: " + id);
    }
}
```

**Uso no use case:**

```java
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;

receitaRepository.findById(input.receitaId())
    .orElseThrow(() -> new ReceitaNaoEncontradaException(input.receitaId()));
```

### Mapa exceção → HTTP (ApiExceptionHandler)

| Exceção | HTTP | type (URI) |
|---------|------|------------|
| `EstoqueInsuficienteException` | 409 Conflict | estoque-insuficiente |
| `ReceitaNaoEncontradaException` | 404 Not Found | receita-nao-encontrada |
| `CpfClienteDuplicadoException` | 409 Conflict | cpf-duplicado |
| `QuantidadeInvalidaException` | 422 Unprocessable | estoque-invalido |
| `FarmaceuticoNaoVinculadoException` | 403 Forbidden | farmaceutico-nao-vinculado |
| `PagamentoInsuficienteException` | 422 Unprocessable | pagamento-insuficiente |
| Exceção não mapeada | 500 Internal Server Error | erro-interno |

**Formato de resposta (RFC 7807):**

```json
{
  "status": 409,
  "type": "https://farmacia.com.br/estoque-insuficiente",
  "title": "Estoque insuficiente",
  "detail": "Medicamento X: solicitado 10, disponível 3",
  "userMessage": "Mensagem amigável para o usuário",
  "timestamp": "2026-06-01T12:00:00-03:00"
}
```

---

## 7. Padrão 4 — Use Cases sem inner classes

### Lista completa de inner classes REMOVIDAS

| Use Case | Inner classes removidas |
|----------|------------------------|
| `RegistrarAjusteSaldoUseCase` | 10 exceções |
| `RegistrarNotaFiscalEntradaUseCase` | 7 exceções |
| `AbrirCaixaUseCase` | 4 exceções |
| `CriarPedidoCompraUseCase` | 4 exceções |
| `ValidarReceitaUseCase` | 3 exceções |
| `CadastrarReceitaUseCase` | 3 exceções |
| `RegistrarEntradaEstoqueUseCase` | 3 exceções |
| `FecharCaixaUseCase` | 2 exceções |
| `CadastrarFornecedorUseCase` | 2 exceções |
| `ConfirmarPedidoCompraUseCase` | 2 exceções |
| `ClienteContatoService` | 2 exceções |
| Demais (1 cada) | ConsultarMedicamento, CancelarVenda, ConsultarVenda, ConsultarEstoque, AtualizarItemEstoque, ConsultarCliente, CadastrarCliente, CadastrarMedicamento, ConferirNotaComPedido, ConsultarReceita |

**Verificação:** não deve existir `static class XxxException` em `farmacia-application` nem em controllers da API.

---

## 8. Padrão 5 — ApiExceptionHandler desacoplado

### ANTES — import acoplado

```java
import br.com.farmacia.application.receituario.usecase.ValidarReceitaUseCase;

@ExceptionHandler(ValidarReceitaUseCase.ReceitaNaoEncontradaException.class)
public ResponseEntity<Problem> handleReceitaNaoEncontrada(...) { ... }
```

### DEPOIS — import do domínio

```java
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;

@ExceptionHandler(ReceitaNaoEncontradaException.class)
public ResponseEntity<Problem> handleReceitaNaoEncontrada(ReceitaNaoEncontradaException ex) {
    return notFound(ex.getMessage(), "https://farmacia.com.br/receita-nao-encontrada",
        "Receita não encontrada", "A receita informada não foi localizada no sistema.");
}
```

### Handlers agrupados por tipo de erro

```java
@ExceptionHandler({
    QuantidadeInvalidaException.class,
    LoteVencidoException.class,
    ParametroInvalidoException.class,
    MotivoObrigatorioException.class,
    // ...
})
public ResponseEntity<Problem> handleRegraEstoqueInvalida(RuntimeException ex) {
    return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/estoque-invalido",
        "Operação de estoque inválida", ex.getMessage());
}
```

**Ganho para QA:** erros de estoque retornam **422** com mensagem clara, não mais 500 genérico por exceção não mapeada.

---

## 9. Padrão 6 — Controllers sem regra de negócio

### PdvController

**ANTES:**

```java
.orElseThrow(() -> new PdvNaoEncontradoException(numero));  // inner class no controller

public static class PdvNaoEncontradoException extends RuntimeException { ... }
```

**DEPOIS:**

```java
import br.com.farmacia.domain.financeiro.exception.PdvNaoEncontradoException;

.orElseThrow(() -> new PdvNaoEncontradoException(numero));
// sem inner class no controller
```

### ReceitaController

**ANTES:**

```java
.orElseThrow(() -> new FarmaceuticoNaoVinculadoException(funcionarioId));
// inner class no controller
```

**DEPOIS:**

```java
import br.com.farmacia.domain.receituario.exception.FarmaceuticoNaoVinculadoException;
```

**Teste QA:** usuário sem registro de farmacêutico tentando validar receita → **403 Forbidden** com `userMessage` sobre farmacêutico não habilitado.

---

## 10. Padrão 7 — Infraestrutura com `atribuirId()`

### Por que não usar `setId()`?

Com `@Setter` removido, adapters de persistência não podem mais fazer `entity.setId(uuid)`. O método `atribuirId()`:

1. Só funciona **uma vez** (evita sobrescrever identidade)
2. Valida id não nulo
3. Deixa explícito que é operação de **persistência**, não regra de negócio

```java
// Padrão em todos os RepositoryAdapter
if (cliente.getId() == null) {
    cliente.atribuirId(UUID.randomUUID());
}
```

### PedidoCompraRepositoryAdapter — caso especial

```java
// DEPOIS
item.vincularPedido(pedido.getId());
item.inicializarQuantidadeRecebida();  // garante 0 se null
```

### DevOperacionalSeed — atualização de entidade existente

Como não há setters, entidades existentes são **reconstruídas com Builder**:

```java
estoqueRepository.salvar(ItemEstoque.builder()
    .id(item.getId())              // preserva identidade
    .medicamento(med)
    .quantidadeAtual(200)
    .quantidadeMinima(10)
    .quantidadeMaxima(item.getQuantidadeMaxima())
    .build())
```

**Lição QA:** seeds e testes também devem respeitar o encapsulamento — usar Builder ou métodos de domínio, nunca setter direto.

---

## 11. Alterações nos testes (QA)

### Por que os testes quebraram?

Testes usavam `receita.setStatus()`, `lote.setQuantidadeAtual()` — métodos que **deixaram de existir** propositalmente.

### Como foram corrigidos

| Arquivo | Estratégia |
|---------|------------|
| `ValidarReceitaUseCaseTest` | `Receita.builder().status(PENDENTE).build()` em vez de `setStatus` |
| `VendaMedicamentoControladoSteps` | Helper `reconstruirReceita()` com Builder |
| `IntegracaoTestSeed` | Builder com `.id()` para updates |
| `MedicamentoControllerWebMvcTest` | Import exceção do pacote `domain.medicamento.exception` |

### Exemplo — teste de exceção

**ANTES:**

```java
assertThrows(ValidarReceitaUseCase.ReceitaNaoEncontradaException.class, () -> ...);
```

**DEPOIS:**

```java
import br.com.farmacia.domain.receituario.exception.ReceitaNaoEncontradaException;

assertThrows(ReceitaNaoEncontradaException.class, () -> ...);
```

**Lição:** testes de contrato HTTP devem validar **status code + body Problem**, não o nome da inner class.

---

## 12. Matriz de testes manuais sugerida

### Venda

| ID | Cenário | Passos | Resultado esperado |
|----|---------|--------|-------------------|
| V01 | Venda normal com estoque | Vender 2 unidades de medicamento com lote ativo | Lote decrementado; se zera → status ESGOTADO |
| V02 | Estoque insuficiente | Vender qtd > disponível | HTTP 409, type estoque-insuficiente |
| V03 | Venda com receita | Vender medicamento controlado com receita aprovada | Receita status UTILIZADA |
| V04 | Cancelar venda no dia | Cancelar venda finalizada hoje | Lote.restaurar(); estoque incrementado |
| V05 | Cancelar venda inválida | Cancelar venda de ontem | HTTP 409, venda-nao-cancelavel |

### Receituário

| ID | Cenário | Resultado esperado |
|----|---------|-------------------|
| R01 | Aprovar receita pendente | Status APROVADA, retida se Azul/Amarela |
| R02 | Rejeitar receita vencida | Status REJEITADA + motivo |
| R03 | Validar sem ser farmacêutico | HTTP 403 farmaceutico-nao-vinculado |
| R04 | Buscar receita inexistente | HTTP 404 receita-nao-encontrada |

### Caixa / PDV

| ID | Cenário | Resultado esperado |
|----|---------|-------------------|
| C01 | Abrir caixa | PDV status ABERTO |
| C02 | Abrir caixa já aberto | HTTP 409 caixa-ja-aberto |
| C03 | Fechar caixa | Caixa FECHADO, saldoFechamento calculado |
| C04 | PDV inexistente (contexto) | HTTP 404 pdv-nao-encontrado |

### Estoque

| ID | Cenário | Resultado esperado |
|----|---------|-------------------|
| E01 | Ajuste negativo sem saldo | HTTP 422 saldo insuficiente |
| E02 | Ajuste sem motivo | HTTP 422 motivo obrigatório |
| E03 | Entrada em lote vencido | HTTP 422 lote vencido |
| E04 | Scheduler vencimento | Lote VENCIDO, saldo 0, alerta gerado |

### Cliente

| ID | Cenário | Resultado esperado |
|----|---------|-------------------|
| CL01 | CPF duplicado | HTTP 409 cpf-duplicado |
| CL02 | Telefone duplicado | HTTP 409 telefone-duplicado |
| CL03 | Atualizar cliente | Dados persistidos via `cliente.atualizar()` |

### Compras

| ID | Cenário | Resultado esperado |
|----|---------|-------------------|
| CP01 | Confirmar pedido rascunho | Status CONFIRMADO |
| CP02 | NF-e chave duplicada | HTTP 409 nfe-duplicada |
| CP03 | Conferir nota com pedido | ItemPedido quantidadeRecebida incrementada |

---

## 13. Como validar tecnicamente

### Compilação e testes automatizados

```bash
cd c:\Java\Farmacia
mvn test -pl farmacia-api -am
```

Resultado esperado: **BUILD SUCCESS** (inclui testes unitários, WebMvc e cenários Cucumber BDD).

### Verificações estáticas (grep)

```bash
# Não deve haver @Setter no domínio
rg "@Setter" farmacia-domain/src/main/java

# Não deve haver inner exception nos use cases
rg "static class.*Exception" farmacia-application/src

# Não deve haver inner exception nos controllers API
rg "static class.*Exception" farmacia-api/src/main
```

### Inspecionar resposta HTTP no Postman/Insomnia

1. Provocar erro de negócio (ex.: CPF duplicado)
2. Verificar status code correto (409, não 500)
3. Verificar campos: `status`, `type`, `title`, `detail`, `userMessage`, `timestamp`

---

## 14. Glossário para estudo

| Termo | Significado |
|-------|-------------|
| **Entidade de domínio** | Objeto com identidade e regras de negócio (ex.: Lote, Receita) |
| **Agregado** | Grupo de entidades com raiz (ex.: Venda + ItemVenda + Pagamento) |
| **Use Case** | Orquestra fluxo: busca dados, chama métodos de domínio, persiste |
| **DomainException** | Erro de regra de negócio, não erro técnico |
| **Encapsulamento** | Estado só muda por métodos validados, não por setters públicos |
| **Problem Details (RFC 7807)** | Padrão JSON para erros HTTP estruturados |
| **Regressão** | Bug introduzido ao mudar código que antes funcionava |
| **Contrato de API** | Status HTTP + formato do body que o cliente espera |

---

## Diagrama — fluxo completo de uma venda (pós-refatoração)

```
Cliente HTTP POST /vendas
        │
        ▼
VendaController
        │
        ▼
RealizarVendaUseCase
        ├── valida caixa aberto → CaixaFechadoException (409)
        ├── para cada item:
        │       ├── aloca lotes FEFO
        │       ├── lote.consumir(qtd)     ← domínio
        │       └── movimentação estoque
        ├── receita.marcarComoUtilizada()  ← domínio
        ├── venda.finalizar(cupom)         ← domínio
        └── vendaRepository.save()
        │
        ▼
ApiExceptionHandler (se erro) → Problem JSON
```

---

## 15. Correções pós-auditoria (mudanca_01.1)

Após a refatoração DDD principal, uma auditoria identificou **dois bugs pontuais**. Ambos foram corrigidos; a suíte `mvn test -pl farmacia-api -am` continua passando.

### Veredicto da auditoria (refatoração principal)

| Item | Status |
|------|--------|
| `@Setter` removido das 26 entidades do domain | ✅ Completo (0 ocorrências) |
| ~51 exceções no domain por bounded context | ✅ Completo |
| 0 `static class ... Exception` em use cases/controllers | ✅ Completo |
| Use cases usam métodos de domínio (zero setters diretos) | ✅ Completo |
| JWT RS256 (`JwtKeyConfig`, `JwtRsaKeyLoader`, `JwtKeyProperties`) | ✅ Completo |
| Bônus: `ClienteValidacao`, `ClienteContatoService`, testes | ✅ Completo |

---

### Bug 1 — `EstoqueRepositoryAdapter.decrementarSaldo()` silenciava saldo negativo

**Arquivo:** `farmacia-infrastructure/.../estoque/EstoqueRepositoryAdapter.java`

**Problema:** o adapter usava `Math.max(0, ...)`, zerando o saldo consolidado em vez de sinalizar inconsistência entre `ItemEstoque` (consolidado) e lotes individuais.

**ANTES (incorreto):**

```java
int novoSaldo = Math.max(0, item.getQuantidadeAtual() - quantidade);
item.setQuantidadeAtual(novoSaldo);
```

**DEPOIS (correto):**

```java
int disponivel = item.getQuantidadeAtual() != null ? item.getQuantidadeAtual() : 0;
int novoSaldo = disponivel - quantidade;
if (novoSaldo < 0) {
    String nomeMedicamento = medicamentoJpaRepository.findById(medicamentoId)
        .map(m -> m.getNomeComercial())
        .orElse(medicamentoId.toString());
    throw new EstoqueInsuficienteException(nomeMedicamento, quantidade, disponivel);
}
item.setQuantidadeAtual(novoSaldo);
item.setUltimaMovimentacao(LocalDateTime.now());
itemRepository.save(item);
```

**Por que `EstoqueInsuficienteException`?**

- Mesma exceção usada em `RealizarVendaUseCase` quando falta estoque na venda
- Já mapeada no `ApiExceptionHandler` → **HTTP 409**, `type: estoque-insuficiente`
- Inclui `userMessage` amigável (nome do medicamento, solicitado vs disponível)

**Quem chama `decrementarSaldo()`:**

| Chamador | Contexto |
|----------|----------|
| `RealizarVendaUseCase` | Após `lote.consumir()` — valida estoque antes, adapter falha se houver divergência |
| `RegistrarAjusteSaldoUseCase` | Ajuste negativo no consolidado |
| `AlertaVencimentoScheduler` | Bloqueio de lote vencido — decrementa consolidado |

**Impacto em QA:** inconsistência lote × consolidado passa a **falhar explicitamente** (409), não mascarar com saldo 0.

---

### Bug 2 — `ClienteDadosInvalidosException` fora da hierarquia `DomainException`

**Arquivo:** `farmacia-domain/.../cliente/exception/ClienteDadosInvalidosException.java`

**Problema:** era a **única** exceção de negócio do domain que estendia `RuntimeException` diretamente, quebrando a hierarquia DDD.

**ANTES:**

```java
public class ClienteDadosInvalidosException extends RuntimeException {
    public ClienteDadosInvalidosException(String message) {
        super(message);
    }
}
```

**DEPOIS:**

```java
import br.com.farmacia.domain.shared.exception.DomainException;

public class ClienteDadosInvalidosException extends DomainException {
    public ClienteDadosInvalidosException(String message) {
        super(message);
    }
}
```

**Nota sobre HTTP:** o `ApiExceptionHandler` **já possuía** handler dedicado antes da correção:

```java
@ExceptionHandler(ClienteDadosInvalidosException.class)
public ResponseEntity<Problem> handleClienteDadosInvalidos(ClienteDadosInvalidosException ex) {
    return unprocessableEntity(ex.getMessage(), "https://farmacia.com.br/cliente-dados-invalidos",
        "Dados do cliente inválidos", ex.getMessage());
}
```

Portanto a API já retornava **422** (não 500). A correção garante **consistência arquitetural**: 100% das exceções de negócio no domain estendem `DomainException`.

**Quem lança:** `ClienteValidacao` / `CadastrarClienteUseCase` / `AtualizarClienteUseCase` (nome inválido, CPF, e-mail, telefone, data de nascimento).

---

### Veredicto final (pós mudanca_01.1)

| Área | Status |
|------|--------|
| Rich Domain / exceções / use cases | ✅ Completo |
| Bug 1 — saldo consolidado silencioso | ✅ Corrigido |
| Bug 2 — hierarquia `DomainException` | ✅ Corrigido |
| Problemas críticos ou médios conhecidos | ✅ Nenhum |
| `mvn test -pl farmacia-api -am` | ✅ Passando |

**Observação (edge case não corrigido):** se `decrementarSaldo()` for chamado para medicamento **sem** registro em `itens_estoque`, o `ifPresent` ainda não executa nada (comportamento anterior). Tratamento opcional para iteração futura.

---

## Resumo executivo

| Métrica | Valor |
|---------|-------|
| Entidades sem `@Setter` | 26/26 no domínio |
| Exceções novas no domínio | 39 classes |
| Use cases refatorados | 24 arquivos |
| Repository adapters atualizados | 17 arquivos |
| Inner exceptions removidas | ~52 |
| Testes atualizados | 5 arquivos |
| Correções pós-auditoria (mudanca_01.1) | 2 bugs (EstoqueRepositoryAdapter + ClienteDadosInvalidosException) |
| Exceções estendendo `DomainException` | 100% no domain |
| `mvn test -pl farmacia-api -am` | ✅ Passando |

---

*Documento gerado para estudo de QA — refatoração DDD mudanca_01 + correções mudanca_01.1.*  
*Projeto: Farmacia — Java 21 + Spring Boot 3.5 + React.*
