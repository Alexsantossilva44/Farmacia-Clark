import { expect, type APIRequestContext, type Page } from '@playwright/test'

const apiBase = process.env.PLAYWRIGHT_API_URL ?? 'http://127.0.0.1:8080'
const ADMIN = { email: 'admin@farmacia.com', senha: 'admin123' }

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

export async function irParaMedicamentos(page: Page) {
  await page.goto('/cadastros?aba=medicamentos')
  await expect(
    page.getByRole('heading', { name: /Novo medicamento|Editar medicamento/i }),
  ).toBeVisible({ timeout: 15_000 })
}

export function botaoCadastrar(page: Page) {
  return page.getByRole('button', { name: 'Cadastrar medicamento' })
}

// ─── Geradores ────────────────────────────────────────────────────────────────

export function gerarNomeComercial(seed: number): string {
  return `Medicamento Teste ${seed}`
}

export function gerarNomeCategoria(seed: number): string {
  return `Cat Med Teste ${seed}`
}

export function gerarRazaoSocial(seed: number): string {
  return `Lab Med Teste ${seed}`
}

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

// ─── API helpers ──────────────────────────────────────────────────────────────

export async function criarFabricanteApi(
  request: APIRequestContext,
  token: string,
  seed: number,
): Promise<{ id: string; razaoSocial: string }> {
  const res = await request.post(`${apiBase}/api/v1/catalogo/fabricantes`, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: { razaoSocial: gerarRazaoSocial(seed), cnpj: gerarCnpj(seed) },
  })
  expect(res.ok(), `Fabricante API falhou: ${res.status()} ${await res.text()}`).toBeTruthy()
  return res.json() as Promise<{ id: string; razaoSocial: string }>
}

export async function criarCategoriaApi(
  request: APIRequestContext,
  token: string,
  seed: number,
): Promise<{ id: string; nome: string }> {
  const res = await request.post(`${apiBase}/api/v1/catalogo/categorias`, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: { nome: gerarNomeCategoria(seed), descricao: `Desc cat ${seed}` },
  })
  expect(res.ok(), `Categoria API falhou: ${res.status()} ${await res.text()}`).toBeTruthy()
  return res.json() as Promise<{ id: string; nome: string }>
}

export async function cadastrarMedicamentoApi(
  request: APIRequestContext,
  token: string,
  payload: {
    nomeComercial: string
    nomeGenerico?: string
    tipo?: string
    formaFarmaceutica?: string
    nivelControle?: string
    requerReceita?: boolean
    precoMaximoConsumidor: number
    fabricanteId: string
    categoriaId: string
    codigoEan?: string
  },
): Promise<{ id: string; nomeComercial: string }> {
  const { fabricanteId, categoriaId, ...rest } = payload
  const res = await request.post(`${apiBase}/api/v1/medicamentos`, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: {
      tipo: 'GENERICO',
      formaFarmaceutica: 'COMPRIMIDO',
      nivelControle: 'LIVRE',
      requerReceita: false,
      ...rest,
      fabricante: { id: fabricanteId },
      categoria: { id: categoriaId },
    },
  })
  expect(res.ok(), `Medicamento API falhou: ${res.status()} ${await res.text()}`).toBeTruthy()
  return res.json() as Promise<{ id: string; nomeComercial: string }>
}

// ─── Ações na lista ───────────────────────────────────────────────────────────

export async function clicarEditarNaLista(page: Page, nome: string) {
  await page.locator('button').filter({ hasText: nome }).first().click()
}