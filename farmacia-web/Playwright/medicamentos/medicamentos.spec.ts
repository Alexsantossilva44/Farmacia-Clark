import { test, expect } from '@playwright/test'
import {
  autenticarAdmin,
  tokenAdmin,
  irParaMedicamentos,
  botaoCadastrar,
  gerarNomeComercial,
  criarFabricanteApi,
  criarCategoriaApi,
  cadastrarMedicamentoApi,
  clicarEditarNaLista,
} from './helpers'

const MSG_OBRIGATORIO = 'Lembre-se: Campo Obrigatório.'

// ══════════════════════════════════════════════════════════════════════════════
// MD-01 — NAVEGAÇÃO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-01 — Navegação e abertura do formulário', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
  })

  test('MD-01.1: aba Medicamentos aparece no menu de Cadastros', async ({ page }) => {
    await page.goto('/cadastros')
    await expect(page.getByRole('button', { name: /Medicamentos/i })).toBeVisible({
      timeout: 10_000,
    })
  })

  test('MD-01.2: navegar para Medicamentos exibe o formulário Novo medicamento', async ({
    page,
  }) => {
    await irParaMedicamentos(page)
    await expect(page.getByRole('heading', { name: 'Novo medicamento' })).toBeVisible()
  })

  test('MD-01.3: formulário exibe todos os campos obrigatórios', async ({ page }) => {
    await irParaMedicamentos(page)
    await expect(page.getByLabel('Nome comercial *')).toBeVisible()
    await expect(page.getByLabel('PMC (R$) *')).toBeVisible()
    await expect(page.getByLabel('Fabricante *')).toBeVisible()
    await expect(page.getByLabel('Categoria *')).toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// MD-02 — CAMPO NOME COMERCIAL
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-02 — Campo Nome comercial', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
  })

  test('MD-02.1: campo vazio ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('Nome comercial *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('MD-02.2: não aceita caractere especial no início', async ({ page }) => {
    const campo = page.getByLabel('Nome comercial *')
    await campo.fill('@Dipirona')
    await expect(campo).toHaveValue('')
  })

  test('MD-02.3: limite de 80 caracteres — exibe "Limite: Até 80 caracteres." e trunca', async ({
    page,
  }) => {
    const campo = page.getByLabel('Nome comercial *')
    const texto81 = 'Medicamento Teste Playwright Nome Comercial Longo Demais Para Caber No Campo X'

    await campo.fill(texto81.slice(0, 81))

    await expect(page.getByText('Limite: Até 80 caracteres.')).toBeVisible({ timeout: 5_000 })
    const valor = await campo.inputValue()
    expect(valor.length).toBeLessThanOrEqual(80)
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// MD-03 — CAMPO PMC
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-03 — Campo PMC (R$)', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
  })

  test('MD-03.1: campo zero ao sair exibe mensagem obrigatório', async ({ page }) => {
    const campo = page.getByLabel('PMC (R$) *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('MD-03.2: aplica máscara monetária ao digitar — exibe "R$ X,00"', async ({ page }) => {
    const campo = page.getByLabel('PMC (R$) *')
    await campo.fill('1050')
    await expect(campo).toHaveValue('R$ 10,50')
  })

  test('MD-03.3: digitar valor válido limpa o erro de obrigatório', async ({ page }) => {
    const campo = page.getByLabel('PMC (R$) *')
    await campo.focus()
    await campo.blur()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })

    await campo.fill('2500')
    await expect(page.getByText(MSG_OBRIGATORIO)).not.toBeVisible({ timeout: 3_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// MD-04 — SELECTS OBRIGATÓRIOS
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-04 — Selects obrigatórios (Fabricante e Categoria)', () => {
  test('MD-04.1: submeter sem Fabricante exibe mensagem obrigatório', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 50
    const token = await tokenAdmin(request)
    const cat = await criarCategoriaApi(request, token, seed)

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)

    await page.getByLabel('Nome comercial *').fill(gerarNomeComercial(seed))
    await page.getByLabel('PMC (R$) *').fill('1000')
    await page.getByLabel('Categoria *').selectOption(cat.id)
    // Fabricante deixado em branco

    await botaoCadastrar(page).click()

    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('MD-04.2: selecionar Fabricante e Categoria via select funciona', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 75
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)

    await page.getByLabel('Fabricante *').selectOption(fab.id)
    await page.getByLabel('Categoria *').selectOption(cat.id)

    await expect(page.getByLabel('Fabricante *')).toHaveValue(fab.id)
    await expect(page.getByLabel('Categoria *')).toHaveValue(cat.id)
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// MD-05 — NÍVEL DE CONTROLE E RECEITA
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-05 — Nível de controle e receita médica', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
  })

  test('MD-05.1: nível LIVRE não marca o checkbox "Exige receita médica"', async ({ page }) => {
    await page.getByLabel('Nível de controle *').selectOption('LIVRE')
    const checkbox = page.getByRole('checkbox', { name: /Exige receita médica/i })
    await expect(checkbox).not.toBeChecked()
  })

  test('MD-05.2: mudar para RECEITA_SIMPLES auto-marca o checkbox', async ({ page }) => {
    await page.getByLabel('Nível de controle *').selectOption('RECEITA_SIMPLES')
    const checkbox = page.getByRole('checkbox', { name: /Exige receita médica/i })
    await expect(checkbox).toBeChecked()
  })

  test('MD-05.3: mudar para CONTROLADO_C1 auto-marca o checkbox', async ({ page }) => {
    await page.getByLabel('Nível de controle *').selectOption('CONTROLADO_C1')
    const checkbox = page.getByRole('checkbox', { name: /Exige receita médica/i })
    await expect(checkbox).toBeChecked()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// MD-06 — FLUXO COMPLETO DE CADASTRO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-06 — Fluxo completo de cadastro', () => {
  test('MD-06.1: cadastrar medicamento com campos obrigatórios — sucesso', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 100
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)

    await page.getByLabel('Nome comercial *').fill(gerarNomeComercial(seed))
    await page.getByLabel('PMC (R$) *').fill('1599')
    await page.getByLabel('Fabricante *').selectOption(fab.id)
    await page.getByLabel('Categoria *').selectOption(cat.id)

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/medicamentos') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Medicamento cadastrado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByLabel('Nome comercial *')).toHaveValue('')
  })

  test('MD-06.2: cadastrar medicamento com todos os campos — sucesso', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 200
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)

    await page.getByLabel('Nome comercial *').fill(gerarNomeComercial(seed))
    await page.getByLabel('Nome genérico (DCB)').fill('Paracetamol')
    await page.getByLabel('PMC (R$) *').fill('2499')
    await page.getByLabel('Tipo *').selectOption('REFERENCIA')
    await page.getByLabel('Forma farmacêutica').selectOption('CAPSULA')
    await page.getByLabel('Concentração').fill('500mg')
    await page.getByLabel('Apresentação').fill('Caixa com 20 cápsulas')
    await page.getByLabel('Nível de controle *').selectOption('RECEITA_SIMPLES')
    await page.getByLabel('Fabricante *').selectOption(fab.id)
    await page.getByLabel('Categoria *').selectOption(cat.id)

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/medicamentos') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText('Medicamento cadastrado.')).toBeVisible({ timeout: 10_000 })
  })

  test('MD-06.3: submeter formulário vazio exibe mensagem de campo obrigatório', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)

    await botaoCadastrar(page).click()

    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('MD-06.4: medicamento cadastrado aparece na lista imediatamente', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 300
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)

    await page.getByLabel('Nome comercial *').fill(nome)
    await page.getByLabel('PMC (R$) *').fill('3000')
    await page.getByLabel('Fabricante *').selectOption(fab.id)
    await page.getByLabel('Categoria *').selectOption(cat.id)
    await botaoCadastrar(page).click()

    await expect(page.getByText('Medicamento cadastrado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('button').filter({ hasText: nome })).toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// MD-07 — EDIÇÃO COM CANETA VERMELHA
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-07 — Edição com caneta vermelha', () => {
  test('MD-07.1: clicar na caneta carrega os dados do medicamento no formulário', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 400
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)
    await cadastrarMedicamentoApi(request, token, {
      nomeComercial: nome,
      precoMaximoConsumidor: 25.5,
      fabricanteId: fab.id,
      categoriaId: cat.id,
    })

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
    await clicarEditarNaLista(page, nome)

    await expect(page.getByLabel('Nome comercial *')).toHaveValue(nome)
    await expect(page.getByLabel('PMC (R$) *')).toHaveValue('R$ 25,50')
    await expect(page.getByLabel('Fabricante *')).toHaveValue(fab.id)
    await expect(page.getByLabel('Categoria *')).toHaveValue(cat.id)
  })

  test('MD-07.2: título muda para "Editar medicamento" ao selecionar item da lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 500
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)
    await cadastrarMedicamentoApi(request, token, {
      nomeComercial: nome,
      precoMaximoConsumidor: 10.0,
      fabricanteId: fab.id,
      categoriaId: cat.id,
    })

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
    await clicarEditarNaLista(page, nome)

    await expect(page.getByRole('heading', { name: 'Editar medicamento' })).toBeVisible()
  })

  test('MD-07.3: salvar alterações atualiza medicamento e exibe "Medicamento atualizado."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 600
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)
    const novoNome = gerarNomeComercial(seed + 44444)
    await cadastrarMedicamentoApi(request, token, {
      nomeComercial: nome,
      precoMaximoConsumidor: 15.0,
      fabricanteId: fab.id,
      categoriaId: cat.id,
    })

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
    await clicarEditarNaLista(page, nome)

    const campo = page.getByLabel('Nome comercial *')
    await campo.clear()
    await campo.fill(novoNome)
    await page.getByRole('button', { name: 'Salvar alterações' }).click()

    await expect(page.getByText('Medicamento atualizado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Novo medicamento' })).toBeVisible()
  })

  test('MD-07.4: botão Cancelar retorna ao modo Novo medicamento com campos limpos', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 700
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)
    await cadastrarMedicamentoApi(request, token, {
      nomeComercial: nome,
      precoMaximoConsumidor: 8.0,
      fabricanteId: fab.id,
      categoriaId: cat.id,
    })

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
    await clicarEditarNaLista(page, nome)
    await expect(page.getByRole('heading', { name: 'Editar medicamento' })).toBeVisible()

    await page.getByRole('button', { name: 'Cancelar' }).click()

    await expect(page.getByRole('heading', { name: 'Novo medicamento' })).toBeVisible()
    await expect(page.getByLabel('Nome comercial *')).toHaveValue('')
    await expect(page.getByLabel('PMC (R$) *')).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// MD-08 — INATIVAR MEDICAMENTO (SOFT DELETE — só Admin)
// ══════════════════════════════════════════════════════════════════════════════
test.describe('MD-08 — Inativar medicamento', () => {
  test('MD-08.1: botão Inativar aparece somente no modo edição', async ({ page, request }) => {
    const seed = Date.now() + 800
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)
    await cadastrarMedicamentoApi(request, token, {
      nomeComercial: nome,
      precoMaximoConsumidor: 20.0,
      fabricanteId: fab.id,
      categoriaId: cat.id,
    })

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)

    await expect(page.getByRole('button', { name: 'Inativar' })).not.toBeVisible()

    await clicarEditarNaLista(page, nome)

    await expect(page.getByRole('button', { name: 'Inativar' })).toBeVisible()
  })

  test('MD-08.2: confirmar inativação remove item da lista e exibe "Medicamento inativado."', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 900
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)
    await cadastrarMedicamentoApi(request, token, {
      nomeComercial: nome,
      precoMaximoConsumidor: 30.0,
      fabricanteId: fab.id,
      categoriaId: cat.id,
    })

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
    await clicarEditarNaLista(page, nome)

    page.once('dialog', (dialog) => dialog.accept())
    await page.getByRole('button', { name: 'Inativar' }).click()

    await expect(page.getByText('Medicamento inativado.')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Novo medicamento' })).toBeVisible()
  })

  test('MD-08.3: cancelar diálogo de confirmação mantém o medicamento na lista', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 950
    const token = await tokenAdmin(request)
    const fab = await criarFabricanteApi(request, token, seed)
    const cat = await criarCategoriaApi(request, token, seed)
    const nome = gerarNomeComercial(seed)
    await cadastrarMedicamentoApi(request, token, {
      nomeComercial: nome,
      precoMaximoConsumidor: 12.0,
      fabricanteId: fab.id,
      categoriaId: cat.id,
    })

    await autenticarAdmin(page, request)
    await irParaMedicamentos(page)
    await clicarEditarNaLista(page, nome)

    page.once('dialog', (dialog) => dialog.dismiss())
    await page.getByRole('button', { name: 'Inativar' }).click()

    await expect(page.getByRole('heading', { name: 'Editar medicamento' })).toBeVisible()
    await expect(page.locator('button').filter({ hasText: nome })).toBeVisible()
  })
})