# auth.setup.ts — linha por linha

**Arquivo real:** `farmacia-web/e2e/auth.setup.ts`  
**Função:** **Pré-condição** — um “teste zero” que confirma: API responde, login funciona, front aceita sessão.

---

## Código completo

```typescript
import { test as setup } from '@playwright/test'
import { garantirSessaoAdmin } from './helpers/autenticar'

/** Valida que API + front permitem autenticação antes dos testes autenticados. */
setup('pré-condição: API e sessão admin', async ({ page, request }) => {
  await garantirSessaoAdmin(page, request)
})
```

---

## Linha por linha

| Linha | Código | Explicação |
|-------|--------|------------|
| 1 | `import { test as setup } from '@playwright/test'` | Importa `test` mas renomeia para `setup`. **Motivo:** no relatório HTML aparece como projeto “setup”, não mistura com `login`/`app`. |
| 2 | `import { garantirSessaoAdmin } from './helpers/autenticar'` | Reutiliza a mesma função que o `app.spec.ts` usa no `beforeEach`. |
| 3 | (vazia) | — |
| 4 | Comentário | Documenta o propósito: gate antes dos testes autenticados. |
| 5 | `setup('pré-condição: ...', async ({ page, request }) => {` | Define **um** teste com nome legível em português. Recebe `page` (navegador) e `request` (cliente HTTP). |
| 6 | `await garantirSessaoAdmin(page, request)` | Executa login via API + injeção no `sessionStorage` + verifica menu Estoque. |
| 7 | `})` | Fim do teste. |

---

## Por que existe se `app.spec.ts` já faz login no `beforeEach`?

| Papel | `auth.setup.ts` | `beforeEach` do `app.spec.ts` |
|-------|-----------------|-------------------------------|
| Quando roda | **Uma vez** no início do projeto `app` | **Antes de cada** teste do `app` |
| Objetivo | Falhar cedo se API/front quebraram | Garantir sessão fresca em cada caso |
| Dependência | `app` **não roda** se falhar | Cada teste repete o fluxo |

Pense no setup como **“a porta da sala de aula”**: se a porta está trancada (API off), não adianta entrar em cada prova (`app`).

---

## Ligação com `playwright.config.ts`

```typescript
{
  name: 'setup',
  testMatch: /auth\.setup\.ts/,
},
{
  name: 'app',
  testMatch: /app\.spec\.ts/,
  dependencies: ['setup'],  // ← só roda app se setup passou
}
```

---

## O que você aprende aqui (QA)

1. **Pré-condições** não testam feature — testam **ambiente**.  
2. **Renomear `test` → `setup`** é convenção para organizar relatórios.  
3. **DRY:** uma função (`garantirSessaoAdmin`) serve setup + beforeEach.

---

## Exercício

Se a API estiver parada, qual mensagem de erro você espera ver primeiro: no `setup` ou no 3º teste do `app`? Por quê?
