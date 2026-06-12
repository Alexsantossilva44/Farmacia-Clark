import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { ADMIN } from './credenciais'

const apiBase = process.env.PLAYWRIGHT_API_URL ?? 'http://127.0.0.1:8080'

export async function tokenAdmin(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${apiBase}/api/v1/auth/token`, {
    data: { email: ADMIN.email, senha: ADMIN.senha },
  })
  expect(res.ok(), `API auth falhou: ${res.status()}`).toBeTruthy()
  const body = (await res.json()) as { token: string }
  return body.token
}

/** Gera CPF válido (11 dígitos) a partir de um seed numérico. */
export function gerarCpfValido(seed: number): string {
  const base = String(Math.abs(seed) % 1_000_000_000).padStart(9, '0')
  const digits = base.split('').map(Number)

  const calcDigito = (len: number) => {
    let sum = 0
    for (let i = 0; i < len; i++) sum += digits[i] * (len + 1 - i)
    const rest = sum % 11
    return rest < 2 ? 0 : 11 - rest
  }

  const d1 = calcDigito(9)
  digits.push(d1)
  let sum = 0
  for (let i = 0; i < 10; i++) sum += digits[i] * (11 - i)
  const rest = sum % 11
  const d2 = rest < 2 ? 0 : 11 - rest

  return `${base}${d1}${d2}`
}

/** Telefone celular BR (11 dígitos) único por seed — evita colisão entre execuções E2E. */
export function gerarTelefone(seed: number): string {
  const n = Math.abs(Math.imul(seed ^ 0x9e3779b9, 0x85ebca6b)) % 100_000_000
  return `219${String(n).padStart(8, '0')}`
}

export function formatTelefoneEsperado(digits: string): string {
  const d = digits.replace(/\D/g, '')
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
}

export async function cadastrarClienteApi(
  request: APIRequestContext,
  token: string,
  payload: {
    nome: string
    cpf: string
    telefone?: string
    email?: string
    dataNascimento?: string
    sexo?: string
  },
) {
  const res = await request.post(`${apiBase}/api/v1/clientes`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    data: {
      dataNascimento: payload.dataNascimento ?? '1990-06-15',
      sexo: payload.sexo ?? 'M',
      ...payload,
    },
  })
  expect(res.ok(), `cadastro API falhou: ${res.status()} ${await res.text()}`).toBeTruthy()
  return res.json() as Promise<{ id: string; cpf: string }>
}

export async function irParaCadastroClientes(page: Page) {
  await page.goto('/cadastros?aba=clientes')
  await expect(
    page.getByRole('heading', { name: /Cadastrar cliente|Editar cliente/i }),
  ).toBeVisible({ timeout: 15_000 })
  await liberarFormularioCliente(page)
}

/** Campos iniciam readOnly anti-autofill — primeiro clique libera digitação. */
export async function liberarFormularioCliente(page: Page) {
  await page.locator('form.form-sem-autofill').click({ position: { x: 12, y: 12 } })
}

export async function clicarNovoCadastro(page: Page) {
  await page.getByRole('button', { name: /Novo cadastro/i }).click()
  await liberarFormularioCliente(page)
}

export function botaoCadastrar(page: Page) {
  return page.getByRole('button', { name: 'Cadastrar cliente' })
}

export function secaoEndereco(page: Page) {
  return page.locator('section').filter({ has: page.getByText('Endereço', { exact: true }) })
}

export async function expectUfSelecionada(page: Page, uf: string) {
  const secao = secaoEndereco(page)
  await expect(secao.getByRole('button').filter({ hasText: new RegExp(`^${uf}$`) })).toBeVisible()
}

export async function selecionarUf(page: Page, uf: string) {
  const secao = secaoEndereco(page)
  await secao
    .locator('div')
    .filter({ has: page.getByText('UF', { exact: true }) })
    .getByRole('button')
    .click()
  await page.getByRole('listbox').getByRole('button', { name: uf, exact: true }).click()
}

export async function selecionarCidade(page: Page, cidade: string) {
  const secao = secaoEndereco(page)
  await secao.getByLabel('Cidade').click()
  await page.getByLabel('Filtrar cidades').fill(cidade)
  await page.getByRole('listbox').getByRole('button', { name: cidade, exact: true }).click()
}
