# Relatório de Correção de Bugs — Farmacia Clark
**Data:** 12 de Junho de 2026  
**Sistema:** Farmacia Clark — Java 21 · Spring Boot 3.5 · Arquitetura Hexagonal  
**Repositório:** https://github.com/Alexsantossilva44/Farmacia-Clark  
**Resultado Final:** 76 testes · 0 falhas · 0 erros · BUILD SUCCESS

---

## Visão Geral dos Commits

| Commit | Descrição | Arquivos Alterados |
|--------|-----------|--------------------|
| `cf1fecc` | Commit inicial — 468 arquivos do projeto | 468 |
| commit #2 | Correção dos bugs críticos (🔴) | 3 |
| `cfce987` | Correção dos bugs restantes (🟠🟡) | 7 |
| push | Publicado em GitHub | — |

---

## Correção 1 — BUG H-03 🔴 CRÍTICO
### Ordem de Validação na Realização de Venda
**Arquivo:** `farmacia-application/src/main/java/.../venda/usecase/RealizarVendaUseCase.java`

#### Problema
O fluxo de venda decrementava o estoque **antes** de validar receita e CPF. Se a validação falhasse, o estoque já estava incorreto.

```
FLUXO ERRADO:
1. Para cada item → decrementa estoque  ← BUG
2. Valida receita
3. Valida CPF
4. Se falhar em 2 ou 3 → estoque já baixou indevidamente!
```

#### Correção Aplicada
```java
// H-03: pré-valida receita e CPF antes de qualquer decremento de estoque
{
    for (Input.ItemInput pre : input.itens()) {
        Medicamento preMed = medicamentoRepository.findById(pre.medicamentoId())...;
        if (preMed.isRequerReceita()) {
            validarReceita(input.receitaId(), preMed, pre.quantidade()); // valida primeiro
        }
    }
    if (precisaCpf && cpfVazio) {
        throw new CpfObrigatorioException(...); // falha antes de tocar no estoque
    }
}
// Só aqui: decrementa estoque com segurança
```

```
FLUXO CORRETO:
1. Pré-valida receita para todos os itens
2. Pré-valida CPF obrigatório
3. SE tudo OK → decrementa estoque
```

> **Conceito QA — Princípio "Fail Fast":** Falhe cedo, antes de modificar qualquer estado. Isso garante consistência de dados em qualquer cenário de erro.

---

## Correção 2 — BUG M-08/M-09 🔴 CRÍTICO
### Receita Aprovada/Rejeitada Não Persistida no Banco
**Arquivo:** `farmacia-application/src/main/java/.../receituario/usecase/ValidarReceitaUseCase.java`

#### Problema
```java
// ERRADO — mudança só em memória, nunca salva no banco
private void aprovarReceita(Receita receita, Farmaceutico farmaceutico) {
    receita.aprovar(farmaceutico);
    // FALTAVA: receitaRepository.save(receita);
}
```

#### Por Que Isso Acontece?
Em arquitetura hexagonal, `Receita` é uma **entidade de domínio pura** (sem `@Entity`). O JPA não monitora objetos de domínio — não há dirty-checking automático. O `save()` explícito é sempre obrigatório.

#### Correção Aplicada
```java
// CORRETO
private void aprovarReceita(Receita receita, Farmaceutico farmaceutico) {
    receita.aprovar(farmaceutico);
    receitaRepository.save(receita); // obrigatório em arquitetura hexagonal
}

private void rejeitarReceita(Receita receita, Farmaceutico farmaceutico,
                              List<String> violacoes) {
    receita.rejeitar(farmaceutico, String.join("; ", violacoes));
    receitaRepository.save(receita); // obrigatório
}
```

> **Conceito QA — Teste de Persistência:** Sempre verifique com `verify(repository).save(any())` que operações de negócio realmente persistem dados.

---

## Correção 3 — BUG T-01 🔴 CRÍTICO
### Assertiva com Acentuação Incorreta no Teste
**Arquivo:** `farmacia-api/src/test/java/.../unit/AlertaVencimentoSchedulerTest.java`

#### Problema
```java
// ERRADO — "crítico" com acento vs "CRITICO" sem acento
assertThat(captor.getValue().getMensagem())
    .containsIgnoringCase("crítico"); // não ignora acentos!
```

`containsIgnoringCase` ignora maiúsculas/minúsculas, mas **não ignora acentos**. A mensagem real no código era `"CRITICO"` (sem acento), portanto a assertiva sempre falhava.

#### Correção Aplicada
```java
// CORRETO — usa exatamente o texto que aparece na mensagem
assertThat(captor.getValue().getMensagem())
    .containsIgnoringCase("CRITICO");
```

> **Conceito QA — Assertivas sobre Constantes:** Prefira assertivas sobre constantes definidas no código em vez de strings literais. Se o código mudar a mensagem, o teste quebrará indicando a mudança.

---

## Correção 4 — BUG D-06/M-04 🟠 ALTA
### CONTROLADO_B2 com Tipo de Receita Errado
**Arquivo:** `farmacia-domain/src/main/java/.../medicamento/enums/NivelControle.java`

#### Contexto Regulatório — Portaria ANVISA 344/98
| Classificação | Tipo de Medicamento | Receita Exigida |
|---------------|--------------------|-----------------| 
| Lista B1 | Entorpecentes (morfina, codeína) | Branca Especial |
| Lista B2 | Entorpecentes veterinários | Branca Especial |
| Lista C1 | Psicotrópicos (clonazepam) | Azul |
| Lista C2 | Retinoides (isotretinoína) | Amarela |
| Antimicrobiano | Antibióticos | Simples + retenção |

#### Problema
```java
// ERRADO — B2 caia no default → retornava "SIMPLES" (VIOLAÇÃO DA PORTARIA 344)
public String getTipoReceitaRequerido() {
    return switch (this) {
        case CONTROLADO_B1 -> "BRANCA_ESPECIAL"; // B2 não estava aqui!
        case CONTROLADO_C1 -> "AZUL";
        case CONTROLADO_C2 -> "AMARELA";
        case ANTIMICROBIANO -> "SIMPLES ou ANTIMICROBIANO";
        default -> "SIMPLES"; // ← B2 caia aqui incorretamente
    };
}
```

#### Correção Aplicada
```java
// CORRETO — B1 e B2 mapeados juntos (mesma exigência regulatória)
public String getTipoReceitaRequerido() {
    return switch (this) {
        // B1 e B2 são entorpecentes (Lista B, Portaria 344/98) — ambos exigem Branca Especial
        case CONTROLADO_B1, CONTROLADO_B2 -> "BRANCA_ESPECIAL";
        case CONTROLADO_C1 -> "AZUL";
        case CONTROLADO_C2 -> "AMARELA";
        case ANTIMICROBIANO -> "SIMPLES ou ANTIMICROBIANO";
        default -> "SIMPLES";
    };
}
```

> **Conceito QA — Conhecimento de Domínio:** QA em sistemas regulatórios precisa entender a legislação. Sem saber da Portaria 344/98, o bug D-06 seria invisível — o código funcionava, mas produzia resultado regulatoriamente errado.

---

## Correção 5 — BUG C-09 🔴 CRÍTICO
### Contradição Lógica em Datas de Vencimento de Receita
**Arquivo:** `farmacia-domain/src/main/java/.../receituario/entity/Receita.java`

#### Problema — Off-by-One Temporal

```java
// estaValida() usa !isAfter() → true quando hoje <= dataValidade (inclusivo)
public boolean estaValida() {
    return !LocalDate.now().isAfter(this.dataValidade) && status == APROVADA;
}

// estaVencida() usava !isBefore() → true quando hoje >= dataValidade (inclusivo)
public boolean estaVencida() {
    return !LocalDate.now().isBefore(this.dataValidade); // BUG
}

// No DIA DE VENCIMENTO (hoje == dataValidade):
// estaValida()  → !false → TRUE
// estaVencida() → !false → TRUE  ← CONTRADIÇÃO LÓGICA
```

#### Visualização

```
               dataValidade = 20/06/2026
                        │
ANTES:   ───[válida]────[AMBAS TRUE]────[vencida]───
                         ↑ dia 20

DEPOIS:  ───[válida]────[válida]────[vencida]───
              até dia 20  ↑        ↑ a partir do dia 21
                     dia 20 (último dia válido, inclusive)
```

#### Tabela de Comportamento

| Hoje | `estaVencida()` ANTES | `estaVencida()` DEPOIS | `estaValida()` |
|------|----------------------|----------------------|----------------|
| 19/06 | false | false | true (se APROVADA) |
| **20/06 (= dataValidade)** | **true ← BUG** | **false ← correto** | true |
| 21/06 | true | true | false |

#### Correção Aplicada
```java
// CORRETO — isAfter() é exclusivo: só retorna true DEPOIS da data
public boolean estaVencida() {
    // C-09: dataValidade é o último dia válido (inclusive);
    // isAfter() garante mutex com estaValida() que usa !isAfter()
    return LocalDate.now().isAfter(this.dataValidade);
}
```

> **Conceito QA — Boundary Testing em Datas:** Sempre teste: `dataValidade - 1`, `dataValidade` (o próprio dia), `dataValidade + 1`. Esses três valores revelam bugs de fronteira invisíveis em testes com datas arbitrárias.

---

## Correção 6 — BUG C-08 🟠 ALTA
### Dado de Teste Semanticamente Errado no Builder
**Arquivo:** `farmacia-api/src/test/java/.../qa/builder/FarmaciaTestBuilders.java`

#### Problema
```java
// ERRADO — lote criado com status VENCIDO
public static LoteBuilder umLoteVencido() {
    return new LoteBuilder()
        .comDataValidade(LocalDate.now().minusDays(1))
        .comStatus(StatusLote.VENCIDO); // ← BUG SEMÂNTICO
}

// O scheduler busca com:
loteRepository.findByStatusAndDataValidadeBefore(StatusLote.ATIVO, LocalDate.now())
// Lotes com status VENCIDO nunca seriam encontrados! → teste testava cenário impossível
```

#### Correção Aplicada
```java
// CORRETO — lote expirado ainda não processado pelo scheduler
public static LoteBuilder umLoteVencido() {
    return new LoteBuilder()
        .comDataValidade(LocalDate.now().minusDays(1))
        // C-08: ATIVO = expirado aguardando processamento do scheduler
        .comStatus(StatusLote.ATIVO);
}
```

> **Conceito QA — Semântica dos Dados de Teste:** Dados de teste incorretos são piores que ausência de testes — criam **falsa segurança**. O método se chama `umLoteVencido()` mas o contexto correto é "lote expirado aguardando o scheduler processar", que é `ATIVO`.

---

## Correção 7 — BUG M-05 🟡 MÉDIA
### Log de Alertas com Contagem Incorreta
**Arquivo:** `farmacia-infrastructure/src/main/java/.../scheduler/AlertaVencimentoScheduler.java`

#### Problema
```java
// ERRADO — reportava candidatos totais, não alertas gerados
for (ItemEstoque item : itensAbaixoMinimo) {
    if (alertaJaExiste) continue; // ← 7 itens ignorados
    alertaRepository.save(alerta); // ← 3 alertas gerados
}
log.info("{} alerta(s) gerado(s).", itensAbaixoMinimo.size()); // reportava 10, deveria ser 3!
```

#### Correção Aplicada
```java
// CORRETO — contador real
int alertasGerados = 0; // M-05: conta apenas alertas efetivamente salvos
for (ItemEstoque item : itensAbaixoMinimo) {
    if (alertaJaExiste) continue;
    alertaRepository.save(alerta);
    alertasGerados++; // M-05: incrementa só após save
}
log.info("{} alerta(s) gerado(s) de {} candidato(s).",
    alertasGerados, itensAbaixoMinimo.size()); // distingue gerados vs candidatos
```

> **Conceito QA — Observabilidade como Requisito:** Logs são parte do sistema. Um log que reporta dados incorretos é um bug — pode levar equipes operacionais a tomar decisões erradas.

---

## Correção 8 — BUG D-07 🟡 MÉDIA
### Nomes Compostos com Hífen Rejeitados
**Arquivo:** `farmacia-domain/src/main/java/.../cliente/ClienteValidacao.java`

#### Problema
```java
// ERRADO — regex não permite hífen
private static final Pattern NOME_PESSOA = Pattern.compile(
    "^[\\p{L}]+(?: [\\p{L}]+)+$");

// normalizarNome() removia o hífen antes da validação
return nome.trim()
    .replaceAll("[^\\p{L}\\s]", "") // ← remove hífen!
    .replaceAll("\\s+", " ");
```

#### Correção Aplicada
```java
// CORRETO — hífen simples permitido entre partes do nome
private static final Pattern NOME_PESSOA = Pattern.compile(
    "^[\\p{L}]+(?:-[\\p{L}]+)*(?: [\\p{L}]+(?:-[\\p{L}]+)*)+$");

return nome.trim()
    .replaceAll("[^\\p{L}\\s-]", "") // D-07: mantém hífen
    .replaceAll("\\s+", " ");
```

#### Tabela de Casos de Teste

| Nome | Resultado Esperado | Motivo |
|------|-------------------|--------|
| `"João da Silva"` | ✅ Válido | Nome padrão |
| `"Maria da Silva-Santos"` | ✅ Válido | Hífen simples |
| `"José-Maria da Costa"` | ✅ Válido | Hífen no primeiro nome |
| `"Jorge ---Macedo"` | ❌ Inválido | Múltiplos hífens consecutivos |
| `"Ana*Silva"` | ❌ Inválido | Caractere especial |
| `"Maria"` | ❌ Inválido | Apenas um nome (sem sobrenome) |

---

## Resultado Final

```
[INFO] Tests run: 76, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Todos os 8 bugs corrigidos sem introduzir regressões.
