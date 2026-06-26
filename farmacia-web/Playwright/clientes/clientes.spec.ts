import { test, expect } from '@playwright/test'
import {
  autenticarAdmin,
  tokenAdmin,
  irParaCadastroClientes,
  novoCadastro,
  botaoCadastrar,
  gerarCpf,
  gerarTelefone,
  cadastrarClienteApi,
  selecionarUf,
  selecionarCidade,
  preencherEndereco,
  preencherDadosPessoais,
} from './helpers'

const MSG_OBRIGATORIO = 'Lembre-se: Campo Obrigatório.'

// ══════════════════════════════════════════════════════════════════════════════
// C-01 — NAVEGAÇÃO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-01 — Navegação e abertura do formulário', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
  })

  test('C-01.1: menu Cadastros abre na aba Clientes por padrão', async ({ page }) => {
    await page.goto('/cadastros')
    await expect(
      page.getByRole('heading', { name: /Cadastrar cliente|Editar cliente/i }),
    ).toBeVisible({ timeout: 15_000 })
  })

  test('C-01.2: aba Clientes é a primeira do menu de Cadastros', async ({ page }) => {
    await page.goto('/cadastros')
    const tabs = page.locator('nav.page-tabs button')
    await expect(tabs.first()).toContainText('Clientes')
  })

  test('C-01.3: botão Novo Cadastro exibe formulário limpo', async ({ page }) => {
    await irParaCadastroClientes(page)
    await novoCadastro(page)
    await expect(page.getByRole('heading', { name: /Cadastrar cliente/i })).toBeVisible()
    await expect(page.getByLabel('Nome completo *')).toHaveValue('')
    await expect(page.getByLabel('CPF *')).toHaveValue('')
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-02 — NOME COMPLETO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-02 — Campo Nome Completo', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-02.1: remove símbolos e hífens mantendo apenas letras', async ({ page }) => {
    const campo = page.getByLabel('Nome completo *')
    await campo.fill('Jorge ---Macedo')
    await expect(campo).toHaveValue('Jorge Macedo')
  })

  test('C-02.2: remove números do nome', async ({ page }) => {
    const campo = page.getByLabel('Nome completo *')
    await campo.fill('Ana123 Silva')
    await expect(campo).toHaveValue('Ana Silva')
  })

  test('C-02.3: somente um nome exibe erro ao sair do campo', async ({ page }) => {
    const campo = page.getByLabel('Nome completo *')
    await campo.fill('Maria')
    await campo.blur()
    await expect(
      page.getByText(/Use apenas letras e um espaço entre nome e sobrenome/i),
    ).toBeVisible()
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('C-02.4: limite de 100 caracteres — exibe mensagem e trunca ao atingir o 101º', async ({
    page,
  }) => {
    const campo = page.getByLabel('Nome completo *')
    // 101 caracteres com letras e espaços válidos
    const textoLongo = 'Abcde Fghij Klmno Pqrst Uvwxy Zabcd Efghi Jklmn Opqrs Tuvwx Yzabc Defgh Ijklm'

    await campo.fill(textoLongo.slice(0, 101))

    await expect(page.getByText('Limite: Até 100 caracteres.')).toBeVisible({ timeout: 5_000 })
    const valor = await campo.inputValue()
    expect(valor.length).toBeLessThanOrEqual(100)
  })

  test('C-02.5: mensagem de limite desaparece após 3 segundos', async ({ page }) => {
    const campo = page.getByLabel('Nome completo *')
    const textoLongo = 'Abcde Fghij Klmno Pqrst Uvwxy Zabcd Efghi Jklmn Opqrs Tuvwx Yzabc Defgh Ijklm'

    await campo.fill(textoLongo.slice(0, 101))
    await expect(page.getByText('Limite: Até 100 caracteres.')).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText('Limite: Até 100 caracteres.')).not.toBeVisible({ timeout: 5_000 })
  })

  test('C-02.6: campo vazio ao clicar em Cadastrar exibe mensagem de obrigatório', async ({
    page,
  }) => {
    await botaoCadastrar(page).click()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-03 — CPF
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-03 — Campo CPF', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-03.1: CPF com dígitos inválidos exibe erro ao sair do campo', async ({ page }) => {
    const campo = page.getByLabel('CPF *')
    await campo.fill('00000000000')
    await campo.blur()
    await expect(page.getByText(/CPF inválido/i)).toBeVisible()
  })

  test('C-03.2: CPF já cadastrado bloqueia novo cadastro', async ({ page, request }) => {
    const seed = Date.now() + 300
    const cpf = gerarCpf(seed)
    const token = await tokenAdmin(request)
    await cadastrarClienteApi(request, token, {
      nome: 'Cliente Existente Teste',
      cpf,
      telefone: gerarTelefone(seed),
      email: `existente.${seed}@teste.local`,
    })

    await novoCadastro(page)
    const campo = page.getByLabel('CPF *')
    await campo.fill(cpf)
    await campo.blur()

    await expect(
      page.getByText(/CPF já cadastrado\. Use Buscar por CPF para editar\./i),
    ).toBeVisible({ timeout: 15_000 })
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('C-03.3: CPF incompleto não habilita o botão Cadastrar', async ({ page }) => {
    await page.getByLabel('Nome completo *').fill('Cliente Valido Teste')
    const campo = page.getByLabel('CPF *')
    await campo.fill('1234567')
    await campo.blur()
    await expect(botaoCadastrar(page)).toBeDisabled()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-04 — DATA DE NASCIMENTO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-04 — Campo Data de Nascimento', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-04.1: data inexistente 31/02/1970 é rejeitada', async ({ page }) => {
    await page.getByLabel('Nome completo *').fill('Maria Silva Teste')
    await page.getByLabel('CPF *').fill('52998224725')

    const campo = page.getByLabel('Data de nascimento')
    await campo.fill('31/02/1970')
    await campo.blur()

    await expect(page.getByText(/Data inválida — verifique dia e mês/i)).toBeVisible()
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('C-04.2: cliente menor de 18 anos é bloqueado', async ({ page }) => {
    await page.getByLabel('Nome completo *').fill('Joao Menor Teste')
    await page.getByLabel('CPF *').fill(gerarCpf(Date.now() + 400))

    const campo = page.getByLabel('Data de nascimento')
    await campo.fill('15/06/2010')
    await campo.blur()

    await expect(
      page.getByText(/Cliente deve ter 18 anos ou mais para cadastro/i),
    ).toBeVisible()
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('C-04.3: data futura é rejeitada', async ({ page }) => {
    await page.getByLabel('Nome completo *').fill('Futuro Cliente Teste')
    await page.getByLabel('CPF *').fill(gerarCpf(Date.now() + 500))

    const campo = page.getByLabel('Data de nascimento')
    await campo.fill('15/06/2090')
    await campo.blur()

    await expect(page.getByText(/Data inválida|data futura/i)).toBeVisible()
    await expect(botaoCadastrar(page)).toBeDisabled()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-05 — TELEFONE
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-05 — Campo Telefone', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-05.1: aplica máscara para celular com 11 dígitos', async ({ page }) => {
    const campo = page.getByLabel('Telefone / WhatsApp')
    await campo.fill('21965832561')
    await expect(campo).toHaveValue('(21) 96583-2561')
  })

  test('C-05.2: aplica máscara para fixo com 10 dígitos', async ({ page }) => {
    const campo = page.getByLabel('Telefone / WhatsApp')
    await campo.fill('2196583256')
    await expect(campo).toHaveValue('(21) 9658-3256')
  })

  test('C-05.3: telefone já cadastrado bloqueia novo cadastro', async ({ page, request }) => {
    const seed = Date.now() + 600
    const telefone = gerarTelefone(seed)
    const token = await tokenAdmin(request)
    await cadastrarClienteApi(request, token, {
      nome: 'Telefone Duplicado Teste',
      cpf: gerarCpf(seed),
      telefone,
      email: `tel.${seed}@teste.local`,
    })

    await novoCadastro(page)
    await page.getByLabel('Nome completo *').fill('Novo Cliente Telefone')
    await page.getByLabel('CPF *').fill(gerarCpf(seed + 5000))

    const campo = page.getByLabel('Telefone / WhatsApp')
    await campo.fill(telefone)
    const verificacao = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/clientes/contato/disponivel') &&
        r.request().method() === 'GET',
    )
    await campo.blur()
    await verificacao

    await expect(
      page.getByText(/Telefone já cadastrado em outro cliente\./i),
    ).toBeVisible({ timeout: 15_000 })
    await expect(botaoCadastrar(page)).toBeDisabled()
  })

  test('C-05.4: telefone com menos de 10 dígitos exibe erro de formato', async ({ page }) => {
    const campo = page.getByLabel('Telefone / WhatsApp')
    await campo.fill('21999')
    await campo.blur()
    await expect(page.getByText(/10 ou 11 dígitos/i)).toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-06 — E-MAIL
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-06 — Campo E-mail', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-06.1: remove espaços ao digitar', async ({ page }) => {
    const campo = page.getByLabel('E-mail')
    await campo.fill('teste@email.com ')
    await expect(campo).toHaveValue('teste@email.com')
  })

  test('C-06.2: formato inválido com TLD numérico exibe erro', async ({ page }) => {
    const campo = page.getByLabel('E-mail')
    await campo.fill('helena@gmail.com.123456')
    await campo.blur()
    await expect(page.getByText(/E-mail inválido/i)).toBeVisible()
  })

  test('C-06.3: e-mail duplicado bloqueia novo cadastro', async ({ page, request }) => {
    const seed = Date.now() + 700
    const email = `duplicado.${seed}@teste.local`
    const token = await tokenAdmin(request)
    await cadastrarClienteApi(request, token, {
      nome: 'Email Duplicado Teste',
      cpf: gerarCpf(seed),
      telefone: gerarTelefone(seed + 50),
      email,
    })

    await novoCadastro(page)
    await page.getByLabel('Nome completo *').fill('Outro Cliente Email')
    await page.getByLabel('CPF *').fill(gerarCpf(seed + 6000))

    const campo = page.getByLabel('E-mail')
    await campo.fill(email)
    const verificacao = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/clientes/contato/disponivel') &&
        r.request().method() === 'GET',
    )
    await campo.blur()
    await verificacao

    await expect(
      page.getByText(/E-mail já cadastrado em outro cliente\./i),
    ).toBeVisible({ timeout: 15_000 })
    await expect(botaoCadastrar(page)).toBeDisabled()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-07 — ENDEREÇO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-07 — Campos de Endereço', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-07.1: selecionar UF RJ e cidade Rio de Janeiro', async ({ page }) => {
    await selecionarUf(page, 'RJ')
    await selecionarCidade(page, 'Rio de Janeiro')
    const secao = page
      .locator('section')
      .filter({ has: page.getByText('Endereço', { exact: true }) })
    await expect(secao.getByRole('button').filter({ hasText: /^RJ$/ })).toBeVisible()
    await expect(page.getByLabel('Cidade')).toContainText('Rio de Janeiro')
  })

  test('C-07.2: selecionar UF MG e filtrar Belo Horizonte', async ({ page }) => {
    await selecionarUf(page, 'MG')
    const secao = page
      .locator('section')
      .filter({ has: page.getByText('Endereço', { exact: true }) })
    await secao.getByLabel('Cidade').click()
    await page.getByLabel('Filtrar cidades').fill('Belo Horizonte')
    await page
      .getByRole('listbox')
      .getByRole('button', { name: 'Belo Horizonte', exact: true })
      .click()
    await expect(page.getByLabel('Cidade')).toContainText('Belo Horizonte')
  })

  test('C-07.3: antes de selecionar UF o campo Cidade exibe aviso', async ({ page }) => {
    await expect(page.getByLabel('Cidade')).toContainText(/Selecione a UF primeiro/i)
  })

  test('C-07.4: CEP com 8 dígitos é aceito sem erro', async ({ page }) => {
    const campo = page.getByLabel('CEP *')
    await campo.fill('01310100')
    await campo.blur()
    await expect(page.getByText(/CEP deve conter/i)).not.toBeVisible()
  })

  test('C-07.5: CEP com menos de 8 dígitos exibe erro', async ({ page }) => {
    const campo = page.getByLabel('CEP *')
    await campo.fill('0131')
    await campo.blur()
    await expect(page.getByText(/CEP deve conter 8 dígitos/i)).toBeVisible()
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-08 — MENSAGENS PADRONIZADAS DE CAMPO OBRIGATÓRIO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-08 — Mensagem "Lembre-se: Campo Obrigatório." ao submeter sem preencher', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-08.1: submeter formulário vazio exibe pelo menos uma mensagem de obrigatório', async ({
    page,
  }) => {
    await botaoCadastrar(page).click()
    await expect(page.getByText(MSG_OBRIGATORIO).first()).toBeVisible({ timeout: 5_000 })
  })

  test('C-08.2: a mensagem de obrigatório usa exatamente o texto padrão do sistema', async ({
    page,
  }) => {
    await botaoCadastrar(page).click()
    // Verifica que não existem mensagens no formato antigo "X é obrigatório/a"
    await expect(page.getByText(/é obrigatório/i)).not.toBeVisible({ timeout: 3_000 })
    await expect(page.getByText(/é obrigatória/i)).not.toBeVisible({ timeout: 3_000 })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-09 — BUSCA POR CPF
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-09 — Busca por CPF', () => {
  test.beforeEach(async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)
  })

  test('C-09.1: buscar CPF cadastrado carrega dados do cliente no formulário', async ({
    page,
    request,
  }) => {
    const seed = Date.now() + 800
    const cpf = gerarCpf(seed)
    const token = await tokenAdmin(request)
    await cadastrarClienteApi(request, token, {
      nome: 'Roberto Alves Pereira',
      cpf,
      telefone: gerarTelefone(seed),
      email: `roberto.${seed}@teste.local`,
    })

    const painelBusca = page.locator('aside').filter({ hasText: 'Buscar por CPF' })
    await painelBusca.getByLabel('CPF').fill(cpf)
    await page.getByRole('button', { name: 'Buscar' }).click()

    await expect(page.getByRole('heading', { name: /Editar cliente/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.getByLabel('Nome completo *')).toHaveValue('Roberto Alves Pereira')
  })

  test('C-09.2: trocar CPF na busca limpa os campos do formulário', async ({ page, request }) => {
    const seed = Date.now() + 900
    const cpfOriginal = gerarCpf(seed)
    const cpfNovo = gerarCpf(seed + 9000)
    const token = await tokenAdmin(request)
    await cadastrarClienteApi(request, token, {
      nome: 'Fernando Costa Lima',
      cpf: cpfOriginal,
      telefone: gerarTelefone(seed),
      email: `fernando.${seed}@teste.local`,
    })

    const painelBusca = page.locator('aside').filter({ hasText: 'Buscar por CPF' })
    const cpfBusca = painelBusca.getByLabel('CPF')
    await cpfBusca.fill(cpfOriginal)
    await page.getByRole('button', { name: 'Buscar' }).click()
    await expect(page.getByRole('heading', { name: /Editar cliente/i })).toBeVisible({
      timeout: 15_000,
    })

    await cpfBusca.fill(cpfNovo)

    await expect(page.getByRole('heading', { name: /Cadastrar cliente/i })).toBeVisible()
    await expect(page.getByLabel('Telefone / WhatsApp')).toHaveValue('')
    await expect(page.getByLabel('E-mail')).toHaveValue('')
  })

  test('C-09.3: buscar CPF não cadastrado mantém formulário de novo cadastro', async ({ page }) => {
    const cpfInexistente = gerarCpf(Date.now() + 99999)
    const painelBusca = page.locator('aside').filter({ hasText: 'Buscar por CPF' })
    await painelBusca.getByLabel('CPF').fill(cpfInexistente)
    await page.getByRole('button', { name: 'Buscar' }).click()

    await expect(page.getByRole('heading', { name: /Cadastrar cliente/i })).toBeVisible({
      timeout: 10_000,
    })
  })
})

// ══════════════════════════════════════════════════════════════════════════════
// C-10 — FLUXO COMPLETO
// ══════════════════════════════════════════════════════════════════════════════
test.describe('C-10 — Fluxo completo de cadastro', () => {
  test('C-10.1: cadastra cliente masculino com todos os dados válidos', async ({
    page,
    request,
  }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)

    const seed = Date.now() + 1000
    await preencherDadosPessoais(page, seed)
    await preencherEndereco(page, 'SP', 'Sao Paulo')

    await expect(botaoCadastrar(page)).toBeEnabled({ timeout: 10_000 })

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/clientes') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText(/Cliente cadastrado com sucesso/i)).toBeVisible({
      timeout: 15_000,
    })
  })

  test('C-10.2: cadastra cliente feminino com cidade diferente', async ({ page, request }) => {
    await autenticarAdmin(page, request)
    await irParaCadastroClientes(page)
    await novoCadastro(page)

    const seed = Date.now() + 1100
    await preencherDadosPessoais(page, seed, { nome: 'Ana Paula Souza', sexo: 'Feminino' })
    await preencherEndereco(page, 'RJ', 'Rio de Janeiro')

    await expect(botaoCadastrar(page)).toBeEnabled({ timeout: 10_000 })

    const resposta = page.waitForResponse(
      (r) =>
        r.url().includes('/api/v1/clientes') &&
        r.request().method() === 'POST' &&
        r.status() === 201,
    )
    await botaoCadastrar(page).click()
    await resposta

    await expect(page.getByText(/Cliente cadastrado com sucesso/i)).toBeVisible({
      timeout: 15_000,
    })
  })
})