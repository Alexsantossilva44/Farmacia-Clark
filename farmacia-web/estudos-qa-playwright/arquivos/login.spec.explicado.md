# login.spec.ts — linha por linha

**Arquivo real:** `farmacia-web/e2e/login.spec.ts`  
**Função:** Testar a **tela de login** como um usuário faria — sem atalho de API.

**4 testes** no projeto `login` (não depende do `auth.setup.ts`).

---

## Estrutura geral

```typescript
import → describe → beforeEach → 4 testes
```

---

## Linhas 1–7 — preparação

| Linha | Código | Explicação |
|-------|--------|------------|
| 1 | `import { test, expect } from '@playwright/test'` | `test` = define casos; `expect` = asserções (verificações). |
| 2 | `import { ADMIN, FARMACIA_NOME_COMPLETO } from './helpers/credenciais'` | Dados centralizados (email, senha, nome da marca). |
| 3 | (vazia) | — |
| 4 | `test.describe('Login — Farmácia Clark', () => {` | Agrupa testes no relatório sob o título “Login — Farmácia Clark”. |
| 5 | `test.beforeEach(async ({ page }) => {` | **Antes de cada teste**, roda este bloco. |
| 6 | `await page.goto('/login')` | Abre a página de login (usa `baseURL` da config → `http://localhost:5173/login`). |
| 7 | `})` | Fim do `beforeEach`. |

**Por que `beforeEach`?** Os 4 testes começam na tela de login — evita repetir `goto` em cada um.

---

## Teste 1 — `exibe marca e formulário de acesso` (linhas 9–16)

| Linha | Código | O que verifica |
|-------|--------|----------------|
| 9 | `test('exibe marca...', async ({ page }) => {` | Nome do caso no relatório. |
| 10 | `expect(page).toHaveTitle(/Farmácia Clark/i)` | Aba do navegador contém “Farmácia Clark” (`i` = ignora maiúsculas). |
| 11 | `getByText(FARMACIA_NOME_COMPLETO).first()` | Texto da marca visível (`.first()` se houver mais de um no DOM). |
| 12 | `getByRole('heading', { name: /Entrar no sistema/i })` | Título H1/H2 “Entrar no sistema”. |
| 13 | `getByTestId('login-email')` | Campo e-mail — `data-testid` colocado no React para testes estáveis. |
| 14 | `getByTestId('login-senha')` | Campo senha. |
| 15 | `getByTestId('login-submit')` | Botão enviar. |
| 16 | `.toBeEnabled()` | Botão não está desabilitado. |

**Tipo de teste:** smoke / UI — “a página carregou o essencial?”

---

## Teste 2 — `rejeita credenciais inválidas` (linhas 18–25)

| Linha | Código | O que faz |
|-------|--------|-----------|
| 18 | Início do teste | — |
| 19 | `.fill('invalido@farmacia.com')` | Digita e-mail que **não** existe no seed. |
| 20 | `.fill('senha-errada')` | Senha incorreta. |
| 21 | `.click()` no submit | Envia o formulário. |
| 22 | (vazia) | — |
| 23 | `toHaveURL(/\/login/)` | **Continua** na URL de login (não redirecionou para `/`). |
| 24 | `getByText(/credenciais\|inválid\|erro/i).first()` | Mensagem de erro visível (regex aceita variações de texto). |
| 24 | `timeout: 15_000` | Espera até 15 s (API pode demorar na 1ª requisição). |

**Tipo de teste:** negativo — comportamento quando login **falha**.

---

## Teste 3 — `admin dev autentica e abre o painel` (linhas 27–34)

| Linha | Código | O que faz |
|-------|--------|-----------|
| 28–29 | `fill(ADMIN.email)` e `fill(ADMIN.senha)` | Credenciais do seed dev. |
| 30 | `click()` submit | Login real pela UI. |
| 32 | `toHaveURL('/')` | Redirecionou para o painel principal. |
| 33 | `heading ... /Painel operacional/i` | Título do dashboard visível. |

**Tipo de teste:** caminho feliz — login **com sucesso** ponta a ponta (front + API).

---

## Teste 4 — `com Vite em modo dev exibe atalhos...` (linhas 36–39)

| Linha | Código | O que faz |
|-------|--------|-----------|
| 37 | `getByText(/Contas de desenvolvimento/i)` | Bloco de dicas só aparece em **dev** (`import.meta.env.DEV`). |
| 38 | `getByRole('button', { name: 'Administrador' })` | Botão que preenche conta admin com um clique. |

**Tipo de teste:** ambiente dev — em build de produção este teste **pode falhar** se os atalhos forem removidos (comportamento esperado).

---

## Mapa dos 4 testes

```
beforeEach → /login
    │
    ├─ Teste 1: elementos visíveis?
    ├─ Teste 2: login errado → erro + fica em /login
    ├─ Teste 3: login certo → vai para /
    └─ Teste 4: atalhos dev visíveis?
```

---

## Seletores usados (resumo)

| Método | Quando usar |
|--------|-------------|
| `getByTestId` | IDs estáveis no código (`login-email`) |
| `getByRole` | Botões, headings — alinhado a acessibilidade |
| `getByText` | Texto visível na tela |
| `toHaveTitle` / `toHaveURL` | Estado da página / navegação |

---

## Diferença vs `app.spec.ts`

| | `login.spec.ts` | `app.spec.ts` |
|--|-----------------|---------------|
| Login | Pelo **formulário** | Pela **API** (`garantirSessaoAdmin`) |
| Foco | Tela de acesso | Estoque, PDV, menu |
| Setup | Não usa `auth.setup` | Depende do `setup` |

**Regra de ouro:** teste o que você quer validar. Login visual → `login.spec`. Funcionalidade logada → `app.spec` + helper de API.
