import { expect, type APIRequestContext, type Page } from '@playwright/test'

const apiBase = process.env.PLAYWRIGHT_API_URL ?? 'http://127.0.0.1:8080'

// ─── Auth ─────────────────────────────────────────────────────────────────────

const ADMIN = { email: 'admin@farmaciaclark.com', senha: 'admin123' }

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
      sessionStorage.setItem(
        'farmacia_token_expires',
        String(Date.now() + expiraEmSegundos * 1000),
      )
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

export async function irParaCadastroClientes(page: Page) {
  await page.goto('/cadastros?aba=clientes')
  await expect(
    page.getByRole('heading', { name: /Cadastrar cliente|Editar cliente/i }),
  ).toBeVisible({ timeout: 15_000 })
  await liberarFormulario(page)
}

/** Clica em Novo Cadastro e libera o form anti-autofill. */
export async function novoCadastro(page: Page) {
  await page.getByRole('button', { name: /Novo cadastro/i }).click()
  await liberarFormulario(page)
}

/** O form inicia em readOnly para bloquear autofill — um clique libera os campos. */
export async function liberarFormulario(page: Page) {
  await page.locator('form.form-sem-autofill').click({ position: { x: 12, y: 12 } })
}

export function botaoCadastrar(page: Page) {
  return page.getByRole('button', { name: 'Cadastrar cliente' })
}

// ─── Geradores ────────────────────────────────────────────────────────────────

/** Gera CPF matematicamente válido a partir de um seed. */
export function gerarCpf(seed: number): string {
  const base = String(Math.abs(seed) % 1_000_000_000).padStart(9, '0')
  const digits = base.split('').map(Number)
  const d1 = calcDigito(digits, 9)
  digits.push(d1)
  const d2 = calcDigito(digits, 10)
  return `${base}${d1}${d2}`
}

function calcDigito(digits: number[], len: number): number {
  let sum = 0
  for (let i = 0; i < len; i++) sum += digits[i] * (len + 1 - i)
  const rest = sum % 11
  return rest < 2 ? 0 : 11 - rest
}

/** Telefone celular BR com 11 dígitos único por seed. */
export function gerarTelefone(seed: number): string {
  const n = Math.abs(Math.imul(seed ^ 0x9e3779b9, 0x85ebca6b)) % 100_000_000
  return `219${String(n).padStart(8, '0')}`
}

/** Formata número de telefone para o padrão exibido na UI: (21) 99999-9999. */
export function formatarTelefone(digits: string): string {
  const d = digits.replace(/\D/g, '')
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
}

// ─── API helpers ──────────────────────────────────────────────────────────────

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
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: {
      dataNascimento: payload.dataNascimento ?? '1990-06-15',
      sexo: payload.sexo ?? 'M',
      ...payload,
    },
  })
  expect(res.ok(), `Cadastro API falhou: ${res.status()} ${await res.text()}`).toBeTruthy()
  return res.json() as Promise<{ id: string; cpf: string }>
}

// ─── UF / Cidade ──────────────────────────────────────────────────────────────

export async function selecionarUf(page: Page, uf: string) {
  const secao = page.locator('section').filter({ has: page.getByText('Endereço', { exact: true }) })
  await secao
    .locator('div')
    .filter({ has: page.getByText('UF', { exact: true }) })
    .getByRole('button')
    .click()
  await page.getByRole('listbox').getByRole('button', { name: uf, exact: true }).click()
}

export async function selecionarCidade(page: Page, cidade: string) {
  const secao = page.locator('section').filter({ has: page.getByText('Endereço', { exact: true }) })
  await secao.getByLabel('Cidade').click()
  await page.getByLabel('Filtrar cidades').fill(cidade)
  await page.getByRole('listbox').getByRole('button', { name: cidade, exact: true }).click()
}

/** Preenche todos os campos de endereço com dados válidos. */
export async function preencherEndereco(page: Page, uf = 'SP', cidade = 'Sao Paulo') {
  await selecionarUf(page, uf)
  await selecionarCidade(page, cidade)
  await page.getByLabel('Logradouro *').fill('Rua dos Testes Automatizados')
  await page.getByLabel('Bairro *').fill('Centro')
  await page.getByLabel('CEP *').fill('01310100')
}

/** Preenche os campos pessoais e de contato obrigatórios. */
export async function preencherDadosPessoais(
  page: Page,
  seed: number,
  opts: { nome?: string; sexo?: string } = {},
) {
  await page.getByLabel('Nome completo *').fill(opts.nome ?? 'Playwright Teste QA')
  await page.getByLabel('CPF *').fill(gerarCpf(seed))
  await page.getByLabel('Data de nascimento *').fill('15/06/1990')
  await page
    .locator('div')
    .filter({ has: page.getByText('Sexo *', { exact: true }) })
    .getByRole('button')
    .click()
  await page
    .getByRole('listbox')
    .getByRole('button', { name: opts.sexo ?? 'Masculino', exact: true })
    .click()
  await page.getByLabel('Telefone / WhatsApp *').fill(gerarTelefone(seed))
  await page.getByLabel('E-mail *').fill(`pw.${seed}@teste.local`)
}