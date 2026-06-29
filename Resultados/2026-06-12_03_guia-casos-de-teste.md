# Guia de Casos de Teste — Farmacia Clark
**Data:** 12 de Junho de 2026  
**Objetivo:** Material de estudo para QA Master — técnicas e casos de teste aplicados  
**Sistema:** Farmacia Clark — Java 21 · Spring Boot 3.5 · Arquitetura Hexagonal

---

## Parte 1 — Fundamentos do Teste Exploratório

### O que Glenford Myers ensina?

> *"Testing is the process of executing a program with the intent of finding errors."*  
> — Glenford Myers, **The Art of Software Testing** (1979)

Myers inverteu a mentalidade da época:
- **Errado:** Testar é mostrar que o software funciona
- **Certo:** Testar é tentar provar que o software **não funciona**

Um caso de teste que não encontra erro **não é necessariamente bom** — pode ser que o testador não explorou os cenários certos.

---

### A Heurística SFDIPOT

| Letra | Categoria | Perguntas que o QA deve fazer |
|-------|-----------|-------------------------------|
| **S** | Structure | Quais são as partes do sistema? Como se conectam? |
| **F** | Function | O que o sistema faz? Quais são as regras de negócio? |
| **D** | Data | Quais dados entram? Quais são os limites e fronteiras? |
| **I** | Interface | Como o sistema se comunica? APIs, UI, banco? |
| **P** | Platform | Em qual ambiente roda? Qual versão do Java/Framework? |
| **O** | Operations | Como é usado em produção? Quais são os fluxos reais? |
| **T** | Time | Datas, prazos, expiração, agendamentos — **ÁREA MAIS RICA EM BUGS!** |

---

## Parte 2 — Técnicas de Teste Usadas Hoje

### Técnica 1: Análise de Valor Limite (Boundary Value Analysis)

**Definição:** Testa os valores exatamente nos limites das partições, pois é onde os bugs se escondem.

**Aplicado no bug C-09 (Receita.estaVencida):**

```
Partição Válida:   hoje < dataValidade
LIMITE:            hoje == dataValidade  ← onde o bug estava!
Partição Inválida: hoje > dataValidade
```

**Casos de Teste — Datas de Vencimento:**

| # | Entrada | Resultado Esperado | Tipo |
|---|---------|-------------------|------|
| CT-01 | hoje = dataValidade - 1 dia | `estaVencida() = false` | Abaixo do limite |
| CT-02 | hoje = dataValidade | `estaVencida() = false` | **Limite exato (crítico!)** |
| CT-03 | hoje = dataValidade + 1 dia | `estaVencida() = true` | Acima do limite |

> **Regra de ouro:** Sempre teste `limite - 1`, `limite` e `limite + 1`.

---

### Técnica 2: Partição de Equivalência (Equivalence Partitioning)

**Definição:** Divide as entradas em grupos onde o sistema se comporta da mesma forma.

**Aplicado no bug D-07 (Validação de nomes):**

| Partição | Exemplos | Resultado |
|----------|---------|-----------|
| P1 — Nomes simples válidos | "João da Silva", "José Antônio" | ✅ Válido |
| P2 — Nomes com hífen válidos | "Maria da Silva-Santos", "José-Maria da Costa" | ✅ Válido |
| P3 — Múltiplos hífens consecutivos | "Jorge ---Macedo", "Ana--Silva" | ❌ Inválido |
| P4 — Caracteres especiais | "Jorge Mace*+do", "Ana@Silva" | ❌ Inválido |
| P5 — Apenas um nome | "Maria", "João" | ❌ Inválido |

**Casos de Teste:**

| # | Entrada | Resultado Esperado | Partição |
|---|---------|-------------------|----------|
| CT-04 | `"João da Silva"` | Válido ✅ | P1 |
| CT-05 | `"Maria da Silva-Santos"` | Válido ✅ | P2 |
| CT-06 | `"José-Maria da Costa"` | Válido ✅ | P2 |
| CT-07 | `"Jorge ---Macedo"` | Inválido ❌ | P3 |
| CT-08 | `"Jorge Mace*+do"` | Inválido ❌ | P4 |
| CT-09 | `"Maria"` | Inválido ❌ | P5 |

---

### Técnica 3: Teste de Transição de Estado

**Definição:** Testa como o sistema transita entre estados.

**Aplicado no bug M-08/M-09 (Receita):**

```
ESTADOS DA RECEITA:
PENDENTE → APROVADA → UTILIZADA
PENDENTE → REJEITADA
```

**Diagrama de Transição:**
```
         [aprovar()]          [marcarComoUtilizada()]
PENDENTE ──────────► APROVADA ──────────────────────► UTILIZADA
    │
    │ [rejeitar()]
    ▼
REJEITADA
```

**Casos de Teste:**

| # | Estado Inicial | Ação | Estado Esperado | Persistido? |
|---|---------------|------|-----------------|-------------|
| CT-10 | PENDENTE | `aprovar()` + `save()` | APROVADA | ✅ Sim |
| CT-11 | PENDENTE | `rejeitar()` + `save()` | REJEITADA | ✅ Sim |
| CT-12 | APROVADA | `marcarComoUtilizada()` | UTILIZADA | ✅ Sim |
| CT-13 | REJEITADA | `aprovar()` | Exceção | ❌ Inválido |

---

### Técnica 4: Teste de Decisão em Tabela

**Definição:** Testa combinações de condições e as ações resultantes.

**Aplicado no bug D-06/M-04 (NivelControle × Tipo de Receita):**

| Nível de Controle | Receita Apresentada | Resultado | Regulamento |
|------------------|--------------------|-----------|----|
| LIVRE | Qualquer | ✅ Aprovado | — |
| RECEITA_SIMPLES | SIMPLES | ✅ Aprovado | — |
| CONTROLADO_C1 | AZUL | ✅ Aprovado | Portaria 344/98 Lista C |
| CONTROLADO_C1 | SIMPLES | ❌ Rejeitado | Portaria 344/98 Lista C |
| CONTROLADO_C2 | AMARELA | ✅ Aprovado | Portaria 344/98 Lista C |
| CONTROLADO_B1 | BRANCA_ESPECIAL | ✅ Aprovado | Portaria 344/98 Lista B |
| CONTROLADO_B1 | AZUL | ❌ Rejeitado | Portaria 344/98 Lista B |
| **CONTROLADO_B2** | **BRANCA_ESPECIAL** | **✅ Aprovado** ← corrigido | Portaria 344/98 Lista B |
| ANTIMICROBIANO | SIMPLES | ✅ Aprovado | RDC 20/2011 |
| ANTIMICROBIANO | AZUL | ❌ Rejeitado | RDC 20/2011 |

---

### Técnica 5: Teste de Fluxo (Caminho Feliz + Caminhos Alternativos)

**Aplicado no bug H-03 (RealizarVenda):**

```
FLUXO CORRETO DE VENDA:
┌──────────────────────────────────────────────────┐
│ 1. Receber pedido                                 │
│ 2. Pré-validar receita  ← ANTES do estoque!       │
│ 3. Pré-validar CPF      ← ANTES do estoque!       │
│ 4. SE tudo OK → decrementar estoque               │
│ 5. Calcular total                                  │
│ 6. Registrar venda                                 │
│ 7. Retornar comprovante                            │
└──────────────────────────────────────────────────┘
```

| # | Cenário | Falha em | Estoque Após |
|---|---------|----------|--------------|
| CT-15 | Medicamento livre, sem receita | — (sucesso) | Decrementado |
| CT-16 | Controlado com receita e CPF válidos | — (sucesso) | Decrementado |
| CT-17 | Controlado **sem CPF** | Passo 3 | **NÃO decrementado** |
| CT-18 | Receita **vencida** | Passo 2 | **NÃO decrementado** |
| CT-19 | Receita para medicamento errado | Passo 2 | **NÃO decrementado** |

---

## Parte 3 — Suites de Teste Completas (BDD/Gherkin)

### Suite 1: ValidarReceitaUseCase

```gherkin
Feature: Validação de Receita Médica
  Como farmacêutico responsável
  Quero validar receitas antes da dispensação
  Para garantir conformidade com a Portaria 344/98

  Scenario: Receita Azul aprovada para medicamento C1
    Given um medicamento controlado C1 "Rivotril 2mg"
    And uma receita do tipo AZUL dentro do prazo de validade
    And um farmacêutico ativo no sistema
    When o farmacêutico valida a receita
    Then a receita deve ser APROVADA
    And receitaRepository.save() deve ser chamado 1 vez

  Scenario: Receita SIMPLES rejeitada para medicamento B1
    Given um medicamento controlado B1 "Morfina 10mg"
    And uma receita do tipo SIMPLES
    When o farmacêutico valida a receita
    Then a receita deve ser REJEITADA
    And a violação deve conter "requer receita tipo BRANCA_ESPECIAL"
    And receitaRepository.save() deve ser chamado 1 vez

  Scenario: Receita no dia exato de vencimento ainda é válida
    Given uma receita com dataValidade = hoje
    And o medicamento é do tipo RECEITA_SIMPLES
    When o farmacêutico valida com tipo compatível
    Then a receita deve ser APROVADA

  Scenario: Receita vencida desde ontem é rejeitada
    Given uma receita com dataValidade = ontem
    When o farmacêutico valida a receita
    Then a receita deve ser REJEITADA
    And a violação deve conter "Receita vencida"
```

---

### Suite 2: RealizarVendaUseCase

```gherkin
Feature: Realização de Venda Farmacêutica

  Scenario: Venda de medicamento livre sem receita
    Given um medicamento com nível LIVRE
    And o caixa está aberto
    When o balconista registra a venda
    Then a venda é finalizada com sucesso
    And o estoque é decrementado

  Scenario: Venda controlado sem CPF falha antes de tocar no estoque
    Given um medicamento CONTROLADO_C1
    And o comprador não informa CPF
    When o balconista tenta registrar a venda
    Then uma CpfObrigatorioException é lançada
    And o estoque NÃO deve ter sido decrementado

  Scenario: FEFO — usa lote de menor validade primeiro
    Given dois lotes do mesmo medicamento
    And Lote A vence em 30 dias com 50 unidades
    And Lote B vence em 90 dias com 50 unidades
    When o balconista vende 1 unidade
    Then o Lote A deve ter 49 unidades
    And o Lote B deve ter 50 unidades
```

---

### Suite 3: AlertaVencimentoScheduler

```gherkin
Feature: Alertas Automáticos de Estoque e Vencimento

  Scenario: Lote vencido é bloqueado com status ATIVO inicial
    Given um lote com dataValidade = ontem e status = ATIVO
    When o scheduler verificarLotesVencidos() executa
    Then o status do lote deve ser VENCIDO
    And a quantidade do lote deve ser 0
    And um alerta LOTE_VENCIDO deve ser gerado

  Scenario: Lote com 15 dias gera alerta CRÍTICO
    Given um lote com dataValidade = hoje + 15 dias
    And não existe alerta aberto para este lote
    When o scheduler alertarVencimentoProximo() executa
    Then a mensagem do alerta deve conter "CRITICO"

  Scenario: Log de estoque mínimo conta só alertas reais
    Given 5 itens abaixo do mínimo
    And 3 já possuem alertas abertos
    When alertarEstoqueMinimo() executa
    Then apenas 2 alertas devem ser salvos
    And o log deve mostrar "2 alerta(s) de 5 candidato(s)"
```

---

### Suite 4: ClienteValidacao

```gherkin
Feature: Validação de Dados Cadastrais

  Scenario: Nome com hífen simples é aceito
    When valido "Maria da Silva-Santos"
    Then nenhuma exceção deve ser lançada

  Scenario: Nome com múltiplos hífens consecutivos é rejeitado
    When valido "Jorge ---Macedo"
    Then ClienteDadosInvalidosException deve ser lançada

  Scenario: Cliente que completa 18 anos hoje é aceito (boundary)
    Given data de nascimento = hoje menos 18 anos exatos
    When valido a data de nascimento
    Then nenhuma exceção deve ser lançada

  Scenario: Cliente que completa 18 anos amanhã é rejeitado (boundary)
    Given data de nascimento = hoje menos 18 anos mais 1 dia
    When valido a data de nascimento
    Then ClienteDadosInvalidosException com "18 anos" deve ser lançada
```

---

## Parte 4 — Pirâmide de Testes da Farmacia Clark

```
                    ▲
                   /E2E\          ← BDD/Cucumber (poucos, lentos, confiáveis)
                  /─────\
                 /       \
                /Integração\      ← @WebMvcTest, @SpringBootTest (médio volume)
               /─────────────\
              /               \
             /   Unitários      \ ← JUnit 5 + Mockito (maioria, rápidos)
            /─────────────────────\
```

| Tipo | Ferramenta | Quando Usar |
|------|------------|-------------|
| Unitário | JUnit 5 + Mockito | Regras de domínio, use cases |
| Camada Web | `@WebMvcTest` | Serialização JSON, HTTP status, segurança |
| BDD | Cucumber + Gherkin | Fluxos de negócio end-to-end |
| Builder | Test Data Builder | Criar dados de teste reutilizáveis e semânticos |

---

## Parte 5 — Checklist QA Master

Use ao testar qualquer funcionalidade:

**Dados de Entrada**
- [ ] Valor mínimo válido
- [ ] Valor máximo válido
- [ ] Abaixo do mínimo
- [ ] Acima do máximo
- [ ] Nulo / vazio / em branco
- [ ] Caracteres especiais e acentos

**Lógica de Negócio**
- [ ] Caminho feliz (happy path)
- [ ] Todos os branches do switch/if cobertos
- [ ] Enums — todos os valores mapeados (sem `default` silencioso)
- [ ] Estado persistido após operação (`save()` chamado?)

**Tempo e Datas**
- [ ] Data = limite - 1 dia
- [ ] Data = limite exato
- [ ] Data = limite + 1 dia

**Fluxos e Estado**
- [ ] Ordem correta das operações (validar ANTES de modificar)
- [ ] Transições de estado válidas e inválidas
- [ ] Estado não modificado em caso de falha (rollback)

**Observabilidade**
- [ ] Logs reportam dados corretos
- [ ] Assertivas sobre constantes do código (não strings literais com acentos)
- [ ] Dados de teste são semanticamente corretos

---

## Referências para Estudo

| Recurso | Autor | Tópico |
|---------|-------|--------|
| **The Art of Software Testing** | Glenford Myers | Fundamentos — leitura obrigatória |
| **Explore It!** | Elisabeth Hendrickson | Testes exploratórios na prática |
| **Heurística SFDIPOT** | James Bach | Framework de exploração |
| **Portaria ANVISA 344/98** | ANVISA | Regulação farmacêutica brasileira |
| **RDC 20/2011** | ANVISA | Controle de antimicrobianos |
