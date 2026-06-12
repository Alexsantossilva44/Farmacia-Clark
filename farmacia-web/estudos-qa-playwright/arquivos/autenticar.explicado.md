# autenticar.ts — linha por linha

**Arquivo real:** `e2e/helpers/autenticar.ts`  
**Função:** Deixar o navegador **logado como admin** antes dos testes do `app.spec.ts`.

---

## Código completo

```typescript
import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { ADMIN, TOKEN_EXPIRES_KEY, TOKEN_KEY } from './credenciais'
import { linkMenu } from './navegacao'

const apiBase = process.env.PLAYWRIGHT_API_URL ?? 'http://127.0.0.1:8080'

/** Garante JWT no sessionStorage (API + injeção — estável para E2E). */
export async function garantirSessaoAdmin(page: Page, request: APIRequestContext) {
  const res = await request.post(`${apiBase}/api/v1/auth/token`, {
    data: { email: ADMIN.email, senha: ADMIN.senha },
  })
  expect(res.ok(), `API auth falhou: ${res.status()}`).toBeTruthy()

  const body = (await res.json()) as { token: string; expiraEmSegundos: number }

  await page.goto('/login')
  await page.evaluate(
    ({ token, expiraEmSegundos, tokenKey, expiresKey }) => {
      sessionStorage.setItem(tokenKey, token)
      sessionStorage.setItem(
        expiresKey,
        String(Date.now() + expiraEmSegundos * 1000),
      )
    },
    {
      token: body.token,
      expiraEmSegundos: body.expiraEmSegundos,
      tokenKey: TOKEN_KEY,
      expiresKey: TOKEN_EXPIRES_KEY,
    },
  )

  await page.goto('/')
  await expect(linkMenu(page, 'Estoque')).toBeVisible({ timeout: 15_000 })
}
```

---

## Linha por linha

| Linha | O que faz |
|-------|-----------|
| **1** | Importa `expect` (verificações) e tipos `Page` (navegador) e `APIRequestContext` (HTTP). |
| **2** | Importa usuário, senha e nomes das chaves do `sessionStorage`. |
| **3** | Importa helper do menu lateral. |
| **4** | (vazia) |
| **5** | URL da API: variável de ambiente ou padrão `127.0.0.1:8080` (evita problema IPv6 no Windows). |
| **6** | (vazia) |
| **7** | Comentário JSDoc: propósito da função. |
| **8** | Declara função **assíncrona** exportada; recebe `page` e `request` do Playwright. |
| **9–11** | **POST** na API de login com email/senha do `ADMIN`. |
| **12** | Assert: status HTTP deve ser 2xx; senão mensagem com código de erro. |
| **13** | (vazia) |
| **14** | Lê JSON da resposta; TypeScript espera `token` e `expiraEmSegundos`. |
| **15** | (vazia) |
| **16** | Abre `/login` no front para estar na **mesma origem** (domínio/porta) antes do storage. |
| **17–31** | `page.evaluate`: executa código **dentro do navegador** para gravar token e expiração no `sessionStorage` (igual `saveToken` do app). |
| **32** | (vazia) |
| **33** | Navega para o painel `/`. |
| **34** | Confirma que o login “pegou”: link Estoque visível em até 15 segundos. |

---

## Por que login pela API e não pelo botão?

| Login pela tela | Login pela API (`autenticar.ts`) |
|-----------------|----------------------------------|
| Testa o fluxo visual | Prepara estado rapidamente |
| Mais lento | Mais estável |
| Usado em `login.spec.ts` | Usado em `app.spec.ts` |

**Separação de responsabilidades:** um arquivo testa login; outro testa estoque/PDV.

---

## `page.evaluate` em detalhe

- Código da **função** (linhas 18–23) roda no **browser**.  
- Objeto da **linha 25–30** é copiado do Node para o browser (tem que ser JSON-serializável).  
- Por isso passamos `tokenKey` e `expiresKey` como strings, não importamos constantes dentro do browser.

---

## Exercício

Reescreva em português o fluxo em 5 bullets, como se explicasse para um colega de QA.
