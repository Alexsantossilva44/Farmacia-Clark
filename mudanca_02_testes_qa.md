# mudanca_02 — Guia de Testes QA (Postman + Cucumber + Checklist)

> Complemento do [mudanca_01.md](./mudanca_01.md) (inclui [§15 — correções mudanca_01.1](./mudanca_01.md#15-correções-pós-auditoria-mudanca_011)).  
> Objetivo: executar regressão da refatoração DDD com ferramentas práticas.

---

## Arquivos deste pacote QA

| Arquivo | Uso |
|---------|-----|
| [mudanca_01_checklist.csv](./mudanca_01_checklist.csv) | Planilha de execução (abrir no Excel) |
| [postman/Farmacia_QA_mudanca_01.postman_collection.json](./postman/Farmacia_QA_mudanca_01.postman_collection.json) | Collection importável no Postman/Insomnia |
| Este documento | Roteiro Cucumber + instruções detalhadas |

---

## 1. Preparação do ambiente

### 1.1 Subir a API

```bash
cd c:\Java\Farmacia
docker compose up -d          # PostgreSQL (se usar Docker)
mvn spring-boot:run -pl farmacia-api -am
```

API: **http://localhost:8080**  
Swagger: **http://localhost:8080/swagger-ui.html**

### 1.2 Credenciais de desenvolvimento

| Papel | E-mail | Senha | Uso nos testes |
|-------|--------|-------|----------------|
| Admin | `admin@farmacia.com` | `admin123` | Caixa, estoque, compras |
| Balconista | `balconista@farmacia.com` | `bal123` | Vendas, clientes |
| Farmacêutico | `farmaceutico@farmacia.com` | `farm123` | Validar receitas |

### 1.3 Testes automatizados (smoke)

Antes de testar manualmente, confirme que a suíte passa:

```bash
mvn test -pl farmacia-api -am
```

---

## 2. Checklist Excel (`mudanca_01_checklist.csv`)

### Como abrir

1. Abra o Excel → **Dados → De Texto/CSV**
2. Selecione `mudanca_01_checklist.csv`
3. Delimitador: **ponto e vírgula (`;`)**
4. Codificação: **UTF-8**

### Colunas explicadas

| Coluna | Significado |
|--------|-------------|
| **ID** | Identificador único do caso (ex.: `VND-04`) |
| **Módulo** | Área funcional |
| **Tipo** | Postman / Cucumber / JUnit / Verificação |
| **Prioridade** | Alta / Média / Baixa |
| **Cenário** | Nome legível do teste |
| **Pré-condição** | Estado necessário antes de executar |
| **Passos** | O que fazer |
| **Resultado Esperado** | Comportamento correto |
| **HTTP** | Status code esperado (API) |
| **type URI** | Campo `type` do Problem Details |
| **Exceção Domínio** | Classe em `farmacia-domain/.../exception/` |
| **Automatizado** | Sim/Não |
| **Comando/Referência** | Comando Maven ou request Postman |
| **Status** | Preencha: `Pendente` → `OK` ou `Falha` |
| **Observações** | Bugs, prints, links |

### Dica para QA

Filtre por **Prioridade = Alta** e **Status = Pendente** para a primeira rodada de regressão pós-deploy.

---

## 3. Postman — Collection `Farmacia_QA_mudanca_01`

### 3.1 Importar

1. Postman → **Import** → selecione `postman/Farmacia_QA_mudanca_01.postman_collection.json`
2. A collection traz variável `baseUrl = http://localhost:8080`

### 3.2 Ordem recomendada de execução

```
01 Autenticação
  └─ Login Admin          → preenche {{token}}
  └─ Login Farmacêutico   → preenche {{tokenFarmaceutico}}

02 PDV
  └─ Contexto PDV-01      → preenche {{pdvId}}

09 Medicamento
  └─ Listar Medicamentos  → preenche {{medicamentoId}}

07 Cliente
  └─ Cadastrar Cliente    → preenche {{clienteId}}

05 Receituário
  └─ Cadastrar Receita    → ajuste prescritorId
  └─ Validar Receita      → usa token farmacêutico

04 Venda
  └─ Realizar Venda       → preencha funcionarioId no body ou variável
  └─ Consultar / Cancelar

06 Estoque
  └─ Entrada / Ajuste / Alertas

03 Caixa
  └─ Abrir / Fechar       → cuidado: ambiente dev pode já ter caixa aberto
```

### 3.3 Obter `funcionarioId`

Após login, decodifique o JWT em [jwt.io](https://jwt.io) ou use:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"balconista@farmacia.com\",\"senha\":\"bal123\"}"
```

O claim `funcionarioId` no token é o UUID para `{{funcionarioId}}`.

### 3.4 Validar Problem Details (RFC 7807)

Requests com sufixo `(404)`, `(401)`, `(422)` já incluem **Tests** no Postman.

Exemplo — PDV não encontrado:

```json
{
  "status": 404,
  "type": "https://farmacia.com.br/pdv-nao-encontrado",
  "title": "PDV não encontrado",
  "detail": "PDV não encontrado: PDV-INEXISTENTE",
  "userMessage": "O ponto de venda informado não foi localizado.",
  "timestamp": "2026-06-01T..."
}
```

**O que validar como QA:**

- [ ] `status` numérico = HTTP status
- [ ] `type` contém slug correto (não URL genérica 500)
- [ ] `userMessage` é legível para o usuário final
- [ ] **Não** retorna 500 para erro de negócio conhecido

### 3.5 Mapa rápido — Request → Exceção → HTTP

| Request Postman | HTTP | type URI |
|-----------------|------|----------|
| PDV Não Encontrado | 404 | pdv-nao-encontrado |
| Login Senha Inválida | 401 | credenciais-invalidas |
| Ajuste Sem Motivo | 422 | estoque-invalido |
| Medicamento Inexistente | 404 | medicamento-nao-encontrado |
| Venda caixa fechado | 409 | caixa-fechado |
| CPF duplicado cliente | 409 | cpf-duplicado |
| Validar receita (balconista) | 403 | farmaceutico-nao-vinculado |

---

## 4. Cucumber BDD — Cenários automatizados

### 4.1 Onde estão os arquivos

```
farmacia-api/src/test/resources/features/
├── venda_medicamento_controlado.feature   (@controlado — 9 cenários)
└── alertas_estoque_vencimento.feature       (@alertas — 6 cenários)

farmacia-api/src/test/java/br/com/farmacia/qa/bdd/
├── CucumberRunnerIT.java
└── steps/
    ├── VendaMedicamentoControladoSteps.java
    ├── AlertasEstoqueVencimentoSteps.java
    └── LoteComumSteps.java
```

### 4.2 Executar todos os BDD

```bash
mvn test -pl farmacia-api -am -Dtest=CucumberRunnerIT
```

### 4.3 Executar por tag

**Venda medicamento controlado (9 cenários):**

```bash
mvn test -pl farmacia-api -am -Dtest=CucumberRunnerIT "-Dcucumber.filter.tags=@controlado"
```

**Alertas estoque/vencimento (6 cenários):**

```bash
mvn test -pl farmacia-api -am -Dtest=CucumberRunnerIT "-Dcucumber.filter.tags=@alertas"
```

### 4.4 Feature: Venda Medicamento Controlado

**Arquivo:** `venda_medicamento_controlado.feature`

| # | Cenário | O que valida (pós mudanca_01) |
|---|---------|----------------------------------|
| 1 | Venda bem-sucedida receita Azul | `lote.consumir()`, `receita.marcarComoUtilizada()`, SNGPC |
| 2 | Receita vencida | `receita.rejeitar()` via validação |
| 3 | Tipo receita errado (C1 vs Simples) | Regra compatibilidade Portaria 344 |
| 4 | CPF obrigatório | `CpfObrigatorioException` → 400 |
| 5 | Preço acima PMC | `PrecoAcimaPMCException` → 422 |
| 6 | Qtd acima Portaria 344 | Rejeição na validação |
| 7 | Estoque insuficiente | `EstoqueInsuficienteException` → 409 |
| 8 | Seleção FEFO | Lote mais próximo do vencimento consumido |
| 9 | Aviso lote vencendo | Venda OK + warning na resposta |

**Contexto comum (Given):**

```gherkin
Dado que o PDV "PDV-01" está com o caixa aberto
E que o farmacêutico "CRF-12345/SP" está disponível para validação
E que o medicamento controlado "Rivotril 2mg" está cadastrado com nível "CONTROLADO_C1"
E que há 50 unidades do lote "LOT-2024-RIV" com validade para "2026-12-31"
```

**Como o step funciona (após refatoração):**

- Steps **não usam mais** `receita.setStatus()` — usam `Receita.builder()` ou métodos de domínio
- Lotes são ajustados via `IntegracaoTestSeed.garantirLote()` com Builder
- Validação de receita chama API/use case que executa `receita.aprovar()` / `receita.rejeitar()`

### 4.5 Feature: Alertas Estoque e Vencimento

**Arquivo:** `alertas_estoque_vencimento.feature`

| # | Cenário | O que valida (pós mudanca_01) |
|---|---------|----------------------------------|
| 1 | Lote vencido bloqueado | `lote.bloquearPorVencimento()` no scheduler |
| 2 | Alerta crítico 30 dias | Geração alerta CRÍTICO |
| 3 | Alerta atenção 60 dias | Geração alerta ATENÇÃO |
| 4 | Sem duplicidade | Idempotência do scheduler |
| 5 | Estoque mínimo | Alerta ESTOQUE_MINIMO |
| 6 | Estoque zerado | Alerta ESTOQUE_ZERADO |

**Step relevante — scheduler:**

```java
// AlertaVencimentoScheduler (DEPOIS)
int quantidadeBloqueada = lote.bloquearPorVencimento();
loteRepository.save(lote);
estoqueRepository.decrementarSaldo(medicamentoId, quantidadeBloqueada);
```

### 4.6 Relatório Cucumber

Após execução, veja:

```
farmacia-api/target/cucumber-reports/cucumber.html
```

Abra no navegador para ver cenários verdes/vermelhos com stack trace.

---

## 5. Testes unitários relacionados à mudanca_01

| Classe de teste | O que cobre |
|-----------------|-------------|
| `ValidarReceitaUseCaseTest` | `aprovar()` / `rejeitar()`; exceções `ReceitaNaoEncontradaException`, `FarmaceuticoNaoEncontradoException` |
| `MedicamentoControllerWebMvcTest` | 404 com `MedicamentoNaoEncontradoException` do domínio |
| `ClienteValidacaoTest` | Validações de cliente (módulo paralelo) |

```bash
# Só ValidarReceita
mvn test -pl farmacia-api -Dtest=ValidarReceitaUseCaseTest

# Só WebMvc medicamento
mvn test -pl farmacia-api -Dtest=MedicamentoControllerWebMvcTest
```

---

## 6. Roteiro manual completo — Fluxo PDV (30 min)

Use este roteiro para validar integração front + back após a refatoração.

### Passo 1 — Login e contexto

1. `POST /auth/token` com balconista
2. `GET /pdv/contexto?numero=PDV-01` → anote `pdvId`, confirme `caixaAberto: true`

### Passo 2 — Receita controlada

1. `POST /receitas` — tipo AZUL, número único
2. Login farmacêutico → `PUT /receitas/{id}/validar` com medicamento controlado
3. `GET /receitas/{id}` → status `APROVADA`, `retida: true` se Azul

### Passo 3 — Venda

1. `POST /vendas` com receitaId, compradorCpf, item, pagamento
2. Verificar **201** e cupom gerado
3. `GET /estoque/medicamentos/{id}/lotes` → saldo decrementado
4. `GET /receitas/{id}` → status `UTILIZADA`

### Passo 4 — Erros de negócio (regressão mudanca_01)

Repita venda **sem CPF** → espere **400** `cpf-obrigatorio`, não 500.

Repita com **pagamento menor** → espere **422** `pagamento-insuficiente`.

### Passo 5 — Cancelamento (se GERENTE/ADMIN)

1. `DELETE /vendas/{id}?motivo=teste`
2. Confirmar estoque restaurado (`lote.restaurar()`)

---

## 7. Verificações estáticas (Definition of Done)

Execute no terminal e marque OK na checklist (`DOM-01` a `DOM-03`):

```powershell
# PowerShell — sem @Setter no domínio
rg "@Setter" c:\Java\Farmacia\farmacia-domain\src\main\java

# Sem inner exception nos use cases
rg "static class.*Exception" c:\Java\Farmacia\farmacia-application\src

# Sem inner exception nos controllers
rg "static class.*Exception" c:\Java\Farmacia\farmacia-api\src\main\java
```

Resultado esperado: **nenhuma linha** em cada comando.

---

## 8. Como reportar bug (template)

```markdown
**ID checklist:** VND-04
**Ambiente:** dev local / homologação
**Passos:**
1. ...
**Esperado:** HTTP 409, type estoque-insuficiente
**Obtido:** HTTP 500, type erro-interno
**Exceção esperada:** EstoqueInsuficienteException
**Evidência:** screenshot Postman / log API
**Relacionado:** mudanca_01 — lote.consumir()
```

---

## 9. Perguntas de estudo (autoavaliação QA)

1. Por que `lote.setQuantidadeAtual()` foi substituído por `lote.consumir()`?
2. Qual a diferença entre `detail` e `userMessage` no Problem Details?
3. Por que `FarmaceuticoNaoVinculadoException` saiu do `ReceitaController`?
4. O que acontece se `ApiExceptionHandler` não mapear uma `DomainException`?
5. Por que testes BDD passaram a usar `Receita.builder()` em vez de setters?
6. Qual cenário Cucumber valida FEFO?
7. Qual HTTP status esperado para CPF duplicado vs receita não encontrada?
8. O que `atribuirId()` impede que `setId()` não impedia?
9. Por que `decrementarSaldo()` não deve usar `Math.max(0, ...)`?
10. Qual exceção o adapter lança quando o saldo consolidado fica negativo?
11. Todas as exceções de negócio do domain estendem qual classe base?

---

## 10. Correções pós-auditoria (mudanca_01.1) — testes QA

Complemento da [seção 15 do mudanca_01.md](./mudanca_01.md#15-correções-pós-auditoria-mudanca_011).

### BUG-01 — Saldo consolidado não pode ser silenciado

| Campo | Valor |
|-------|-------|
| **ID checklist sugerido** | BUG-01 |
| **Arquivo** | `EstoqueRepositoryAdapter.decrementarSaldo()` |
| **Prioridade** | Alta |
| **Tipo** | Regressão / integração |

**Como provocar (cenário de inconsistência):**

1. Medicamento com lote tendo saldo, mas `itens_estoque.quantidade_atual` menor que a soma dos lotes (ou zero)
2. Tentar venda ou ajuste negativo que exceda o consolidado
3. **Esperado:** `EstoqueInsuficienteException` → HTTP **409**, `type: estoque-insuficiente`
4. **Não esperado:** saldo consolidado zerado silenciosamente

**Validação automatizada:** cenários Cucumber `@controlado` (venda) e `@alertas` (scheduler vencimento) — `mvn test -pl farmacia-api -am`.

**Comando:**

```bash
mvn test -pl farmacia-api -am -Dtest=CucumberRunnerIT "-Dcucumber.filter.tags=@controlado"
```

---

### BUG-02 — Hierarquia `DomainException` no cliente

| Campo | Valor |
|-------|-------|
| **ID checklist sugerido** | BUG-02 |
| **Arquivo** | `ClienteDadosInvalidosException.java` |
| **Prioridade** | Média (consistência) |
| **Tipo** | Contrato API + unitário |

**Casos de teste manual (Postman):**

| Cenário | Request | HTTP | type URI |
|---------|---------|------|----------|
| Nome só números | `POST /api/v1/clientes` nome `"12345"` | 422 | cliente-dados-invalidos |
| CPF inválido | `POST /api/v1/clientes` CPF `"111.111.111-11"` | 422 | cliente-dados-invalidos |
| E-mail inválido | `POST /api/v1/clientes` email `"foo@"` | 422 | cliente-dados-invalidos |

**Importante:** resposta deve ser **422**, nunca **500** (`erro-interno`).

**Verificação estática:**

```powershell
rg "extends RuntimeException" c:\Java\Farmacia\farmacia-domain\src\main\java\br\com\farmacia\domain
```

Esperado: **apenas** `DomainException` (classe base abstrata) — nenhuma exceção de negócio fora da hierarquia.

**Teste unitário existente:**

```bash
mvn test -pl farmacia-domain -Dtest=ClienteValidacaoTest
```

---

### Veredicto QA pós-correções

| Verificação | Comando / ação | Esperado |
|-------------|----------------|----------|
| Suíte completa | `mvn test -pl farmacia-api -am` | BUILD SUCCESS |
| Sem `@Setter` no domain | `rg "@Setter" farmacia-domain/.../entity` | 0 resultados |
| Sem inner exceptions | `rg "static class.*Exception" farmacia-application` | 0 resultados |
| Cliente inválido → 422 | Postman cadastro cliente | Problem JSON, não 500 |
| Estoque inconsistente → 409 | Venda/ajuste acima do consolidado | `estoque-insuficiente` |

---

## 11. Referências cruzadas

| Tópico | Documento / Arquivo |
|--------|---------------------|
| Teoria DDD e código alterado | [mudanca_01.md](./mudanca_01.md) |
| Checklist execução | [mudanca_01_checklist.csv](./mudanca_01_checklist.csv) |
| Collection Postman | [postman/Farmacia_QA_mudanca_01.postman_collection.json](./postman/Farmacia_QA_mudanca_01.postman_collection.json) |
| README projeto | [README.md](./README.md) |
| Feature BDD venda | `farmacia-api/src/test/resources/features/venda_medicamento_controlado.feature` |
| Feature BDD alertas | `farmacia-api/src/test/resources/features/alertas_estoque_vencimento.feature` |
| Correções pós-auditoria | [mudanca_01.md §15](./mudanca_01.md#15-correções-pós-auditoria-mudanca_011) |

---

*Guia QA mudanca_02 — Farmacia · Java 21 + Spring Boot 3.5 · inclui mudanca_01.1*
