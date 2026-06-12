# app.spec.ts — linha por linha

**Arquivo real:** `farmacia-web/e2e/app.spec.ts`  
**Função:** Testar o sistema **já autenticado** — menu, estoque, entrada de mercadoria, PDV.

**4 testes** no projeto `app` (depende de `auth.setup.ts` ter passado).

---

## Linhas 1–9 — imports e login automático

| Linha | Código | Explicação |
|-------|--------|------------|
| 1 | `import { test, expect } from '@playwright/test'` | Ferramentas base do Playwright. |
| 2 | `import { garantirSessaoAdmin } from './helpers/autenticar'` | Função que loga via API + `sessionStorage`. |
| 3 | `import { FARMACIA_NOME_COMPLETO } from './helpers/credenciais'` | Nome da marca para asserts. |
| 4 | `import { linkMenu } from './helpers/navegacao'` | Helper para links do menu lateral. |
| 5 | (vazia) | — |
| 6 | `test.describe('App autenticado', () => {` | Grupo no relatório. |
| 7 | `test.beforeEach(async ({ page, request }) => {` | Antes de **cada** teste: precisa de `page` **e** `request` (HTTP). |
| 8 | `await garantirSessaoAdmin(page, request)` | Garante JWT válido e painel carregado. |
| 9 | `})` | Fim do hook. |

**Diferença do login.spec:** aqui o `beforeEach` **não** vai para `/login` para digitar senha — usa atalho estável pela API.

---

## Teste 1 — `sidebar exibe marca Farmácia Clark` (linhas 10–15)

| Linha | Código | Explicação |
|-------|--------|------------|
| 10 | `test('sidebar exibe marca...', async ({ page }) => {` | Caso: identidade visual no menu. |
| 11 | `const menu = page.getByRole('complementary')` | Região `<aside>` — barra lateral. |
| 12 | `FARMACIA_NOME_COMPLETO.split(' ')[0]` | Pega primeira palavra: **"Farmácia"**. |
| 12 | `menu.getByText(...).toBeVisible()` | “Farmácia” aparece no menu. |
| 13 | `menu.getByText('Clark')` | Segunda parte da marca. |
| 14 | `linkMenu(page, 'Estoque')` | Link do menu (não o card do painel). |
| 14 | `.toBeVisible()` | Confirma que usuário logado vê navegação principal. |

**O que valida:** sessão ativa + layout do shell (AppShell) com menu.

---

## Teste 2 — `navega para estoque e exibe listagem` (linhas 17–22)

| Linha | Código | Explicação |
|-------|--------|------------|
| 17 | Início do teste | — |
| 18 | `page.goto('/estoque')` | Navega direto para rota de estoque (SPA React). |
| 19 | `heading ... /Estoque & FEFO/i` | Título da página de estoque. |
| 20 | `button ... /Nova entrada/i` | Botão para abrir fluxo de entrada. |
| 21 | `getByPlaceholder(/Buscar medicamento/i)` | Campo de busca na listagem. |

**O que valida:** rota `/estoque` renderiza componentes principais.

---

## Teste 3 — `abre formulário de nova entrada de mercadoria` (linhas 24–29)

| Linha | Código | Explicação |
|-------|--------|------------|
| 24 | Início | — |
| 25 | `goto('/estoque')` | Mesmo ponto de partida. |
| 26 | `getByRole('button', { name: /Nova entrada/i }).click()` | **Ação** do usuário: clica no botão. |
| 27 | `heading ... /Entrada de mercadoria/i` | Painel/modal de entrada abriu. |
| 28 | `getByText(/Somente hoje ou datas futuras/i)` | Regra de validade (datas passadas bloqueadas) visível na UI. |

**O que valida:** interação — clique abre formulário + texto de regra de negócio.

**Padrão Arrange–Act–Assert:**

1. **Arrange:** `goto` estoque (já logado pelo beforeEach)  
2. **Act:** clique em Nova entrada  
3. **Assert:** heading + mensagem de validade  

---

## Teste 4 — `navega para PDV` (linhas 31–34)

| Linha | Código | Explicação |
|-------|--------|------------|
| 31 | Início | — |
| 32 | `page.goto('/vendas')` | Rota do ponto de venda. |
| 33 | `heading ... /Nova venda/i` | Título da tela de vendas. |

**O que valida:** módulo PDV carrega para usuário autenticado.

---

## Visão geral dos 4 testes

```
beforeEach → garantirSessaoAdmin (API + sessionStorage + /)
    │
    ├─ Teste 1: sidebar (marca + link Estoque)
    ├─ Teste 2: /estoque (listagem)
    ├─ Teste 3: /estoque → Nova entrada (formulário)
    └─ Teste 4: /vendas (PDV)
```

---

## Por que não testamos “vender um item” aqui?

Estes são testes **E2E de fumaça** (smoke): provam que rotas críticas **abrem** sem erro.  
Um teste completo de venda exigiria: produto em estoque, carrinho, pagamento, estoque decrementado — mais longo e frágil. Bom próximo passo quando você evoluir em QA.

---

## Checklist mental ao ler este arquivo

- [ ] Entendo que `request` é o cliente HTTP do Playwright (não é o `fetch` do browser).  
- [ ] Entendo que `goto('/estoque')` usa URL relativa por causa do `baseURL`.  
- [ ] Sei por que `linkMenu` evita ambiguidade no teste 1.  
- [ ] Sei a diferença entre teste que só **olha** a tela e teste que **clica** (teste 3).
