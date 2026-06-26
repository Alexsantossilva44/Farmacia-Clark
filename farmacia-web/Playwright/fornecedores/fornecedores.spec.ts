import { test, expect } from '@playwright/test'
import {
  autenticarAdmin,
  tokenAdmin,
  irParaFornecedores,
  botaoCadastrar,
  gerarCnpj,
  formatarCnpj,
  gerarRazaoSocial,
  cadastrarFornecedorApi,
  clicarEditarNaLista,
} from './helpers'

const MSG_OBRIGATORIO = 'Lembre-se: Campo Obrigatório.'

// ══════════════════════════════════════════════════════════════════════════════
// FO-01 — NAVEGAÇÃO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('FO-01 — Navegação e abertura do formulário', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
  })

  test('FO-01.1: aba Fornecedores aparece no menu de Cadastros', async ({ page }) => {
    await page.goto('/cadastros')
    await expect(page.getByRole('button', { name: /Fornecedores/i })).toBeVisible({
      timeout: 10_000,
    })
  })

  test('FO-01.2: navegar para Fornecedores exibe o formulário Novo fornecedor', async ({ page }) => {
    await irParaFornecedores(page)
    await expect(page.getByRole('heading', { name: 'Novo fornecedor' })).toBeVisible()
  })

  test('FO-01.3: formulário abre com todos os campos vazios', async ({ page }) => {
    await irParaFornecedores(page)
    await expect(page.getByLabel('Razão social *')).toHaveValue('')
    await expect(page.getByLabel('Nome fantasia')).toHaveValue('')
    await expect(page.getByLabel('CNPJ *')).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// FO-02 — CAMPO RAZÃO SOCIAL
// ══════════════════════════════════════════════════════════════════════════════
test.describe('FO-02 — Campo Razão Social', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
  })

  test('FO-02.1: campo vazio ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('Razão social *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('FO-02.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Razão social *')
    await campo.fill('@Distribuidora')
    await expect(campo).toHaveValue('')
  })

  test('FO-02.3: razão social duplicada exibe erro ao sair do campo', async ({ page, request }) => {
    const seed = Date.now() + 100
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await irParaFornecedores(page)
    const campo = page.getByLabel('Razão social *')
    await campo.fill(razaoSocial)
    await campo.blur()

    await expect(page.getByText('Razão social já cadastrada.')).toBeVisible({ timeout: 5_000 })
  })

  test('FO-02.4: razão social duplicada limpa o campo após a mensagem', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 150
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await irParaFornecedores(page)
    const campo = page.getByLabel('Razão social *')
    await campo.fill(razaoSocial)
    await campo.blur()

    await expect(page.getByText('Razão social já cadastrada.')).toBeVisible({ timeout: 5_000 })
    await expect(campo).toHaveValue('', { timeout: 5_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// FO-03 — CAMPO NOME FANTASIA
// ══════════════════════════════════════════════════════════════════════════════
test.describe('FO-03 — Campo Nome Fantasia', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
  })

  test('FO-03.1: campo é opcional — não exibe erro ao sair vazio', async ({ page }) => {
    const campo = page.getByLabel('Nome fantasia')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO)).not.toBeVisible()
  })

  test('FO-03.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Nome fantasia')
    await campo.fill('!NomeFantasia')
    await expect(campo).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// FO-04 — CAMPO CNPJ
// ══════════════════════════════════════════════════════════════════════════════
test.describe('FO-04 — Campo CNPJ', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
  })

  test('FO-04.1: aplica máscara 00.000.000/0000-00 ao digitar', async ({ page }) => {
    const campo = page.getByLabel('CNPJ *')
    await campo.fill('11222333000181')
    await expect(campo).toHaveValue('11.222.333/0001-81')
  })

  test('FO-04.2: CNPJ com dígitos inválidos exibe erro ao sair do campo', async ({ page }) => {
    const campo = page.getByLabel('CNPJ *')
    await campo.fill('00000000000000')
    await campo.blur()
    await expect(page.getByText(/CNPJ inválido/i)).toBeVisible()
  })

  test('FO-04.3: CNPJ vazio exibe mensagem obrigatório ao sair', async ({ page }) => {
    const campo = page.getByLabel('CNPJ *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('FO-04.4: CNPJ duplicado exibe erro ao sair do campo', async ({ page, request }) => {
    const seed = Date.now() + 200
    const cnpj = gerarCnpj(seed)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial: gerarRazaoSocial(seed), cnpj })

    await irParaFornecedores(page)
    await page.getByLabel('Razão social *').fill(gerarRazaoSocial(seed + 99999))
    const campo = page.getByLabel('CNPJ *')
    await campo.fill(formatarCnpj(cnpj))
    await campo.blur()

    await expect(page.getByText('CNPJ já cadastrado.')).toBeVisible({ timeout: 5_000 })
  })

  test('FO-04.5: CNPJ duplicado limpa o campo após a mensagem', async ({ page, request }) => {
    const seed = Date.now() + 250
    const cnpj = gerarCnpj(seed)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial: gerarRazaoSocial(seed), cnpj })

    await irParaFornecedores(page)
    await page.getByLabel('Razão social *').fill(gerarRazaoSocial(seed + 88888))
    const campo = page.getByLabel('CNPJ *')
    await campo.fill(formatarCnpj(cnpj))
    await campo.blur()

    await expect(page.getByText('CNPJ já cadastrado.')).toBeVisible({ timeout: 5_000 })
    await expect(campo).toHaveValue('', { timeout: 5_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// FO-05 — EDIÇÃO COM CANETA VERMELHA (funcionalidade desta sessão)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('FO-05 — Edição com caneta vermelha', () => {
  test('FO-05.1: clicar na caneta carrega os dados do fornecedor no formulário', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 300
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, {
      razaoSocial,
      cnpj: gerarCnpj(seed),
      nomeFantasia: 'Fantasia Fornecedor',
    })

    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
    await clicarEditarNaLista(page, razaoSocial)

    await expect(page.getByLabel('Razão social *')).toHaveValue(razaoSocial)
    await expect(page.getByLabel('Nome fantasia')).toHaveValue('Fantasia Fornecedor')
  })

  test('FO-05.2: título muda para "Editar fornecedor" ao selecionar item da lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 400
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
    await clicarEditarNaLista(page, razaoSocial)

    await expect(page.getByRole('heading', { name: 'Editar fornecedor' })).toBeVisible()
  })

  test('FO-05.3: salvar alterações atualiza fornecedor e exibe "Fornecedor atualizado."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 500
    const razaoSocial = gerarRazaoSocial(seed)
    const novaRazao = gerarRazaoSocial(seed + 77777)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
    await clicarEditarNaLista(page, razaoSocial)

    const campo = page.getByLabel('Razão social *')
    await campo.clear()
    await campo.fill(novaRazao)
    await page.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect(page.getByText('Fornecedor atualizado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Novo fornecedor' })).toBeVisible()
  })

  test('FO-05.4: botão Cancelar retorna ao modo Novo fornecedor com campos limpos', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 600
    const razaoSocial = gerarRazaoSocial(seed)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
    await clicarEditarNaLista(page, razaoSocial)
    await expect(page.getByRole('heading', { name: 'Editar fornecedor' })).toBeVisible()

    await page.getByRole('button', { name: 'Cancelar' }).click()

    await expect(page.getByRole('heading', { name: 'Novo fornecedor' })).toBeVisible()
    await expect(page.getByLabel('Razão social *')).toHaveValue('')
    await expect(page.getByLabel('CNPJ *')).toHaveValue('')
  })

  test('FO-05.5: editar CNPJ do fornecedor e salvar com sucesso', async ({ page, request }) => {
    const seed = Date.now() + 650
    const razaoSocial = gerarRazaoSocial(seed)
    const novoCnpj = gerarCnpj(seed + 55555)
    const token = await tokenAdmin(request)
    await cadastrarFornecedorApi(request, token, { razaoSocial, cnpj: gerarCnpj(seed) })

    await autenticarAdmin(page, request)
    await irParaFornecedores(page)
    await clicarEditarNaLista(page, razaoSocial)

    const campoCnpj = page.getByLabel('CNPJ *')
    await campoCnpj.clear()
    await campoCnpj.fill(formatarCnpj(novoCnpj))
    await page.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect(page.getByText('Fornecedor atualizado.')).toBeVisible({ timeout: 10_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// FO-06 — FLUXO COMPLETO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('FO-06 — Fluxo completo de cadastro', () => {
  test('FO-06.1: cadastrar fornecedor com todos os campos — sucesso', async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaFornecedores(page)

    const seed = Date.now() + 700
    await page.getByLabel('Razão social *').fill(gerarRazaoSocial(seed))
    await page.getByLabel('Nome fantasia').fill('Fantasia Completa Fornecedor')
    await page.getByLabel('CNPJ *').fill(formatarCnpj(gerarCnpj(seed)))

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/fornecedores') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Fornecedor cadastrado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByLabel('Razão social *')).toHaveValue('')
  })

  test('FO-06.2: cadastrar fornecedor só com campos obrigatórios (sem Nome Fantasia) — sucesso', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaFornecedores(page)

    const seed = Date.now() + 800
    await page.getByLabel('Razão social *').fill(gerarRazaoSocial(seed))
    await page.getByLabel('CNPJ *').fill(formatarCnpj(gerarCnpj(seed)))

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/fornecedores') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Fornecedor cadastrado.')).toBeVisible({ timeout: 10_000 })
  })

  test('FO-06.3: submeter formulário vazio exibe mensagem de campo obrigatório', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaFornecedores(page)

    await botaoCadastrar(page).click()

    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })
})