import { test, expect } from '@playwright/test'
import { garantirSessaoAdmin } from './helpers/autenticar'
import {
  botaoCadastrar,
  cadastrarClienteApi,
  clicarNovoCadastro,
  expectUfSelecionada,
  gerarCpfValido,
  gerarTelefone,
  formatTelefoneEsperado,
  selecionarUf,
  selecionarCidade,
  irParaCadastroClientes,
  liberarFormularioCliente,
  tokenAdmin,
} from './helpers/clientes-cadastro'

/**
 * Regressão E2E — correções de validação no cadastro de clientes.
 * Cobre bugs encontrados manualmente e corrigidos em validacao-cliente.ts + ClientesCadastroTab.tsx.
 */
test.describe('Cadastro de clientes — validações corrigidas', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeEach(async ({ page, request }) => {
    await garantirSessaoAdmin(page, request)
    await irParaCadastroClientes(page)
    await clicarNovoCadastro(page)
  })

  test('FRONT-01: nome remove hífens e símbolos; exige nome e sobrenome', async ({ page }) => {
    const nome = page.getByLabel('Nome completo *')

    await nome.fill('Jorge ---Macedo')
    await expect(nome).toHaveValue('Jorge Macedo')

    await nome.fill('Maria')
    await nome.blur()
    await expect(
      page.getByText(/Use apenas letras e um espaço entre nome e sobrenome/i),
    ).toBeVisible()
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('FRONT-02: data 31/02/1970 é rejeitada', async ({ page }) => {
    await page.getByLabel('Nome completo *').fill('Maria Silva')
    await page.getByLabel('CPF *').fill('52998224725')

    const data = page.getByLabel('Data de nascimento')
    await data.fill('31/02/1970')
    await data.blur()

    await expect(
      page.getByText(/Data inválida — verifique dia e mês/i),
    ).toBeVisible()
    await expect(botaoCadastrar(page)).toBeDisabled()
    await expect(page.getByText('31/02/1970')).not.toBeVisible()
  })

  test('FRONT-02b: menor de 18 anos não pode ser cadastrado', async ({ page }) => {
    await page.getByLabel('Nome completo *').fill('João Menor Teste')
    await page.getByLabel('CPF *').fill(gerarCpfValido(Date.now() + 500))

    const data = page.getByLabel('Data de nascimento')
    await data.fill('15/06/2010')
    await data.blur()

    await expect(
      page.getByText(/Cliente deve ter 18 anos ou mais para cadastro/i),
    ).toBeVisible()
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('FRONT-03: CPF já cadastrado bloqueia cadastro', async ({ page, request }) => {
    const seed = Date.now()
    const cpf = gerarCpfValido(seed)
    const token = await tokenAdmin(request)

    await cadastrarClienteApi(request, token, {
      nome: 'Ana Paula Souza',
      cpf,
      telefone: gerarTelefone(seed),
      email: `ana.${seed}@teste.local`,
    })

    await clicarNovoCadastro(page)
    await page.getByLabel('Nome completo *').fill('Outra Pessoa Teste')
    const cpfInput = page.getByLabel('CPF *')
    await cpfInput.fill(cpf)
    await cpfInput.blur()

    await expect(
      page.getByText(/CPF já cadastrado\. Use Buscar por CPF para editar\./i),
    ).toBeVisible({ timeout: 15_000 })
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('FRONT-04: telefone duplicado exibe erro no campo', async ({ page, request }) => {
    const seed = Date.now() + 1
    const telefone = gerarTelefone(seed)
    const token = await tokenAdmin(request)

    await cadastrarClienteApi(request, token, {
      nome: 'Carlos Eduardo Lima',
      cpf: gerarCpfValido(seed),
      telefone,
      email: `carlos.${seed}@teste.local`,
    })

    await clicarNovoCadastro(page)
    await page.getByLabel('Nome completo *').fill('Novo Cliente Teste')
    await page.getByLabel('CPF *').fill(gerarCpfValido(seed + 1000))

    const tel = page.getByLabel('Telefone / WhatsApp')
    await tel.fill(telefone)
    const verificacao = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/clientes/contato/disponivel')
        && r.request().method() === 'GET',
    )
    await tel.blur()
    await verificacao

    await expect(
      page.getByText(/Telefone já cadastrado em outro cliente\./i),
    ).toBeVisible({ timeout: 15_000 })
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('FRONT-05: e-mail duplicado exibe erro no campo', async ({ page, request }) => {
    const seed = Date.now() + 2
    const email = `duplicado.${seed}@teste.local`
    const token = await tokenAdmin(request)

    await cadastrarClienteApi(request, token, {
      nome: 'Fernanda Costa Ribeiro',
      cpf: gerarCpfValido(seed),
      telefone: gerarTelefone(seed + 50),
      email,
    })

    await clicarNovoCadastro(page)
    await page.getByLabel('Nome completo *').fill('Cliente Email Teste')
    await page.getByLabel('CPF *').fill(gerarCpfValido(seed + 2000))

    const mail = page.getByLabel('E-mail')
    await mail.fill(email)
    const verificacao = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/clientes/contato/disponivel')
        && r.request().method() === 'GET',
    )
    await mail.blur()
    await verificacao

    await expect(
      page.getByText(/E-mail já cadastrado em outro cliente\./i),
    ).toBeVisible({ timeout: 15_000 })
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('FRONT-06: telefone exibe máscara brasileira', async ({ page }) => {
    const tel = page.getByLabel('Telefone / WhatsApp')
    await tel.fill('2196583256')
    await expect(tel).toHaveValue('(21) 9658-3256')
  })

  test('FRONT-07: e-mail não aceita espaços na digitação', async ({ page }) => {
    const mail = page.getByLabel('E-mail')

    await mail.fill('jorge@gmail.com ')
    await expect(mail).toHaveValue('jorge@gmail.com')

    await mail.fill('')
    await mail.focus()
    await page.keyboard.type('teste@email.com ')
    await expect(mail).toHaveValue('teste@email.com')
  })

  test('FRONT-08: trocar CPF na busca limpa telefone e e-mail do cliente anterior', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 3
    const cpfOriginal = gerarCpfValido(seed)
    const cpfNovo = gerarCpfValido(seed + 3000)
    const token = await tokenAdmin(request)

    await cadastrarClienteApi(request, token, {
      nome: 'Roberto Alves Pereira',
      cpf: cpfOriginal,
      telefone: gerarTelefone(seed),
      email: `roberto.${seed}@teste.local`,
    })

    await clicarNovoCadastro(page)

    const painelBusca = page.locator('aside').filter({ hasText: 'Buscar por CPF' })
    const cpfBusca = painelBusca.getByLabel('CPF')
    await cpfBusca.fill(cpfOriginal)
    await page.getByRole('button', { name: 'Buscar' }).click()

    await expect(page.getByRole('heading', { name: 'Editar cliente' })).toBeVisible({
      timeout: 15_000,
    })
    const telEsperado = gerarTelefone(seed)
    await expect(page.getByLabel('Telefone / WhatsApp')).toHaveValue(
      formatTelefoneEsperado(telEsperado),
    )

    await cpfBusca.fill(cpfNovo)

    await expect(page.getByRole('heading', { name: 'Cadastrar cliente' })).toBeVisible()
    await expect(page.getByLabel('Telefone / WhatsApp')).toHaveValue('')
    await expect(page.getByLabel('E-mail')).toHaveValue('')
    await expect(page.getByLabel('Cidade')).toContainText(/Selecione a UF primeiro/i)
  })

  test('FRONT-09: selecionar UF RJ e município Rio de Janeiro', async ({ page }) => {
    await selecionarUf(page, 'RJ')
    await selecionarCidade(page, 'Rio de Janeiro')
    await expectUfSelecionada(page, 'RJ')
    await expect(page.getByLabel('Cidade')).toContainText('Rio de Janeiro')
  })

  test('FRONT-10: após UF MG, lista municípios e permite selecionar Belo Horizonte', async ({
    page,
  }) => {
    await selecionarUf(page, 'MG')
    await page.getByLabel('Cidade').click()
    await page.getByLabel('Filtrar cidades').fill('Belo Horizonte')
    await expect(
      page.getByRole('listbox').getByRole('button', { name: 'Belo Horizonte', exact: true }),
    ).toBeVisible()
    await page.getByRole('listbox').getByRole('button', { name: 'Belo Horizonte', exact: true }).click()
    await expect(page.getByLabel('Cidade')).toContainText('Belo Horizonte')
  })
})

test.describe('Cadastro de clientes — fluxo mínimo válido', () => {
  test('cadastra cliente novo com dados válidos', async ({ page, request }) => {
    await garantirSessaoAdmin(page, request)
    await irParaCadastroClientes(page)
    await clicarNovoCadastro(page)

    const seed = Date.now() + 99
    await page.getByLabel('Nome completo *').fill('Playwright Teste QA')
    await page.getByLabel('CPF *').fill(gerarCpfValido(seed))
    await page.getByLabel('Data de nascimento *').fill('15/06/1990')
    await page
      .locator('div')
      .filter({ has: page.getByText('Sexo *', { exact: true }) })
      .getByRole('button')
      .click()
    await page.getByRole('listbox').getByRole('button', { name: 'Masculino', exact: true }).click()
    await page.getByLabel('Telefone / WhatsApp *').fill(gerarTelefone(seed + 99))
    await page.getByLabel('E-mail *').fill(`pw.${seed}@teste.local`)
    await selecionarUf(page, 'SP')
    await selecionarCidade(page, 'Sao Paulo')
    await page.getByLabel('Logradouro *').fill('Rua Playwright Teste')
    await page.getByLabel('Bairro *').fill('Centro')
    await page.getByLabel('CEP *').fill('01310100')

    await expect(botaoCadastrar(page)).toBeEnabled({ timeout: 10_000 })

    const cadastro = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/clientes')
        && r.request().method() === 'POST'
        && r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await cadastro

    await expect(page.getByText(/Cliente cadastrado com sucesso/i)).toBeVisible({
      timeout: 15_000,
    })
  })
})
