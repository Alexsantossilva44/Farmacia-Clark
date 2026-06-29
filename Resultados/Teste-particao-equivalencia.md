# Relatório de Testes — Partição de Equivalência — Farmacia Clark
**Data:** 29 de Junho de 2026  
**Técnica:** Partição de Equivalência (Equivalence Partitioning) — ISO/IEC/IEEE 29119  
**Definição:** Divide o domínio de entradas em classes onde o sistema deve se comportar identicamente para qualquer valor da mesma classe. Testa um representante por classe.  
**Testador:** Alex Santos Silva  
**Professor/Facilitador:** QA Master (Claude Sonnet 4.6)  
**Sistema:** Farmacia Clark — Java 21 · Spring Boot 3.5 · React 18 · Tailwind CSS  
**Módulos cobertos:** Fornecedores (CNPJ, Razão Social) · Clientes (CPF)  

---

## Princípio da Técnica

```
Domínio de entradas do campo CNPJ
─────────────────────────────────────────────────────────────────
│  Inválida          │ Inválida │ Inválida │ Inválida │  Válida  │
│  (vazio)           │ (letras) │ (curto)  │(checksum)│(correto) │
─────────────────────────────────────────────────────────────────
    ↑                    ↑          ↑           ↑          ↑
Testar 1 rep.       1 rep.     1 rep.      1 rep.     1 rep.
de cada classe → cobertura total com mínimo de testes
```

**Economia:** 6 casos de teste cobrem todo o espaço de entradas do CNPJ,
em vez de testar 50+ CNPJs que pertencem à mesma classe.

---

## Módulo 1: Fornecedores — Campo CNPJ

**Campo:** `CNPJ *` em `FornecedoresCadastroTab`
**Validação:** `validarCnpj(cnpj, true)` em `farmacia-web/src/lib/validacao-formulario.ts`
**Máscara:** `maskCnpjInput(value)` em `farmacia-web/src/lib/cadastro-options.ts`

### Análise das Classes de Equivalência

| Classe | Condição | Representante escolhido | Justificativa |
|--------|----------|------------------------|---------------|
| I-1: Vazio | `digits.length === 0` | `""` (em branco) | Único valor da classe |
| I-2: Apenas letras/especiais | `onlyDigits(v) === ""` | `ABCD@#$%EFG!` | `maskCnpjInput` filtra tudo |
| I-3: Dígitos insuficientes | `digits.length < 14` | `12345678` (8 dígitos) | Meio da sub-faixa menor que 14 |
| I-4: Dígitos em excesso | Campo rejeita 15º dígito | `123456789012345` (15 dígitos) | maxLength=18 (com máscara) |
| I-5: Checksum errado | Falha no algoritmo módulo 11 | `12345678901234` | 14 dígitos, dígitos verificadores errados |
| V-1: Válido | `validarCnpj` retorna null | `12.345.678/0001-95` | Calculado manualmente com módulo 11 |

### Resultados dos Testes

| ID | Classe | Entrada | Resultado Esperado | Resultado Real | Status |
|---|---|---|---|---|---|
| PE-CNPJ-01 | I-1: Vazio | `(em branco)` + submit | Erro "Campo obrigatório" | Erro temporário 2s exibido, foco no campo | ✅ PASS |
| PE-CNPJ-02 | I-2: Letras/especiais | `ABCD@#$%EFG!` | Campo filtrado, permanece vazio | Campo permaneceu vazio (maskCnpjInput filtrou tudo) | ✅ PASS |
| PE-CNPJ-03 | I-3: Curto | `12345678` (8 dígitos) | Bloqueado no submit | Erro "CNPJ deve conter 14 dígitos." exibido | ✅ PASS |
| PE-CNPJ-04 | I-4: Longo | `123456789012345` (15 dígitos) | 15º dígito rejeitado | Campo aceita no máximo 14 dígitos (maxLength com máscara) | ✅ PASS |
| PE-CNPJ-05 | I-5: Checksum errado | `12345678901234` | Erro "CNPJ inválido." | Erro temporário 3s exibido no blur e bloqueado no submit | ✅ PASS |
| PE-CNPJ-06 | V-1: Válido | `12.345.678/0001-95` | Fornecedor cadastrado | "Fornecedor cadastrado." + item na lista | ✅ PASS |

**Resultado:** 6/6 PASS — 0 falhas

### Cálculo Manual do CNPJ de Teste (PE-CNPJ-06)
```
Dígitos base: 1 2 3 4 5 6 7 8 0 0 0 1

1º dígito verificador (pesos 5,4,3,2,9,8,7,6,5,4,3,2):
1×5 + 2×4 + 3×3 + 4×2 + 5×9 + 6×8 + 7×7 + 8×6 + 0×5 + 0×4 + 0×3 + 1×2
= 5+8+9+8+45+48+49+48+0+0+0+2 = 222
222 % 11 = 2  →  11 - 2 = 9  ✓

2º dígito verificador (pesos 6,5,4,3,2,9,8,7,6,5,4,3,2):
1×6+2×5+3×4+4×3+5×2+6×9+7×8+8×7+0×6+0×5+0×4+1×3+9×2
= 6+10+12+12+10+54+56+56+0+0+0+3+18 = 237
237 % 11 = 6  →  11 - 6 = 5  ✓

CNPJ resultante: 12.345.678/0001-95
```

---

## Módulo 2: Fornecedores — Campo Razão Social

**Campo:** `Razão social *` em `FornecedoresCadastroTab`
**Validação:** `obrigatorio(razaoSocial)` + verificação de duplicata (`checarRazaoSocialDuplicada`)
**Bloqueio silencioso:** `if (v && !/^[a-zA-ZÀ-ÿ0-9]/.test(v)) return`

### Análise das Classes de Equivalência

| Classe | Condição | Representante escolhido | Justificativa |
|--------|----------|------------------------|---------------|
| I-1: Vazio | `razaoSocial.trim() === ""` | `""` (em branco) | Único valor |
| I-2: Inicia com especial | Primeiro char não em `[a-zA-ZÀ-ÿ0-9]` | `@Distribuidora` | `@` é especial |
| I-3: Duplicata | Nome já existe (case-insensitive) | `Distribuidora Teste QA` | Já cadastrado na sessão |
| V-1: Inicia com número | Número em `[0-9]` é aceito pelo regex | `3M Distribuidora` | Números são válidos |
| V-2: Nome típico | Letras + espaços, único | `Nova Distribuidora QA` | Caso de uso normal |

### Resultados dos Testes

| ID | Classe | Entrada | Resultado Esperado | Resultado Real | Status |
|---|---|---|---|---|---|
| PE-RS-01 | I-1: Vazio | `(em branco)` + submit | Erro "Campo obrigatório" | Erro temporário 2s, foco no campo | ✅ PASS |
| PE-RS-02 | I-2: Inicia com `@` | `@Distribuidora` | `@` bloqueado silenciosamente | Campo recebeu apenas "Distribuidora" (D maiúsculo) | ✅ PASS |
| PE-RS-03 | V-1: Inicia com número | `3M Distribuidora` | Aceito (número é válido no regex) | Campo preencheu "3M Distribuidora" sem bloqueio | ✅ PASS |
| PE-RS-04 | I-3: Duplicata | `Distribuidora Teste QA` + blur | Erro "Razão social já cadastrada." + campo limpo | Erro 3s, campo limpo automaticamente | ✅ PASS |
| PE-RS-05 | V-2: Nome típico | `Nova Distribuidora QA` + submit | Cadastro realizado | "Fornecedor cadastrado." confirmado | ✅ PASS |

**Resultado:** 5/5 PASS — 0 falhas

---

## Módulo 3: Clientes — Campo CPF (painel de busca)

**Campo:** `CPF` no painel "Buscar por CPF" em `ClientesCadastroTab`
**Máscara:** `formatCpfDisplay(value)` — aplica máscara `000.000.000-00` apenas quando há exatamente 11 dígitos
**Validação de busca:** Botão "Buscar" habilitado somente quando `onlyDigits(cpfBusca).length === 11`
**Validação de negócio:** `validarCpf(cpf)` em `farmacia-web/src/lib/validacao-cliente.ts` — só executada no submit do formulário

### Análise das Classes de Equivalência

| Classe | Condição | Representante escolhido | Justificativa |
|--------|----------|------------------------|---------------|
| I-1: Vazio | `onlyDigits("").length === 0` | `""` (em branco) | Buscar sempre disabled |
| I-2: Letras | `onlyDigits("abc").length === 0` | `ABCDE` | Máscara não filtra — campo aceita mas Buscar disabled |
| I-3: Curto | `digits.length < 11` | `12345` (5 dígitos) | Buscar disabled |
| I-4: Dígitos iguais | `/^(\d)\1+$/.test(digits)` | `11111111111` | Inválido por regra — mas Buscar habilita |
| I-5: Checksum errado | Falha em `calc(9)` ou `calc(10)` | `12345678900` | 11 dígitos, 2º dígito verificador incorreto |
| V-1: Válido | `validarCpf` retorna null | `12345678909` | Calculado manualmente com módulo 11 |

### Resultados dos Testes

| ID | Classe | Entrada | Resultado Esperado | Resultado Real | Status |
|---|---|---|---|---|---|
| PE-CPF-01 | I-1: Vazio | `""` | Buscar desabilitado | Botão `disabled` desde o início | ✅ PASS |
| PE-CPF-02 | I-2: Letras | `ABCDE` | Buscar desabilitado | Campo mostra "ABCDE", Buscar `disabled` (0 dígitos extraídos) | ✅ PASS |
| PE-CPF-03 | I-3: Curto | `12345` | Buscar desabilitado | Buscar `disabled` (5 dígitos < 11) | ✅ PASS |
| PE-CPF-04 | I-4: Dígitos iguais | `111.111.111-11` | Erro "CPF inválido" no Buscar | ⚠️ Buscar habilitou, API chamada → "CPF não cadastrado." (sem validação prévia) | ⚠️ ALERTA |
| PE-CPF-05 | I-5: Checksum errado | `123.456.789-00` | Erro "CPF inválido" no Buscar | ⚠️ Mesmo comportamento de PE-CPF-04 → API chamada sem validação | ⚠️ ALERTA |
| PE-CPF-06 | V-1: Válido | `123.456.789-09` | "CPF não cadastrado." + form disponível | "CPF não cadastrado." confirmado + formulário desbloqueado | ✅ PASS |

**Resultado:** 4/6 PASS — 2 ALERTAS (lacunas de UX, não falhas de segurança)

### Cálculo Manual do CPF de Teste (PE-CPF-06)
```
Dígitos base: 1 2 3 4 5 6 7 8 9

1º dígito verificador (pesos 10→2):
1×10 + 2×9 + 3×8 + 4×7 + 5×6 + 6×5 + 7×4 + 8×3 + 9×2
= 10+18+24+28+30+30+28+24+18 = 210
210 % 11 = 1  →  1 < 2  →  dígito = 0  ✓

2º dígito verificador (pesos 11→2, inclui o 1º = 0):
1×11 + 2×10 + 3×9 + 4×8 + 5×7 + 6×6 + 7×5 + 8×4 + 9×3 + 0×2
= 11+20+27+32+35+36+35+32+27+0 = 255
255 % 11 = 2  →  11 - 2 = 9  ✓

CPF resultante: 123.456.789-09
```

### Análise dos Alertas (PE-CPF-04 e PE-CPF-05)

**Situação:** O painel "Buscar por CPF" não valida o formato/checksum antes de chamar a API. CPFs com dígitos repetidos ou checksum incorreto têm o botão Buscar habilitado e a requisição é enviada ao servidor.

**Impacto:** UX — o usuário recebe "CPF não cadastrado." em vez de "CPF inválido.", só descobrindo o erro no submit.

**Severidade:** Baixa — o cadastro final é impedido pela validação no submit (`validarCpf`). Não há risco de dados inválidos persistidos no banco.

**Recomendação:** Adicionar validação client-side no painel de busca antes da chamada API:
```typescript
// ClientesCadastroTab.tsx — função tentarBuscar()
const cpfErr = validarCpf(cpfBusca)
if (cpfErr) {
  // exibir erro no painel de busca
  return
}
buscarMutation.mutate()
```

---

## Resumo Consolidado

| Módulo | Campo | Total | PASS | ALERTA | FAIL |
|--------|-------|-------|------|--------|------|
| Fornecedores | CNPJ | 6 | 6 | 0 | 0 |
| Fornecedores | Razão Social | 5 | 5 | 0 | 0 |
| Clientes | CPF (busca) | 6 | 4 | 2 | 0 |
| **TOTAL** | | **17** | **15** | **2** | **0** |

**Taxa de sucesso:** 15/17 (88%) — 0 falhas críticas, 2 alertas de UX
**Bugs de bloqueio:** 0
**Melhoria sugerida:** 1 (validação de CPF no painel de busca antes da chamada API)

---

## Aprendizados da Sessão

### Por que Partição de Equivalência?
- **Eficiência:** 17 casos cobriram 3 campos inteiros. Testar aleatoriamente exigiria centenas de casos para a mesma cobertura.
- **Clareza:** Cada caso representa uma classe — quando falha, sabemos exatamente qual tipo de dado o sistema não trata.
- **Derivação:** As classes foram identificadas lendo o código de validação (`validarCnpj`, `validarCpf`) e o código de `onChange` (bloqueio silencioso).

### Diferença entre as técnicas desta sessão

| Técnica | Quando usar | O que encontra |
|---------|------------|----------------|
| Teste Exploratório | Desconhecido — explorar comportamento geral | Comportamentos inesperados, fluxos não documentados |
| Partição de Equivalência | Campos com validação conhecida | Lacunas em classes específicas de entrada |

Os alertas PE-CPF-04/05 só foram encontrados porque a PE forçou o teste de CPFs inválidos com exatamente 11 dígitos — um testador intuitivo raramente tentaria isso sistematicamente.
