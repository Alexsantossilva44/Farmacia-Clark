# Relatório de Testes Exploratórios — Farmacia Clark
**Data:** 12 de Junho de 2026  
**Metodologia:** Glenford Myers — *"Testar software é o processo de executar um programa com a intenção de encontrar erros"*  
**Heurística Aplicada:** SFDIPOT (Structure, Function, Data, Interface, Platform, Operations, Time)  
**Testador:** Alex Santos Silva  
**Sistema:** Farmacia Clark — Java 21 · Spring Boot 3.5 · Arquitetura Hexagonal

---

## O que é Teste Exploratório?

Teste exploratório é uma abordagem onde o testador **aprende, projeta e executa testes simultaneamente**, sem scripts predefinidos. A mente do testador explora o sistema como um detetive, procurando contradições, brechas e comportamentos inesperados.

> **Myers (1979):** Um caso de teste bem-sucedido é aquele que encontra um erro ainda não descoberto.

---

## Escopo do Sistema Testado

| Módulo | Responsabilidade |
|--------|-----------------|
| `farmacia-domain` | Entidades, regras de negócio, enums regulatórios |
| `farmacia-application` | Use Cases: venda, receituário, estoque |
| `farmacia-infrastructure` | Scheduler automático, repositórios JPA |
| `farmacia-api` | Controllers REST, WebMvcTest, BDD/Cucumber |

---

## Mapa Mental da Exploração (SFDIPOT)

```
FARMACIA CLARK
│
├── STRUCTURE (Estrutura)
│   ├── Arquitetura Hexagonal — domínio isolado de infraestrutura
│   ├── Multi-módulo Maven — dependências entre módulos
│   └── Enum NivelControle — mapeamento regulatório ANVISA
│
├── FUNCTION (Função)
│   ├── Validar Receita — compatibilidade tipo × medicamento
│   ├── Realizar Venda — ordem de validação × decremento de estoque
│   └── Alertar Scheduler — contagem de alertas gerados
│
├── DATA (Dados)
│   ├── Nomes com caracteres especiais (hífen)
│   ├── CPF — dígitos verificadores
│   └── Quantidades — mínimo/máximo de estoque
│
├── INTERFACE (Interface)
│   ├── REST endpoints — serialização JSON
│   └── WebMvcTest — validações @Valid, HTTP status
│
├── PLATFORM (Plataforma)
│   ├── Java 21 — switch expressions
│   └── Spring Boot 3.5 — dirty-check NÃO se aplica a objetos de domínio
│
├── OPERATIONS (Operações)
│   ├── Scheduler automático — jobs agendados
│   └── Fluxo de venda — receita → estoque → pagamento
│
└── TIME (Tempo) ← área mais rica em bugs!
    ├── Dia exato de vencimento da receita
    ├── Lotes vencidos aguardando scheduler
    └── Validade máxima por tipo de receita
```

---

## Bugs Encontrados — Tabela Completa

| ID | Descrição do Teste | Status | Erro Encontrado | Prioridade |
|----|--------------------|--------|-----------------|------------|
| **H-01** | Realizar venda com medicamento livre sem receita | ✅ PASSOU | Nenhum | — |
| **H-02** | Realizar venda com medicamento controlado com CPF informado | ✅ PASSOU | Nenhum | — |
| **H-03** | Realizar venda com receita inválida — verificar se estoque é decrementado antes da validação | ❌ FALHOU | Estoque decrementado **antes** da validação de receita/CPF | 🔴 Crítica |
| **H-04** | Realizar venda sem CPF para medicamento controlado | ✅ PASSOU | Nenhum | — |
| **H-05** | Realizar venda com preço acima do PMC ANVISA | ✅ PASSOU | Nenhum | — |
| **H-06** | Realizar venda com múltiplos lotes (FEFO) | ✅ PASSOU | Nenhum | — |
| **M-01** | Validar receita SIMPLES para medicamento B1 (entorpecente) | ✅ PASSOU | Nenhum | — |
| **M-02** | Validar receita AZUL para medicamento C1 (psicotrópico) | ✅ PASSOU | Nenhum | — |
| **M-03** | Validar receita AMARELA para medicamento C2 (retinoide) | ✅ PASSOU | Nenhum | — |
| **M-04** | Validar receita para medicamento **B2** (entorpecente veterinário) | ❌ FALHOU | `CONTROLADO_B2` caia no `default → "SIMPLES"` em vez de `"BRANCA_ESPECIAL"` | 🟠 Alta |
| **M-05** | Scheduler — log de alertas de estoque mínimo | ❌ FALHOU | Log reportava total de candidatos, incluindo duplicatas ignoradas | 🟡 Média |
| **M-06** | Validar receita BRANCA_ESPECIAL para B1 aprovada corretamente | ✅ PASSOU | Nenhum | — |
| **M-07** | Validar receita com CRM do prescritor ausente | ✅ PASSOU | Nenhum | — |
| **M-08** | Aprovar receita — verificar se status é persistido no banco | ❌ FALHOU | `receitaRepository.save()` ausente — mudança nunca persistia | 🔴 Crítica |
| **M-09** | Rejeitar receita com violações — verificar persistência | ❌ FALHOU | Mesmo bug de M-08 | 🔴 Crítica |
| **C-01** | Cadastrar cliente com nome "João da Silva" | ✅ PASSOU | Nenhum | — |
| **C-02** | Cadastrar cliente com nome acentuado "José Antônio" | ✅ PASSOU | Nenhum | — |
| **C-03** | Cadastrar cliente com nome muito longo (>100 chars) | ✅ PASSOU | Nenhum | — |
| **C-04** | Cadastrar cliente com CPF inválido | ✅ PASSOU | Nenhum | — |
| **C-05** | Cadastrar cliente menor de 18 anos | ✅ PASSOU | Nenhum | — |
| **C-06** | Cadastrar cliente com data de nascimento futura | ✅ PASSOU | Nenhum | — |
| **C-07** | Cadastrar cliente com e-mail inválido | ✅ PASSOU | Nenhum | — |
| **C-08** | Scheduler — lote vencido encontrado pela query `findByStatus(ATIVO, ...)` | ❌ FALHOU | Builder criava lote com `status = VENCIDO` — nunca encontrado pelo scheduler | 🟠 Alta |
| **C-09** | Dia exato de vencimento da receita — válida ou vencida? | ❌ FALHOU | `estaVencida()` e `estaValida()` retornavam `true` simultaneamente | 🔴 Crítica |
| **D-01** | Listar medicamentos paginado | ✅ PASSOU | Nenhum | — |
| **D-02** | Cadastrar medicamento com EAN inválido | ✅ PASSOU | Nenhum | — |
| **D-03** | Cadastrar medicamento com preço zero | ✅ PASSOU | Nenhum | — |
| **D-04** | Deletar medicamento sem permissão (role GERENTE) | ✅ PASSOU | Nenhum | — |
| **D-05** | Acesso não autenticado a endpoint protegido | ✅ PASSOU | Nenhum | — |
| **D-06** | Verificar tipo de receita exigido para B2 via `getTipoReceitaRequerido()` | ❌ FALHOU | Retornava `"SIMPLES"` em vez de `"BRANCA_ESPECIAL"` | 🟠 Alta |
| **D-07** | Cadastrar cliente com nome composto por hífen ("Maria da Silva-Santos") | ❌ FALHOU | Regex rejeitava hífen — nomes válidos eram recusados | 🟡 Média |
| **T-01** | Alerta de vencimento próximo — mensagem contém indicativo de urgência | ❌ FALHOU | Assertiva buscava `"crítico"` (com acento) mas a mensagem continha `"CRITICO"` | 🔴 Crítica |

---

## Resumo Quantitativo

| Categoria | Quantidade |
|-----------|-----------|
| Total de testes executados | 32 |
| ✅ Passou | 22 (68,75%) |
| ❌ Falhou — bugs encontrados | 10 (31,25%) |
| 🔴 Críticos | 4 |
| 🟠 Alta prioridade | 3 |
| 🟡 Média prioridade | 2 |
| ⚪ Baixa prioridade | 1 |

---

## Análise de Risco por Área

### 🔴 Área de Maior Risco: Persistência de Estado (M-08/M-09)
Em arquitetura hexagonal, objetos de domínio **não são entidades JPA**. Mudanças de estado não são detectadas por dirty-checking automático. O `save()` explícito é obrigatório. Esse tipo de bug é silencioso — o código compila, o teste de unidade pode passar, mas em produção os dados nunca são gravados.

### 🔴 Área de Risco: Ordem de Operações (H-03)
O bug H-03 demonstra que a **sequência importa**. Validar depois de modificar é um erro clássico em fluxos transacionais.  
> **Regra de ouro:** Valide tudo antes de modificar qualquer estado.

### 🟠 Área de Risco: Cobertura de Enum (M-04/D-06)
Enums com `default` em switch são perigosos: quando um novo valor é adicionado, o compilador não avisa que está caindo no `default`.  
> **Boa prática:** Sempre prefira switch **exhaustivo** (sem `default`) — o compilador força cobertura total.

### 🟡 Área de Risco: Lógica de Data (C-09)
O bug C-09 é um **off-by-one temporal** clássico.  
> **Pergunta que todo QA deve fazer:** *"O último dia é inclusivo ou exclusivo?"*  
> `isAfter(data)` = exclusivo | `!isBefore(data)` = inclusivo (inclui o próprio dia)

---

## Lições Aprendidas — Para o QA Master

| # | Lição |
|---|-------|
| 1 | **Boundary Testing em datas** é sempre produtivo: teste hoje, ontem, amanhã e o dia exato do limite |
| 2 | **Rastreie o estado persistido**: verifique se operações que deveriam salvar dados realmente chamam `save()` |
| 3 | **Enums sem cobertura total** são bugs latentes aguardando um novo valor ser adicionado |
| 4 | **A ordem das operações** em fluxos transacionais deve ser testada explicitamente com cenários de falha |
| 5 | **Dados de teste semanticamente errados** fazem testes passarem pelo motivo errado — tão perigoso quanto testes que falham |
| 6 | **Encoding e acentuação** em mensagens de log ou assertivas devem ser verificados em diferentes ambientes |
