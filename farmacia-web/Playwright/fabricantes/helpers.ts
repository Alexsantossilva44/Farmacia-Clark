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

export async function irParaFabricantes(page: Page) {
  await page.goto('/cadastros?aba=fabricantes')
  await expect(
    page.getByRole('heading', { name: /Novo Fabricante|Editar Fabricante/i }),
  ).toBeVisible({ timeout: 15_000 })
}

export function botaoCadastrar(page: Page) {
  return page.getByRole('button', { name: 'Cadastrar fabricante' })
}

// ─── Geradores ────────────────────────────────────────────────────────────────

/** Gera CNPJ matematicamente válido a partir de um seed. */
export function gerarCnpj(seed: number): string {
  const base = String(Math.abs(seed) % 1_000_000_000_000).padStart(12, '0')
  const digits = base.split('').map(Number)

  const w1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
  const sum1 = digits.reduce((acc, d, i) => acc + d * w1[i], 0)
  const d1 = sum1 % 11 < 2 ? 0 : 11 - (sum1 % 11)
  digits.push(d1)

  const w2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
  const sum2 = digits.reduce((acc, d, i) => acc + d * w2[i], 0)
  const d2 = sum2 % 11 < 2 ? 0 : 11 - (sum2 % 11)

  return `${base}${d1}${d2}`
}

/** Formata CNPJ para exibição: 00.000.000/0000-00 */
export function formatarCnpj(cnpj: string): string {
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
}

/** Razão social única por seed para evitar colisões entre execuções. */
export function gerarRazaoSocial(seed: number): string {
  return `Farmacorp Teste ${seed}`
}

// ─── API helpers ──────────────────────────────────────────────────────────────

export async function cadastrarFabricanteApi(
  request: APIRequestContext,
  token: string,
  payload: { razaoSocial: string; cnpj: string; nomeFantasia?: string },
) {
  const res = await request.post(`${apiBase}/api/v1/catalogo/fabricantes`, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: payload,
  })
  expect(res.ok(), `Cadastro API falhou: ${res.status()} ${await res.text()}`).toBeTruthy()
  return res.json() as Promise<{ id: string; razaoSocial: string; cnpj: string }>
}

// ─── Ações na lista ───────────────────────────────────────────────────────────

/** Clica no item da lista que contém o texto informado (ativa edição via caneta). */
export async function clicarEditarNaLista(page: Page, texto: string) {
  await page.locator('button').filter({ hasText: texto }).first().click()
}