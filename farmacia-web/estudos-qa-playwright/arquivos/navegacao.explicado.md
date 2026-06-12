# navegacao.ts — linha por linha

**Arquivo real:** `e2e/helpers/navegacao.ts`  
**Função:** Achar links do **menu lateral** sem confundir com outros links da página.

---

## Código completo

```typescript
import type { Page } from '@playwright/test'

/** Link do menu lateral (evita ambiguidade com cards do painel). */
export function linkMenu(page: Page, nome: string) {
  return page.getByRole('complementary').getByRole('link', { name: nome, exact: true })
}
```

---

## Linha por linha

| Linha | Código | O que faz |
|-------|--------|-----------|
| 1 | `import type { Page } from '@playwright/test'` | Importa só o **tipo** `Page` (aba do navegador). Não gera código JavaScript extra. |
| 2 | (vazia) | — |
| 3 | Comentário | Explica o problema que resolve: dois links “Estoque” na mesma página. |
| 4 | `export function linkMenu(page, nome)` | Função reutilizável: recebe a página e o texto do menu (ex.: `'Estoque'`). |
| 5 | `getByRole('complementary')` | Restringe busca à região **complementar** — no HTML é o `<aside>` (barra lateral). |
| 5 | `.getByRole('link', { name: nome, exact: true })` | Dentro do aside, acha link com nome **exato** (não “Estoque FEFO” do card). |

---

## O problema real (caso de estudo)

No **Painel**, existe:

- Link **Estoque** no menu (sidebar)  
- Card com texto **“Estoque FEFO”** que também leva a `/estoque`  

`page.getByRole('link', { name: 'Estoque' })` → Playwright acha **2 elementos** → erro *strict mode violation*.

`linkMenu(page, 'Estoque')` → só o do menu.

---

## Boas práticas QA

| Ruim | Melhor |
|------|--------|
| `page.locator('.sidebar a:nth-child(4)')` | Quebra se CSS mudar |
| `getByText('Estoque')` | Pega card e menu |
| `getByRole('complementary').getByRole('link', ...)` | Semântica de acessibilidade |
