// Cópia para estudo — arquivo original: farmacia-web/e2e/helpers/autenticar.ts
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
