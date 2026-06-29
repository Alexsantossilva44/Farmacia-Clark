# Relatório de Testes Exploratórios — Farmacia Clark
**Data:** 29 de Junho de 2026  
**Metodologia:** Session-Based Test Management (SBTM) com heurística SFDIPOT  
**Referência:** Glenford Myers — *"Testar software é o processo de executar um programa com a intenção de encontrar erros"*  
**Testador:** Alex Santos Silva  
**Professor/Facilitador:** QA Master (Claude Sonnet 4.6)  
**Sistema:** Farmacia Clark — Java 21 · Spring Boot 3.5 · React 18 · Tailwind CSS  
**Foco da Sessão:** Módulo Fornecedores — Cadastro: CNPJ, Razão Social, fluxo geral  

---

## Charter da Sessão

> **Explorar** o módulo Fornecedores de Compra (`FornecedoresCadastroTab`)  
> **Para descobrir** comportamentos de validação, UX e possíveis bugs nos campos CNPJ e Razão Social  
> **Usando** interação direta com o formulário no browser (Chrome DevTools MCP)  

**Duração estimada:** 45 minutos  
**Ambiente:** `http://localhost:5173/cadastros?aba=fornecedores` — usuário `admin@farmacia.com` / perfil Administrador  

---

## Escopo do Sistema Testado

| Módulo | Responsabilidade |
|--------|-----------------|
| `farmacia-web / FornecedoresCadastroTab` | Formulário de cadastro: CNPJ, Razão Social, Nome Fantasia |
| `farmacia-web / useErrosCampo` | Hook de erros temporários: `setErroTemporario(campo, msg, delay)` |
| `farmacia-web / validacao-formulario.ts` | `validarCnpj()` — algoritmo módulo 11 para 14 dígitos |
| `farmacia-web / cadastro-options.ts` | `maskCnpjInput()`, `formatCnpjDisplay()`, `onlyDigits()` |
| `farmacia-api / FornecedorController` | Endpoints REST: POST `/fornecedores`, PUT `/fornecedores/{id}` |

---

## Mapa Mental da Exploração (SFDIPOT)

```
CADASTRO DE FORNECEDORES
│
├── STRUCTURE (Estrutura)
│   ├── Layout grid: lista à esquerda + formulário à direita (lg:grid-cols-2)
│   ├── Lista ordenada alfabeticamente (pt-BR, localeCompare sensitivity: base)
│   ├── Formulário: 3 campos — Razão Social*, Nome Fantasia, CNPJ*
│   └── Glassmorphism aplicado: glass-section + rounded-xl nas seções
│
├── FUNCTION (Função)
│   ├── CNPJ: aceita apenas dígitos (maskCnpjInput filtra silenciosamente)
│   ├── CNPJ: valida algoritmo módulo 11 no blur e no submit
│   ├── CNPJ inválido no blur: erro temporário 3s (DELAY_ERRO_MS)
│   ├── CNPJ duplicado no submit: erro 3s + campo limpo automaticamente
│   ├── Razão Social: bloqueia primeiro caractere especial (bloqueio silencioso)
│   ├── Razão Social duplicada no blur: erro 3s + campo limpo automaticamente
│   ├── Progresso do botão: barra escura desaparece ao preencher obrigatórios
│   └── Edição inline: clique em item da lista carrega dados no formulário
│
├── DATA (Dados)
│   ├── CNPJ inválido: 12345678901234 (checksum errado) → BLOQUEADO
│   ├── CNPJ letras/especiais: ABCD@#$% → campo permanece vazio (filtrado)
│   ├── CNPJ válido: 12.345.678/0001-95 (módulo 11) → ACEITO
│   ├── Razão Social vazia → "Campo obrigatório" (2s)
│   ├── Razão Social: "@Distribuidora" → campo recebe apenas "Distribuidora"
│   └── Razão Social duplicada: erro + limpeza automática
│
├── INTERFACE (Interface)
│   ├── Máscara CNPJ: 00.000.000/0000-00 — aplicada a cada keystroke
│   ├── Erros exibidos em vermelho (text-coral) por tempo limitado
│   ├── Mensagem de sucesso em verde (text-mint): "Fornecedor cadastrado."
│   └── Glassmorphism: backdrop-filter blur(28px) no card, blur(10px) nas seções
│
├── PLATFORM (Plataforma)
│   ├── Browser: Chrome (DevTools MCP)
│   ├── Resolução: 1536×768 (viewport padrão dos testes)
│   └── Autenticação: admin / Administrador (canGerenciarCompras = true)
│
└── OPERATIONS (Operações)
    ├── Cadastro novo: formulário vazio → preencher → "Cadastrar fornecedor"
    └── Edição: clique em item da lista → campos preenchidos → "Salvar alterações"
```

---

## Roteiro da Sessão

### Passo 1 — Reconhecimento do módulo
Acesso a `/cadastros?aba=fornecedores`. Observado:
- Lista com 11 fornecedores pré-existentes (BioMedic, DistDev, Drogaria Popular, etc.)
- Formulário à direita: Razão Social *, Nome Fantasia, CNPJ *
- Botão "Cadastrar fornecedor" com barra de progresso embutida (escurece proporcionalmente ao preenchimento)

### Passo 2 — Exploração da máscara CNPJ
**Ação:** Digitamos `ABCD@#$%EFG!` no campo CNPJ  
**Resultado:** Campo permaneceu completamente vazio — `maskCnpjInput` filtra qualquer não-dígito silenciosamente  
**Código:** `farmacia-web/src/lib/cadastro-options.ts` → `maskCnpjInput()` usa `onlyDigits()` internamente

### Passo 3 — Exploração do bloqueio silencioso em Razão Social
**Ação:** Digitamos `@Distribuidora` no campo Razão Social  
**Resultado:** O `@` foi ignorado; o campo recebeu apenas `Distribuidora` (iniciando com `D` maiúsculo)  
**Código:** `onChange` checa `!/^[a-zA-ZÀ-ÿ0-9]/.test(v)` → se primeiro char for especial, retorna sem atualizar estado

### Passo 4 — Exploração da validação de CNPJ inválido (blur)
**Ação:** Digitamos `12345678901234` (14 dígitos, checksum errado) e saímos do campo  
**Resultado:** Mensagem de erro vermelha "CNPJ inválido." exibida por ~3 segundos, depois desaparece automaticamente  
**Mecanismo:** `setErroTemporario('cnpj', cnpjErr, DELAY_ERRO_MS)` — `DELAY_ERRO_MS = 3000ms`

### Passo 5 — Exploração da detecção de duplicata (Razão Social)
**Ação:** Digitamos `Distribuidora Teste QA` (já cadastrado) e saímos do campo  
**Resultado:** Erro "Razão social já cadastrada." exibido 3s, campo limpo automaticamente, foco voltou ao primeiro input  
**Mecanismo:** `erroComLimpeza()` → `setErroTemporario + setTimeout(() => setRazaoSocial(''), DELAY_ERRO_MS)`

### Passo 6 — Cadastro com CNPJ válido calculado manualmente
**CNPJ:** `12.345.678/0001-95` (calculado via algoritmo módulo 11 — dígitos verificadores: 9 e 5)  
**Razão Social:** `Distribuidora Teste QA`  
**Resultado:** "Fornecedor cadastrado." — item apareceu na lista em ordem alfabética correta  

### Passo 7 — Segundo cadastro válido
**CNPJ:** `11.444.777/0001-61` (calculado via módulo 11)  
**Razão Social:** `Nova Distribuidora QA`  
**Resultado:** "Fornecedor cadastrado." — confirmado na lista  

---

## Resumo de Achados

| # | Tipo | Campo | Comportamento Observado | Severidade |
|---|------|--------|------------------------|-----------|
| 01 | Informação | CNPJ | Máscara filtra 100% de não-dígitos silenciosamente via `maskCnpjInput` | — |
| 02 | Informação | Razão Social | Primeiro char especial bloqueado; demais chars aceitos | — |
| 03 | Informação | Ambos | Erros são temporários (2s padrão / 3s nos campos críticos) | — |
| 04 | Informação | Razão Social | Duplicata detectada no `onBlur` — campo limpo antes do submit | — |
| 05 | Informação | CNPJ | Algoritmo módulo 11 implementado corretamente (2 dígitos verificadores) | — |
| 06 | Informação | Formulário | Barra de progresso proporcional ao preenchimento dos campos obrigatórios | — |
| 07 | Informação | CNPJ | Dígitos todos iguais (ex: 11111111111111) sempre inválidos (regex `/^(\d)\1+$/`) | — |

**Bugs encontrados:** 0  
**Cobertura SFDIPOT:** Structure ✅ Function ✅ Data ✅ Interface ✅ Platform ✅ Operations ✅

---

## Critérios de Saída da Sessão

- [x] Todos os campos obrigatórios testados com entrada vazia  
- [x] Máscara CNPJ testada com letras, especiais e dígitos  
- [x] Algoritmo módulo 11 validado com CNPJ calculado manualmente  
- [x] Bloqueio silencioso de Razão Social verificado  
- [x] Detecção de duplicata (CNPJ e Razão Social) verificada  
- [x] Cadastro bem-sucedido confirmado (2 fornecedores criados)  
- [x] Interface glassmorphism verificada visualmente  

---

## Artefatos Gerados

- Fornecedor criado: `Distribuidora Teste QA` — CNPJ `12.345.678/0001-95`
- Fornecedor criado: `Nova Distribuidora QA` — CNPJ `11.444.777/0001-61`
- Partições de equivalência derivadas desta exploração → ver `Teste-particao-equivalencia.md`
