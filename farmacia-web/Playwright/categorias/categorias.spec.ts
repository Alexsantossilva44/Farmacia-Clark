import { test, expect } from '@playwright/test'
import {
  autenticarAdmin,
  tokenAdmin,
  irParaCategorias,
  botaoCadastrar,
  gerarNomeCategoria,
  gerarDescricao,
  cadastrarCategoriaApi,
  clicarEditarNaLista,
} from './helpers'

const MSG_OBRIGATORIO = 'Lembre-se: Campo Obrigatório.'

// ══════════════════════════════════════════════════════════════════════════════
// CA-01 — NAVEGAÇÃO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('CA-01 — Navegação e abertura do formulário', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
  })

  test('CA-01.1: aba Categorias aparece no menu de Cadastros', async ({ page }) => {
    await page.goto('/cadastros')
    await expect(page.getByRole('button', { name: /Categorias/i })).toBeVisible({
      timeout: 10_000,
    })
  })

  test('CA-01.2: navegar para Categorias exibe o formulário Nova Categoria', async ({ page }) => {
    await irParaCategorias(page)
    await expect(page.getByRole('heading', { name: 'Nova Categoria' })).toBeVisible()
  })

  test('CA-01.3: formulário abre com todos os campos vazios', async ({ page }) => {
    await irParaCategorias(page)
    await expect(page.getByLabel('Nome da categoria *')).toHaveValue('')
    await expect(page.getByLabel('Descrição *')).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// CA-02 — CAMPO NOME DA CATEGORIA
// ══════════════════════════════════════════════════════════════════════════════
test.describe('CA-02 — Campo Nome da Categoria', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCategorias(page)
  })

  test('CA-02.1: campo vazio ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('Nome da categoria *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('CA-02.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Nome da categoria *')
    await campo.fill('@Antibióticos')
    await expect(campo).toHaveValue('')
  })

  test('CA-02.3: nome duplicado exibe erro ao sair do campo', async ({ page, request }) => {
    const seed = Date.now() + 100
    const nome = gerarNomeCategoria(seed)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao: gerarDescricao(seed) })

    await irParaCategorias(page)
    const campo = page.getByLabel('Nome da categoria *')
    await campo.fill(nome)
    await campo.blur()

    await expect(page.getByText('Nome de categoria já cadastrado.')).toBeVisible({ timeout: 5_000 })
  })

  test('CA-02.4: nome duplicado limpa o campo após a mensagem', async ({ page, request }) => {
    const seed = Date.now() + 150
    const nome = gerarNomeCategoria(seed)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao: gerarDescricao(seed) })

    await irParaCategorias(page)
    const campo = page.getByLabel('Nome da categoria *')
    await campo.fill(nome)
    await campo.blur()

    await expect(page.getByText('Nome de categoria já cadastrado.')).toBeVisible({ timeout: 5_000 })
    await expect(campo).toHaveValue('', { timeout: 5_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// CA-03 — CAMPO DESCRIÇÃO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('CA-03 — Campo Descrição', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCategorias(page)
  })

  test('CA-03.1: descrição vazia ao sair exibe obrigatório no modo cadastro', async ({ page }) => {
    const campo = page.getByLabel('Descrição *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('CA-03.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Descrição *')
    await campo.fill('!Medicamentos para dor')
    await expect(campo).toHaveValue('')
  })

  test('CA-03.3: descrição é opcional no modo editar — label muda para "Descrição"', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 200
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, {
      nome: gerarNomeCategoria(seed),
      descricao: gerarDescricao(seed),
    })

    await irParaCategorias(page)
    await clicarEditarNaLista(page, gerarNomeCategoria(seed))

    // No modo editar o label não tem asterisco
    await expect(page.getByLabel('Descrição')).toBeVisible()
    const campo = page.getByLabel('Descrição')
    await campo.clear()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO)).not.toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// CA-04 — EDIÇÃO COM CANETA VERMELHA
// ══════════════════════════════════════════════════════════════════════════════
test.describe('CA-04 — Edição com caneta vermelha', () => {
  test('CA-04.1: clicar na caneta carrega os dados da categoria no formulário', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 300
    const nome = gerarNomeCategoria(seed)
    const descricao = gerarDescricao(seed)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)
    await clicarEditarNaLista(page, nome)

    await expect(page.getByLabel('Nome da categoria *')).toHaveValue(nome)
    await expect(page.getByLabel('Descrição')).toHaveValue(descricao)
  })

  test('CA-04.2: título muda para "Editar Categoria" ao selecionar item da lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 400
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, {
      nome: gerarNomeCategoria(seed),
      descricao: gerarDescricao(seed),
    })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)
    await clicarEditarNaLista(page, gerarNomeCategoria(seed))

    await expect(page.getByRole('heading', { name: 'Editar Categoria' })).toBeVisible()
  })

  test('CA-04.3: salvar alterações atualiza categoria e exibe "Categoria atualizada."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 500
    const nome = gerarNomeCategoria(seed)
    const novoNome = gerarNomeCategoria(seed + 66666)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao: gerarDescricao(seed) })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)
    await clicarEditarNaLista(page, nome)

    const campo = page.getByLabel('Nome da categoria *')
    await campo.clear()
    await campo.fill(novoNome)
    await page.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect(page.getByText('Categoria atualizada.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Nova Categoria' })).toBeVisible()
  })

  test('CA-04.4: botão Cancelar retorna ao modo Nova Categoria com campos limpos', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 600
    const nome = gerarNomeCategoria(seed)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao: gerarDescricao(seed) })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)
    await clicarEditarNaLista(page, nome)
    await expect(page.getByRole('heading', { name: 'Editar Categoria' })).toBeVisible()

    await page.getByRole('button', { name: 'Cancelar' }).click()

    await expect(page.getByRole('heading', { name: 'Nova Categoria' })).toBeVisible()
    await expect(page.getByLabel('Nome da categoria *')).toHaveValue('')
    await expect(page.getByLabel('Descrição *')).toHaveValue('')
  })

  test('CA-04.5: editar descrição e salvar com sucesso', async ({ page, request }) => {
    const seed = Date.now() + 650
    const nome = gerarNomeCategoria(seed)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao: gerarDescricao(seed) })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)
    await clicarEditarNaLista(page, nome)

    const campoDesc = page.getByLabel('Descrição')
    await campoDesc.clear()
    await campoDesc.fill('Nova descrição atualizada pelo teste')
    await page.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect(page.getByText('Categoria atualizada.')).toBeVisible({ timeout: 10_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// CA-05 — INATIVAR CATEGORIA (SOFT DELETE)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('CA-05 — Inativar categoria', () => {
  test('CA-05.1: botão Inativar aparece somente no modo edição', async ({ page, request }) => {
    const seed = Date.now() + 700
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, {
      nome: gerarNomeCategoria(seed),
      descricao: gerarDescricao(seed),
    })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)

    // No modo criar: sem botão Inativar
    await expect(page.getByRole('button', { name: 'Inativar' })).not.toBeVisible()

    await clicarEditarNaLista(page, gerarNomeCategoria(seed))

    // No modo editar: botão Inativar visível
    await expect(page.getByRole('button', { name: 'Inativar' })).toBeVisible()
  })

  test('CA-05.2: confirmar inativação remove item da lista e exibe "Categoria inativada."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 800
    const nome = gerarNomeCategoria(seed)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao: gerarDescricao(seed) })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)
    await clicarEditarNaLista(page, nome)

    page.once('dialog', (dialog) => dialog.accept())
    await page.getByRole('button', { name: 'Inativar' }).click()

    await expect(page.getByText('Categoria inativada.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Nova Categoria' })).toBeVisible()
  })

  test('CA-05.3: cancelar diálogo de confirmação mantém a categoria na lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 850
    const nome = gerarNomeCategoria(seed)
    const token = await tokenAdmin(request)
    await cadastrarCategoriaApi(request, token, { nome, descricao: gerarDescricao(seed) })

    await autenticarAdmin(page, request)
    await irParaCategorias(page)
    await clicarEditarNaLista(page, nome)

    page.once('dialog', (dialog) => dialog.dismiss())
    await page.getByRole('button', { name: 'Inativar' }).click()

    await expect(page.getByRole('heading', { name: 'Editar Categoria' })).toBeVisible()
    await expect(page.locator('button').filter({ hasText: nome })).toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// CA-06 — FLUXO COMPLETO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('CA-06 — Fluxo completo de cadastro', () => {
  test('CA-06.1: cadastrar categoria com todos os campos — sucesso', async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCategorias(page)

    const seed = Date.now() + 900
    await page.getByLabel('Nome da categoria *').fill(gerarNomeCategoria(seed))
    await page.getByLabel('Descrição *').fill(gerarDescricao(seed))

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/catalogo/categorias') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Cadastro realizado com sucesso.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByLabel('Nome da categoria *')).toHaveValue('')
  })

  test('CA-06.2: submeter formulário vazio exibe mensagem de campo obrigatório', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaCategorias(page)

    await botaoCadastrar(page).click()

    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('CA-06.3: categoria cadastrada aparece na lista imediatamente', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaCategorias(page)

    const seed = Date.now() + 950
    const nome = gerarNomeCategoria(seed)
    await page.getByLabel('Nome da categoria *').fill(nome)
    await page.getByLabel('Descrição *').fill(gerarDescricao(seed))
    await botaoCadastrar(page).click()

    await expect(page.getByText('Cadastro realizado com sucesso.')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('button').filter({ hasText: nome })).toBeVisible()
  })
})