import { expect, type APIRequestContext, type Page } from '@playwright/test'

const apiBase = process.env.PLAYWRIGHT_API_URL ?? 'http://127.0.0.1:8080'
const ADMIN = { email: 'admin@farmaciaclark.com', senha: 'admin123' }

// ─── Auth ─────────────────────────────────────────────────────────────────────

export async function autenticarAdmin(page: Page, request: APIRequestContext) {
  const res = await request.post(`${apiBase}/api/v1/auth/token`, {
    data: { email: ADMIN.email, senha: ADMIN.senha },
  })
  expect(res.ok(), `Auth falhou: ${res.status()}`).toBeTruthy()
  const body = (await res.json()) as { token: string; expiraEmSegundos: number }

  await page.goto('/login')
  await page.evaluate(
    ({ token, expiraEmSegundos }) => {
      sessionStorage.setItem('farmacia_token', token)
      sessionStorage.setItem('farmacia_token_expires', String(Date.now() + expiraEmSegundos * 1000))
    },
    { token: body.token, expiraEmSegundos: body.expiraEmSegundos },
  )
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /Painel operacional/i })).toBeVisible({
    timeout: 15_000,
  })
}

export async function tokenAdmin(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${apiBase}/api/v1/auth/token`, {
    data: { email: ADMIN.email, senha: ADMIN.senha },
  })
  const body = (await res.json()) as { token: string }
  return body.token
}

// ─── Navegação ────────────────────────────────────────────────────────────────

export async function irParaPrescritores(page: Page) {
  await page.goto('/cadastros?aba=prescritores')
  await expect(
    page.getByRole('heading', { name: /Novo Prescritor|Editar Prescritor/i }),
  ).toBeVisible({ timeout: 15_000 })
}

export function botaoCadastrar(page: Page) {
  return page.getByRole('button', { name: 'Cadastrar prescritor' })
}

// ─── Geradores ────────────────────────────────────────────────────────────────

export function gerarNomePrescritor(seed: number): string {
  return `Dr Teste Playwright ${seed}`
}

export function gerarCrm(seed: number): string {
  return String(100000 + (seed % 900000))
}

// ─── API helpers ──────────────────────────────────────────────────────────────

export async function cadastrarPrescritorsApi(
  request: APIRequestContext,
  token: string,
  payload: { nome: string; crm: string; ufCrm: string; especialidade: string; email?: string },
) {
  const res = await request.post(`${apiBase}/api/v1/catalogo/prescritores`, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: payload,
  })
  expect(res.ok(), `Cadastro API falhou: ${res.status()} ${await res.text()}`).toBeTruthy()
  return res.json() as Promise<{ id: string; nome: string; crm: string; ufCrm: string }>
}

// ─── Ações na lista ───────────────────────────────────────────────────────────

export async function clicarEditarNaLista(page: Page, texto: string) {
  await page.locator('button').filter({ hasText: texto }).first().click()
}