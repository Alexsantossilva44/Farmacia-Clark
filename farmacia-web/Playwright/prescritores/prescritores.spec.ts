import { test, expect } from '@playwright/test'
import {
  autenticarAdmin,
  tokenAdmin,
  irParaPrescritores,
  botaoCadastrar,
  gerarNomePrescritor,
  gerarCrm,
  cadastrarPrescritorsApi,
  clicarEditarNaLista,
} from './helpers'

const MSG_OBRIGATORIO = 'Lembre-se: Campo Obrigatório.'

// ══════════════════════════════════════════════════════════════════════════════
// PR-01 — NAVEGAÇÃO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('PR-01 — Navegação e abertura do formulário', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
  })

  test('PR-01.1: aba Prescritores aparece no menu de Cadastros', async ({ page }) => {
    await page.goto('/cadastros')
    await expect(page.getByRole('button', { name: /Prescritores/i })).toBeVisible({
      timeout: 10_000,
    })
  })

  test('PR-01.2: navegar para Prescritores exibe o formulário Novo Prescritor', async ({ page }) => {
    await irParaPrescritores(page)
    await expect(page.getByRole('heading', { name: 'Novo Prescritor' })).toBeVisible()
  })

  test('PR-01.3: formulário abre com campos Nome, CRM, UF e Especialidade', async ({ page }) => {
    await irParaPrescritores(page)
    await expect(page.getByLabel('Nome completo *')).toBeVisible()
    await expect(page.getByLabel('CRM *')).toBeVisible()
    await expect(page.getByLabel('UF *')).toBeVisible()
    await expect(page.getByLabel('Especialidade *')).toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// PR-02 — CAMPO NOME COMPLETO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('PR-02 — Campo Nome completo', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
  })

  test('PR-02.1: campo vazio ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('Nome completo *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('PR-02.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Nome completo *')
    await campo.fill('@Dr Silva')
    await expect(campo).toHaveValue('')
  })

  test('PR-02.3: nome duplicado exibe erro ao sair do campo', async ({ page, request }) => {
    const seed = Date.now() + 100
    const nome = gerarNomePrescritor(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Clínica Geral',
    })

    await irParaPrescritores(page)
    const campo = page.getByLabel('Nome completo *')
    await campo.fill(nome)
    await campo.blur()

    await expect(page.getByText('Nome já cadastrado.')).toBeVisible({ timeout: 5_000 })
  })

  test('PR-02.4: nome duplicado limpa o campo após a mensagem', async ({ page, request }) => {
    const seed = Date.now() + 150
    const nome = gerarNomePrescritor(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Clínica Geral',
    })

    await irParaPrescritores(page)
    const campo = page.getByLabel('Nome completo *')
    await campo.fill(nome)
    await campo.blur()

    await expect(page.getByText('Nome já cadastrado.')).toBeVisible({ timeout: 5_000 })
    await expect(campo).toHaveValue('', { timeout: 5_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// PR-03 — CAMPO CRM
// ══════════════════════════════════════════════════════════════════════════════
test.describe('PR-03 — Campo CRM', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
  })

  test('PR-03.1: campo vazio ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('CRM *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('PR-03.2: aceita apenas dígitos — letras são ignoradas', async ({ page }) => {
    const campo = page.getByLabel('CRM *')
    await campo.fill('12ABC34')
    await expect(campo).toHaveValue('1234')
  })

  test('PR-03.3: CRM + UF duplicados exibem erro ao sair do campo', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 200
    const crm = gerarCrm(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome: gerarNomePrescritor(seed),
      crm,
      ufCrm: 'SP',
      especialidade: 'Cardiologia',
    })

    await irParaPrescritores(page)
    await page.getByLabel('Nome completo *').fill(gerarNomePrescritor(seed + 99999))
    const campoCrm = page.getByLabel('CRM *')
    await campoCrm.fill(crm)
    // UF já está em SP (default)
    await campoCrm.blur()

    await expect(page.getByText('CRM já cadastrado para esta UF.')).toBeVisible({ timeout: 5_000 })
  })

  test('PR-03.4: mesmo CRM em UF diferente é permitido', async ({ page, request }) => {
    const seed = Date.now() + 250
    const crm = gerarCrm(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome: gerarNomePrescritor(seed),
      crm,
      ufCrm: 'SP',
      especialidade: 'Ortopedia',
    })

    await irParaPrescritores(page)
    await page.getByLabel('Nome completo *').fill(gerarNomePrescritor(seed + 11111))
    await page.getByLabel('Especialidade *').fill('Ortopedia')
    await page.getByLabel('UF *').selectOption('RJ')

    const campoCrm = page.getByLabel('CRM *')
    await campoCrm.fill(crm)
    await campoCrm.blur()

    await expect(page.getByText('CRM já cadastrado para esta UF.')).not.toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// PR-04 — CAMPO ESPECIALIDADE
// ══════════════════════════════════════════════════════════════════════════════
test.describe('PR-04 — Campo Especialidade', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
  })

  test('PR-04.1: campo vazio ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('Especialidade *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('PR-04.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Especialidade *')
    await campo.fill('!Cardiologia')
    await expect(campo).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// PR-05 — EDIÇÃO COM CANETA VERMELHA
// ══════════════════════════════════════════════════════════════════════════════
test.describe('PR-05 — Edição com caneta vermelha', () => {
  test('PR-05.1: clicar na caneta carrega os dados do prescritor no formulário', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 300
    const nome = gerarNomePrescritor(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'RJ',
      especialidade: 'Neurologia',
    })

    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
    await clicarEditarNaLista(page, nome)

    await expect(page.getByLabel('Nome completo *')).toHaveValue(nome)
    await expect(page.getByLabel('CRM *')).toHaveValue(gerarCrm(seed))
    await expect(page.getByLabel('UF *')).toHaveValue('RJ')
    await expect(page.getByLabel('Especialidade *')).toHaveValue('Neurologia')
  })

  test('PR-05.2: título muda para "Editar Prescritor" ao selecionar item da lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 400
    const nome = gerarNomePrescritor(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Pediatria',
    })

    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
    await clicarEditarNaLista(page, nome)

    await expect(page.getByRole('heading', { name: 'Editar Prescritor' })).toBeVisible()
  })

  test('PR-05.3: salvar alterações atualiza prescritor e exibe "Prescritor atualizado."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 500
    const nome = gerarNomePrescritor(seed)
    const novoNome = gerarNomePrescritor(seed + 55555)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Dermatologia',
    })

    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
    await clicarEditarNaLista(page, nome)

    const campo = page.getByLabel('Nome completo *')
    await campo.clear()
    await campo.fill(novoNome)
    await page.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect(page.getByText('Prescritor atualizado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Novo Prescritor' })).toBeVisible()
  })

  test('PR-05.4: botão Cancelar retorna ao modo Novo Prescritor com campos limpos', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 600
    const nome = gerarNomePrescritor(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Ginecologia',
    })

    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
    await clicarEditarNaLista(page, nome)
    await expect(page.getByRole('heading', { name: 'Editar Prescritor' })).toBeVisible()

    await page.getByRole('button', { name: 'Cancelar' }).click()

    await expect(page.getByRole('heading', { name: 'Novo Prescritor' })).toBeVisible()
    await expect(page.getByLabel('Nome completo *')).toHaveValue('')
    await expect(page.getByLabel('CRM *')).toHaveValue('')
    await expect(page.getByLabel('Especialidade *')).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// PR-06 — INATIVAR PRESCRITOR (SOFT DELETE)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('PR-06 — Inativar prescritor', () => {
  test('PR-06.1: botão Inativar aparece somente no modo edição', async ({ page, request }) => {
    const seed = Date.now() + 700
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome: gerarNomePrescritor(seed),
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Clínica Geral',
    })

    await autenticarAdmin(page, request)
    await irParaPrescritores(page)

    await expect(page.getByRole('button', { name: 'Inativar' })).not.toBeVisible()

    await clicarEditarNaLista(page, gerarNomePrescritor(seed))

    await expect(page.getByRole('button', { name: 'Inativar' })).toBeVisible()
  })

  test('PR-06.2: confirmar inativação remove item da lista e exibe "Prescritor inativado."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 800
    const nome = gerarNomePrescritor(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Clínica Geral',
    })

    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
    await clicarEditarNaLista(page, nome)

    page.once('dialog', (dialog) => dialog.accept())
    await page.getByRole('button', { name: 'Inativar' }).click()

    await expect(page.getByText('Prescritor inativado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Novo Prescritor' })).toBeVisible()
  })

  test('PR-06.3: cancelar diálogo de confirmação mantém o prescritor na lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 850
    const nome = gerarNomePrescritor(seed)
    const token = await tokenAdmin(request)
    await cadastrarPrescritorsApi(request, token, {
      nome,
      crm: gerarCrm(seed),
      ufCrm: 'SP',
      especialidade: 'Clínica Geral',
    })

    await autenticarAdmin(page, request)
    await irParaPrescritores(page)
    await clicarEditarNaLista(page, nome)

    page.once('dialog', (dialog) => dialog.dismiss())
    await page.getByRole('button', { name: 'Inativar' }).click()

    await expect(page.getByRole('heading', { name: 'Editar Prescritor' })).toBeVisible()
    await expect(page.locator('button').filter({ hasText: nome })).toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// PR-07 — FLUXO COMPLETO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('PR-07 — Fluxo completo de cadastro', () => {
  test('PR-07.1: cadastrar prescritor com todos os campos — sucesso', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaPrescritores(page)

    const seed = Date.now() + 900
    await page.getByLabel('Nome completo *').fill(gerarNomePrescritor(seed))
    await page.getByLabel('CRM *').fill(gerarCrm(seed))
    await page.getByLabel('UF *').selectOption('MG')
    await page.getByLabel('Especialidade *').fill('Oncologia')

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/catalogo/prescritores') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Cadastro realizado com sucesso.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByLabel('Nome completo *')).toHaveValue('')
  })

  test('PR-07.2: submeter formulário vazio exibe mensagem de campo obrigatório', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaPrescritores(page)

    await botaoCadastrar(page).click()

    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('PR-07.3: prescritor cadastrado aparece na lista imediatamente', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaPrescritores(page)

    const seed = Date.now() + 950
    const nome = gerarNomePrescritor(seed)
    await page.getByLabel('Nome completo *').fill(nome)
    await page.getByLabel('CRM *').fill(gerarCrm(seed))
    await page.getByLabel('UF *').selectOption('BA')
    await page.getByLabel('Especialidade *').fill('Psiquiatria')
    await botaoCadastrar(page).click()

    await expect(page.getByText('Cadastro realizado com sucesso.')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('button').filter({ hasText: nome })).toBeVisible()
  })
})