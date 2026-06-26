import { test, expect } from '@playwright/test'
import {
  autenticarAdmin,
  tokenAdmin,
  irParaFabricantes,
  botaoCadastrar,
  gerarCnpj,
  formatarCnpj,
  gerarRazaoSocial,
  cadastrarFabricanteApi,
  clicarEditarNaLista,
} from './helpers'

const MSG_OBRIGATORIO = 'Lembre-se: Campo Obrigatório.'

// ══════════════════════════════════════════════════════════════════════════════
// F-01 — NAVEGAÇÃO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('F-01 — Navegação e abertura do formulário', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
  })

  test('F-01.1: aba Fabricantes aparece no menu de Cadastros', async ({ page }) => {
    await page.goto('/cadastros')
    await expect(page.getByRole('button', { name: /Fabricantes/i })).toBeVisible({
      timeout: 10_000,
    })
  })

  test('F-01.2: navegar para Fabricantes exibe o formulário Novo Fabricante', async ({ page }) => {
    await irParaFabricantes(page)
    await expect(page.getByRole('heading', { name: 'Novo Fabricante' })).toBeVisible()
  })

  test('F-01.3: formulário abre com todos os campos vazios', async ({ page }) => {
    await irParaFabricantes(page)
    await expect(page.getByLabel('Razão social *')).toHaveValue('')
    await expect(page.getByLabel('Nome fantasia')).toHaveValue('')
    await expect(page.getByLabel('CNPJ *')).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// F-02 — CAMPO RAZÃO SOCIAL
// ══════════════════════════════════════════════════════════════════════════════
test.describe('F-02 — Campo Razão Social', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFabricantes(page)
  })

  test('F-02.1: campo vazio ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('Razão social *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('F-02.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Razão social *')
    await campo.fill('@EmpresaTeste')
    await expect(campo).toHaveValue('')
  })

  test('F-02.3: limite de 80 caracteres — exibe mensagem e trunca', async ({ page }) => {
    const campo = page.getByLabel('Razão social *')
    const texto81 = 'Empresa Teste Playwright LTDA Cadastro Automatizado Farmacia Clark Brasil SA XY'

    await campo.fill(texto81.slice(0, 81))

    await expect(page.getByText('Limite: Até 80 caracteres.')).toBeVisible({ timeout: 5_000 })
    const valor = await campo.inputValue()
    expect(valor.length).toBeLessThanOrEqual(80)
  })

  test('F-02.4: mensagem de limite desaparece após 3 segundos', async ({ page }) => {
    const campo = page.getByLabel('Razão social *')
    const texto81 = 'Empresa Teste Playwright LTDA Cadastro Automatizado Farmacia Clark Brasil SA XY'

    await campo.fill(texto81.slice(0, 81))
    await expect(page.getByText('Limite: Até 80 caracteres.')).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText('Limite: Até 80 caracteres.')).not.toBeVisible({ timeout: 5_000 })
  })

  test('F-02.5: razão social duplicada exibe erro ao sair do campo', async ({ page, request }) => {
    const seed = Date.now() + 100
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFabricanteApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await irParaFabricantes(page)
    const campo = page.getByLabel('Razão social *')
    await campo.fill(razaoSocial)
    await campo.blur()

    await expect(page.getByText('Razão social já cadastrada.')).toBeVisible({ timeout: 5_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// F-03 — CAMPO NOME FANTASIA
// ══════════════════════════════════════════════════════════════════════════════
test.describe('F-03 — Campo Nome Fantasia', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFabricantes(page)
  })

  test('F-03.1: campo é opcional — não exibe erro ao sair vazio', async ({ page }) => {
    const campo = page.getByLabel('Nome fantasia')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO)).not.toBeVisible()
  })

  test('F-03.2: limite de 80 caracteres no Nome Fantasia', async ({ page }) => {
    const campo = page.getByLabel('Nome fantasia')
    const texto81 = 'Empresa Teste Playwright LTDA Cadastro Automatizado Farmacia Clark Brasil SA XY'

    await campo.fill(texto81.slice(0, 81))

    await expect(page.getByText('Limite: Até 80 caracteres.')).toBeVisible({ timeout: 5_000 })
    const valor = await campo.inputValue()
    expect(valor.length).toBeLessThanOrEqual(80)
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// F-04 — CAMPO CNPJ
// ══════════════════════════════════════════════════════════════════════════════
test.describe('F-04 — Campo CNPJ', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFabricantes(page)
  })

  test('F-04.1: aplica máscara 00.000.000/0000-00 ao digitar', async ({ page }) => {
    const campo = page.getByLabel('CNPJ *')
    await campo.fill('11222333000181')
    await expect(campo).toHaveValue('11.222.333/0001-81')
  })

  test('F-04.2: CNPJ com dígitos inválidos exibe erro ao sair do campo', async ({ page }) => {
    const campo = page.getByLabel('CNPJ *')
    await campo.fill('00000000000000')
    await campo.blur()
    await expect(page.getByText(/CNPJ inválido/i)).toBeVisible()
  })

  test('F-04.3: CNPJ vazio exibe mensagem obrigatório ao sair', async ({ page }) => {
    const campo = page.getByLabel('CNPJ *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('F-04.4: CNPJ já cadastrado exibe erro após tentar salvar', async ({ page, request }) => {
    const seed = Date.now() + 200
    const cnpj = gerarCnpj(seed)
    const token = await tokenAdmin(request)
    await cadastrarFabricanteApi(request, token, { razaoSocial: gerarRazaoSocial(seed), cnpj })

    await irParaFabricantes(page)
    await page.getByLabel('Razão social *').fill(gerarRazaoSocial(seed + 99999))
    await page.getByLabel('CNPJ *').fill(formatarCnpj(cnpj))
    await botaoCadastrar(page).click()

    await expect(page.getByText('CNPJ já cadastrado.')).toBeVisible({ timeout: 10_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// F-05 — EDIÇÃO COM CANETA VERMELHA (funcionalidade desta sessão)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('F-05 — Edição com caneta vermelha', () => {
  test('F-05.1: clicar na caneta carrega os dados do fabricante no formulário', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 300
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFabricanteApi(request, token, {
      razaoSocial,
      cnpj: gerarCnpj(seed),
      nomeFantasia: 'Fantasia Teste',
    })

    await autenticarAdmin(page, request)
    await irParaFabricantes(page)
    await clicarEditarNaLista(page, razaoSocial)

    await expect(page.getByLabel('Razão social *')).toHaveValue(razaoSocial)
    await expect(page.getByLabel('Nome fantasia')).toHaveValue('Fantasia Teste')
  })

  test('F-05.2: título muda para "Editar Fabricante" ao selecionar item da lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 400
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFabricanteApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await autenticarAdmin(page, request)
    await irParaFabricantes(page)
    await clicarEditarNaLista(page, razaoSocial)

    await expect(page.getByRole('heading', { name: 'Editar Fabricante' })).toBeVisible()
  })

  test('F-05.3: salvar alterações atualiza fabricante e exibe "Fabricante atualizado."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 500
    const razaoSocial = gerarRazaoSocial(seed)
    const novaRazao = gerarRazaoSocial(seed + 77777)
    const token = await tokenAdmin(request)
    await cadastrarFabricanteApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await autenticarAdmin(page, request)
    await irParaFabricantes(page)
    await clicarEditarNaLista(page, razaoSocial)

    const campo = page.getByLabel('Razão social *')
    await campo.clear()
    await campo.fill(novaRazao)
    await page.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect(page.getByText('Fabricante atualizado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Novo Fabricante' })).toBeVisible()
  })

  test('F-05.4: botão Cancelar retorna ao modo Novo Fabricante com campos limpos', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 600
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFabricanteApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await autenticarAdmin(page, request)
    await irParaFabricantes(page)
    await clicarEditarNaLista(page, razaoSocial)
    await expect(page.getByRole('heading', { name: 'Editar Fabricante' })).toBeVisible()

    await page.getByRole('button', { name: 'Cancelar' }).click()

    await expect(page.getByRole('heading', { name: 'Novo Fabricante' })).toBeVisible()
    await expect(page.getByLabel('Razão social *')).toHaveValue('')
    await expect(page.getByLabel('CNPJ *')).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// F-06 — FLUXO COMPLETO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('F-06 — Fluxo completo de cadastro', () => {
  test('F-06.1: cadastrar fabricante com todos os campos — sucesso', async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFabricantes(page)

    const seed = Date.now() + 700
    await page.getByLabel('Razão social *').fill(gerarRazaoSocial(seed))
    await page.getByLabel('Nome fantasia').fill('Fantasia Completa Teste')
    await page.getByLabel('CNPJ *').fill(formatarCnpj(gerarCnpj(seed)))

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/catalogo/fabricantes') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Cadastro realizado com sucesso.')).toBeVisible({
      timeout: 10_000,
    })
    await expect(page.getByLabel('Razão social *')).toHaveValue('')
  })

  test('F-06.2: cadastrar fabricante só com campos obrigatórios (sem Nome Fantasia) — sucesso', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaFabricantes(page)

    const seed = Date.now() + 800
    await page.getByLabel('Razão social *').fill(gerarRazaoSocial(seed))
    await page.getByLabel('CNPJ *').fill(formatarCnpj(gerarCnpj(seed)))

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/catalogo/fabricantes') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Cadastro realizado com sucesso.')).toBeVisible({
      timeout: 10_000,
    })
  })

  test('F-06.3: submeter formulário vazio exibe mensagem de campo obrigatório', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaFabricantes(page)

    await botaoCadastrar(page).click()

    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })
})