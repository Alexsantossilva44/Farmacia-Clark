# Relatório de Testes VADER — Farmacia Clark
**Data:** 29 de Junho de 2026  
**Técnica:** VADER — V=Válido · A=Adverso · D=Default · E=Extremos · R=Random  
**Testador:** Alex Santos Silva  
**Professor/Facilitador:** QA Master (Claude Sonnet 4.6)  
**Sistema:** Farmacia Clark — Java 21 · Spring Boot 3.5 · React 18 · Tailwind CSS  
**Campo testado:** `Nome completo *` — Módulo Clientes (`ClientesCadastroTab`)  

---

## O que é VADER?

VADER é uma heurística de dados de teste — um "mapa mental" para não esquecer nenhuma categoria de entrada importante. O nome é um acrônimo:

```
V — Válido     → entradas que DEVEM ser aceitas pelo sistema
A — Adverso    → entradas hostis, maliciosas ou inesperadas
D — Default    → entradas vazias, nulas ou padrão do campo
E — Extremos   → valores nos limites do permitido (1 char abaixo / exato / 1 acima)
R — Random     → entradas aleatórias que testam comportamentos não planejados
```

**Por que VADER?** Porque um testador iniciante tende a testar só o caso feliz ("João Silva"). VADER força você a pensar em *todas* as categorias de ataque ao campo. Em 14 casos, cobrimos uma amplitude de dados que testes manuais casuais raramente atingem.

---

## Campo Alvo

**Elemento:** `<input placeholder="Ex.: Jorge Macedo">` em `ClientesCadastroTab.tsx`  
**Arquivo de validação:** `farmacia-web/src/lib/validacao-cliente.ts`  

### Como o campo processa entradas

```
Usuário digita
     ↓
sanitizeNomePessoa(value)      ← dispara em EVERY keystroke (onChange)
  • remove [^\p{L}\s]           → números, hífens, símbolos, emojis
  • remove espaço no início
  • colapsa espaços duplos → um
  • trunca em 100 chars
     ↓
React state atualizado
     ↓
Usuário clica "Cadastrar cliente"
     ↓
validarNomePessoa(nome)        ← dispara APENAS no submit
  • trim() no resultado sanitizado
  • vazio → "Campo obrigatório"
  • regex ^[\p{L}]+(?: [\p{L}]+)+$ → exige mínimo: nome + sobrenome
  • null → campo válido, prossegue para API
```

---

## Resultados dos Testes

### V — Válido (entradas que devem ser aceitas)

| ID | Entrada | sanitize() | validar() | Status |
|----|---------|-----------|-----------|--------|
| V-01 | `João Silva` | `"João Silva"` | `null` | ✅ PASS |
| V-02 | `Ângela Cristina Souza` | `"Ângela Cristina Souza"` | `null` | ✅ PASS |

**Análise:** Nomes com acentos (`ã`, `â`), múltiplas palavras e até três partes funcionam corretamente. `\p{L}` na regex cobre todo o Unicode — letras de qualquer idioma são aceitas.

---

### A — Adverso (entradas hostis)

| ID | Entrada | sanitize() | validar() | Status |
|----|---------|-----------|-----------|--------|
| A-01 | `'; DROP TABLE clientes; --` | `"DROP TABLE clientes "` | `null` | ⚠️ ALERTA |
| A-02 | `<script>alert('xss')</script>` | `"scriptalertxssscript"` | `"Formato inválido"` | ✅ SEGURO |
| A-03 | `João 😀 Silva` | `"João Silva"` | `null` | ✅ PASS |
| A-04 | `João-Silva` | `"JoãoSilva"` | `"Formato inválido"` | ⚠️ UX ALERTA |

#### Detalhamento A-01 — SQL Injection

```
Entrada:    '; DROP TABLE clientes; --
sanitize:   remove ' ; ; -  -  →  " DROP TABLE clientes  "
            remove espaço líder →  "DROP TABLE clientes  "
            colapsa duplo espaço→  "DROP TABLE clientes "
validar:    trim()             →  "DROP TABLE clientes"
            regex test         →  PASSA! (2 palavras com espaço)
            retorna null       →  ENVIADO PARA A API
```

**Risco real:** ZERO. O back-end usa JPA/Hibernate com prepared statements — o valor é tratado como dado literal, não como SQL. Nenhum banco de dados é afetado.

**Problema real:** Qualidade semântica dos dados. O sistema aceita "DROP TABLE clientes" como nome de cliente. Um banco de dados de clientes teria registros com nomes nonsense derivados de tentativas de injection.

**Recomendação:** Nenhuma correção urgente. Se desejado, adicionar validação de tamanho mínimo (ex: 5 chars) — mas isso é custo alto para risco baixo.

#### Detalhamento A-02 — XSS

```
Entrada:    <script>alert('xss')</script>
sanitize:   remove < > ( ) ' /  →  "scriptalertxssscript"
validar:    uma palavra só      →  "Formato inválido"
            BLOQUEADO ✓
```

**Conclusão:** XSS é bloqueado em dois níveis: (1) o sanitize remove todos os caracteres de marcação HTML; (2) mesmo que passasse, o React escapa valores no JSX por padrão. Defesa em profundidade funcionando.

#### Detalhamento A-04 — Nomes com Hífen

```
Entrada:    João-Carlos
sanitize:   remove -          →  "JoãoCarlos"
validar:    uma palavra só    →  "Formato inválido"
            BLOQUEADO
```

**Bug de UX — Severidade: Média.** Nomes brasileiros compostos com hífen (João-Carlos, Ana-Paula, Maria-Fernanda) são comuns e válidos. O sistema os recusa sem explicação — o hífen é silenciosamente removido e o resultado falha na regex.

**Impacto:** Clientes com nomes hifenizados não conseguem se cadastrar. Farmácia perde cliente ou registra nome incorreto (sem hífen).

**Recomendação:** Alterar `sanitizeNomePessoa` para preservar o hífen como separador:
```typescript
// farmacia-web/src/lib/validacao-cliente.ts
// Antes:
.replace(/[^\p{L}\s]/gu, '')
// Depois (permite hífen):
.replace(/[^\p{L}\s\-]/gu, '')
// E atualizar a regex de validação:
const NOME_PESSOA = /^[\p{L}]+([\s\-][\p{L}]+)+$/u
```

---

### D — Default (entradas padrão/vazias)

| ID | Entrada | sanitize() | validar() | Status |
|----|---------|-----------|-----------|--------|
| D-01 | `""` (campo vazio) | `""` | `"Campo obrigatório"` | ✅ PASS |
| D-02 | `"   "` (só espaços) | `""` | `"Campo obrigatório"` | ✅ PASS |
| D-03 | `"12345"` (só números) | `""` | `"Campo obrigatório"` | ✅ PASS |

#### Efeito colateral D-02 / D-03 — Anti-Autofill Reset

Quando o campo esvazia (sanitize retorna `""`), o React re-renderiza com estado vazio. O mecanismo de anti-autofill (`bloquearAutofillInicial`) interpreta isso como "sem dados carregados" e volta ao modo `readOnly`. O formulário inteiro fica travado.

**Solução:** Clicar novamente no formulário (disparando `onMouseDown`) reativa o modo de edição.

**Severidade:** Baixa — é comportamento esperado do anti-autofill, não um bug.

---

### E — Extremos (valores limítrofes)

| ID | Entrada | Chars | sanitize() len | validar() | Status |
|----|---------|-------|----------------|-----------|--------|
| E-01 | `Ana` (1 palavra) | 3 | 3 | `"Formato inválido"` | ✅ PASS |
| E-02 | `A×49 + " " + B×50` (limite exato) | 100 | 100 | `null` | ✅ PASS |
| E-03 | `A×50 + " " + B×50` (um além) | 101 | 100 (truncado) | `null` | ✅ PASS |

#### Detalhamento E-01 — Palavra única

"Ana" passa pelo `sanitize` intacta (só letras), mas o `validarNomePessoa` exige ao menos `nome + espaço + sobrenome`. A regex `^[\p{L}]+(?: [\p{L}]+)+$` requer obrigatoriamente um grupo adicional. "Ana" sozinha falha.

**Por que isso importa?** Farmácias frequentemente atendem clientes que fornecem apenas o primeiro nome. O sistema força o sobrenome — decisão de negócio correta para identificação única.

#### Detalhamento E-03 — Truncamento no limite

```
Entrada (101 chars):    A×50 + " " + B×50
sanitize .slice(0,100): mantém A×49 + " " + B×50  → 100 chars
validar:                null (aceito)
```

O truncamento acontece no `sanitize`, silenciosamente. Um UX de excelência exibiria um contador `X/100 caracteres` para o usuário saber o limite antes de acontecer.

---

### R — Random (entradas aleatórias)

| ID | Entrada | sanitize() | validar() | Status |
|----|---------|-----------|-----------|--------|
| R-01 | `JOÃO SILVA` (todo caps) | `"JOÃO SILVA"` | `null` | ✅ PASS |
| R-02 | `João  Silva` (espaço duplo) | `"João Silva"` | `null` | ✅ PASS |

**R-01:** `\p{L}` na regex cobre letras maiúsculas Unicode — "Ã", "Â", "Ç" maiúsculas são aceitas. O sistema não normaliza capitalização, então "JOÃO SILVA" seria armazenado como digitado.

**R-02:** O `sanitize` detecta espaços duplos (`.replace(/\s{2,}/g, ' ')`) e os colapsa em um único espaço. Comportamento silencioso e correto.

---

## Resumo Consolidado VADER

| Categoria | ID | Entrada resumida | Status | Observação |
|-----------|-----|-----------------|--------|------------|
| Válido | V-01 | `João Silva` | ✅ PASS | — |
| Válido | V-02 | `Ângela Cristina Souza` | ✅ PASS | — |
| Adverso | A-01 | SQL injection | ⚠️ ALERTA | Risco zero, qualidade semântica baixa |
| Adverso | A-02 | XSS `<script>` | ✅ SEGURO | Dupla proteção: sanitize + React escape |
| Adverso | A-03 | Emoji no nome | ✅ PASS | Emoji removido silenciosamente |
| Adverso | A-04 | Nome com hífen | ⚠️ UX ALERTA | Nomes como João-Carlos bloqueados |
| Default | D-01 | Campo vazio | ✅ PASS | — |
| Default | D-02 | Só espaços | ✅ PASS | Efeito colateral: form reset |
| Default | D-03 | Só números | ✅ PASS | Efeito colateral: form reset |
| Extremos | E-01 | `Ana` (1 palavra) | ✅ PASS | Sobrenome exigido |
| Extremos | E-02 | 100 chars válidos | ✅ PASS | Limite exato aceito |
| Extremos | E-03 | 101 chars | ✅ PASS | Truncado silenciosamente a 100 |
| Random | R-01 | `JOÃO SILVA` caps | ✅ PASS | Maiúsculas aceitas |
| Random | R-02 | Espaço duplo | ✅ PASS | Colapsado para 1 espaço |

**Total:** 14 casos  
**PASS:** 12  
**ALERTA:** 2  
**FAIL:** 0  

---

## Bugs e Recomendações

### Bug #1 — Nomes com Hífen (A-04) — Severidade: Média

| Item | Detalhe |
|------|---------|
| Campo | `Nome completo *` em `ClientesCadastroTab` |
| Arquivo | `farmacia-web/src/lib/validacao-cliente.ts` → `sanitizeNomePessoa` |
| Comportamento atual | Hífen removido silenciosamente; nome fica com uma palavra → regex rejeita |
| Comportamento esperado | João-Carlos aceito e armazenado como "João-Carlos" |
| Impacto | Clientes com nomes compostos hifenizados não conseguem se cadastrar |
| Correção | Incluir `-` no allowlist do sanitize + atualizar regex de validação |

### Alerta #2 — Dados Semânticos (A-01) — Severidade: Baixa

| Item | Detalhe |
|------|---------|
| Campo | `Nome completo *` em `ClientesCadastroTab` |
| Comportamento | Strings de SQL injection passam a validação como nomes válidos |
| Risco de segurança | Zero (JPA prepared statements protegem o banco) |
| Problema | Qualidade semântica dos dados no banco |
| Ação sugerida | Validação de tamanho mínimo (ex: `>= 5 chars` após trim) |

---

## Comparação das Técnicas

| Técnica | Casos | Encontrou A-04? | Encontrou A-01? |
|---------|-------|-----------------|-----------------|
| Teste Intuitivo | ~3 | Improvável | Improvável |
| Partição de Equivalência | ~5 | Possível (classe I-especial) | Não (foco no formato) |
| **VADER** | **14** | **Sim (categoria A)** | **Sim (categoria A)** |

**Lição principal:** A-04 (nomes hifenizados) é um bug de UX que afeta um segmento real de clientes brasileiros. A Partição de Equivalência não forçaria o teste de hífen porque a classe "inválida" seria representada por `@` ou `$`. Só VADER, com a categoria **Adverso**, forçou o pensamento em *"o que um usuário real com nome não-padrão digitaria?"*.

---

## Aprendizados

1. **`\p{L}` vs caracteres específicos:** A regex Unicode aceita qualquer letra do mundo, mas a regra de negócio não definiu claramente se hífen faz parte de um nome válido no Brasil. Essa ambiguidade gerou o bug A-04.

2. **Sanitize silencioso vs feedback:** Remover caracteres silenciosamente é uma escolha de UX. O trade-off: menos ruído para o usuário vs. usuário confuso quando o nome que digitou "sumiu" ou "virou outro".

3. **Múltiplas camadas de proteção (Defense in Depth):** XSS falha em 3 camadas — sanitize remove `<>`, React escapa JSX, back-end JPA usa prepared statements. Cada camada funcionaria sozinha; todas juntas garantem robustez.

4. **BVA dentro de VADER:** E-02 (100 chars) e E-03 (101 chars) são exatamente a técnica BVA (Boundary Value Analysis) encaixada dentro da categoria Extremos do VADER. As técnicas se complementam — VADER é o mapa, BVA é o instrumento para a categoria E.
