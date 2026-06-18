import { clearToken, getAuthHeader } from './auth'
import type { ApiProblem } from '@/types/api'
import { traduzirTextoConhecido, resolverMensagemErroApi } from './mensagens'

const BASE = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  status: number
  problem?: ApiProblem

  constructor(message: string, status: number, problem?: ApiProblem) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

interface ApiOptions {
  /** Não redireciona para /login em 401 (ex.: tela de login) */
  skipAuthRedirect?: boolean
}

export async function api<T>(
  path: string,
  init: RequestInit = {},
  options: ApiOptions = {},
): Promise<T> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...getAuthHeader(),
    ...init.headers,
  }

  let res: Response
  try {
    res = await fetch(`${BASE}${path}`, { ...init, headers })
  } catch (cause) {
    const msg =
      cause instanceof Error
        ? traduzirTextoConhecido(cause.message)
        : 'Não foi possível conectar à API.'
    throw new ApiError(msg, 0)
  }

  if (res.status === 401 && !options.skipAuthRedirect) {
    clearToken()
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login'
    }
    throw new ApiError('Sessão expirada. Faça login novamente.', 401)
  }

  if (!res.ok) {
    let problem: ApiProblem | undefined
    try {
      problem = (await res.json()) as ApiProblem
    } catch {
      /* corpo não-JSON */
    }
    const fallback = resolverMensagemErroApi(
      res.status,
      problem?.detail ?? problem?.title ?? res.statusText,
      problem?.userMessage,
    )
    throw new ApiError(fallback, res.status, problem)
  }

  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export async function login(email: string, senha: string) {
  return api<{ token: string; tipo: string; expiraEmSegundos: number }>(
    '/api/v1/auth/token',
    {
      method: 'POST',
      body: JSON.stringify({ email, senha }),
    },
    { skipAuthRedirect: true },
  )
}

export async function fetchAuthContexto() {
  return api<import('@/types/api').AuthContexto>('/api/v1/auth/contexto')
}

export async function fetchMedicamentos(page = 0, size = 20, busca?: string) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: 'nomeComercial,asc',
  })
  if (busca?.trim()) {
    params.set('busca', busca.trim())
  }
  return api<import('@/types/api').Page<import('@/types/api').Medicamento>>(
    `/api/v1/medicamentos?${params}`,
  )
}

/** Carrega todas as páginas — para selects, PDV e buscas client-side. */
export async function fetchAllMedicamentos() {
  const pageSize = 100
  const first = await fetchMedicamentos(0, pageSize)
  const all = [...first.content]
  for (let page = 1; page < first.totalPages; page++) {
    const next = await fetchMedicamentos(page, pageSize)
    all.push(...next.content)
  }
  return all
}

export async function fetchMedicamento(id: string) {
  return api<import('@/types/api').Medicamento>(`/api/v1/medicamentos/${id}`)
}

export async function cadastrarMedicamento(input: import('@/types/cadastro').MedicamentoInput) {
  return api<import('@/types/api').Medicamento>('/api/v1/medicamentos', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function atualizarMedicamento(
  id: string,
  input: import('@/types/cadastro').MedicamentoInput,
) {
  return api<import('@/types/api').Medicamento>(`/api/v1/medicamentos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export async function excluirMedicamento(id: string) {
  return api<void>(`/api/v1/medicamentos/${id}`, { method: 'DELETE' })
}

// ── Clientes ───────────────────────────────────────────────────────────────

export async function fetchClientePorCpf(cpf: string) {
  const digits = cpf.replace(/\D/g, '')
  return api<import('@/types/cliente').Cliente>(`/api/v1/clientes/cpf/${digits}`)
}

export type DisponibilidadeContato = {
  telefoneDisponivel: boolean
  emailDisponivel: boolean
}

export async function verificarContatoCliente(params: {
  telefone?: string
  email?: string
  excluirClienteId?: string
}) {
  const query = new URLSearchParams()
  if (params.telefone?.trim()) {
    query.set('telefone', params.telefone.replace(/\D/g, ''))
  }
  if (params.email?.trim()) {
    query.set('email', params.email.trim().toLowerCase())
  }
  if (params.excluirClienteId) {
    query.set('excluirClienteId', params.excluirClienteId)
  }
  return api<DisponibilidadeContato>(
    `/api/v1/clientes/contato/disponivel?${query.toString()}`,
  )
}

export async function cadastrarCliente(input: import('@/types/cadastro').ClienteInput) {
  return api<import('@/types/cliente').Cliente>('/api/v1/clientes', {
    method: 'POST',
    body: JSON.stringify({
      ...input,
      cpf: input.cpf.replace(/\D/g, ''),
      telefone: input.telefone?.replace(/\D/g, '') || undefined,
      email: input.email?.trim().toLowerCase() || undefined,
    }),
  })
}

export async function atualizarCliente(
  id: string,
  input: import('@/types/cadastro').ClienteAtualizacaoInput,
) {
  return api<import('@/types/cliente').Cliente>(`/api/v1/clientes/${id}`, {
    method: 'PUT',
    body: JSON.stringify({
      ...input,
      telefone: input.telefone?.replace(/\D/g, '') || undefined,
      email: input.email?.trim().toLowerCase() || undefined,
    }),
  })
}

// ── Catálogo auxiliar ──────────────────────────────────────────────────────

export async function fetchFabricantes() {
  return api<import('@/types/catalogo').Fabricante[]>('/api/v1/catalogo/fabricantes')
}

export async function cadastrarFabricante(input: import('@/types/catalogo').FabricanteInput) {
  return api<import('@/types/catalogo').Fabricante>('/api/v1/catalogo/fabricantes', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function fetchCategorias() {
  return api<import('@/types/catalogo').Categoria[]>('/api/v1/catalogo/categorias')
}

export async function cadastrarCategoria(input: import('@/types/catalogo').CategoriaInput) {
  return api<import('@/types/catalogo').Categoria>('/api/v1/catalogo/categorias', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function atualizarCategoria(id: string, input: import('@/types/catalogo').CategoriaInput) {
  return api<import('@/types/catalogo').Categoria>(`/api/v1/catalogo/categorias/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export async function excluirCategoria(id: string) {
  return api<void>(`/api/v1/catalogo/categorias/${id}`, { method: 'DELETE' })
}

export async function fetchPrescritores() {
  return api<import('@/types/catalogo').Prescritor[]>('/api/v1/catalogo/prescritores')
}

export async function cadastrarPrescritor(input: import('@/types/catalogo').PrescritorInput) {
  return api<import('@/types/catalogo').Prescritor>('/api/v1/catalogo/prescritores', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function atualizarPrescritor(id: string, input: import('@/types/catalogo').PrescritorInput) {
  return api<import('@/types/catalogo').Prescritor>(`/api/v1/catalogo/prescritores/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export async function excluirPrescritor(id: string) {
  return api<void>(`/api/v1/catalogo/prescritores/${id}`, { method: 'DELETE' })
}

// ── Caixa ──────────────────────────────────────────────────────────────────

export async function abrirCaixa(input: {
  pdvId: string
  funcionarioId: string
  saldoAbertura?: number
}) {
  return api<{ caixaId: string; pdvId: string; pdvNumero: string }>('/api/v1/caixa/abrir', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function fecharCaixa(input: { pdvId: string; observacao?: string }) {
  return api<{
    caixaId: string
    pdvId: string
    pdvNumero: string
    saldoFechamento: number
  }>('/api/v1/caixa/fechar', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function fetchPdvContexto(numero = 'PDV-01') {
  return api<import('@/types/venda').PdvContexto>(
    `/api/v1/pdv/contexto?numero=${encodeURIComponent(numero)}`,
  )
}

export async function realizarVenda(input: import('@/types/venda').VendaInput) {
  return api<import('@/types/venda').VendaRealizada>('/api/v1/vendas', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

// ── Estoque ────────────────────────────────────────────────────────────────

export async function fetchEstoqueItens(params: { page?: number; size?: number; busca?: string } = {}) {
  const { page = 0, size = 20, busca } = params
  const qs = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: 'medicamentoNome,asc',
  })
  if (busca?.trim()) {
    qs.set('busca', busca.trim())
  }
  return api<import('@/types/api').Page<import('@/types/estoque').ItemEstoque>>(
    `/api/v1/estoque/itens?${qs}`,
  )
}

export async function fetchEstoqueDisponivelVenda() {
  return api<import('@/types/estoque').DisponivelVenda[]>('/api/v1/estoque/disponivel-venda')
}

export async function fetchEstoqueSaldo(medicamentoId: string) {
  return api<import('@/types/estoque').ItemEstoque>(
    `/api/v1/estoque/medicamentos/${medicamentoId}`,
  )
}

export async function fetchLotesFefo(medicamentoId: string) {
  return api<import('@/types/estoque').Lote[]>(
    `/api/v1/estoque/medicamentos/${medicamentoId}/lotes`,
  )
}

export async function fetchLotesParaAjuste(medicamentoId: string) {
  return api<import('@/types/estoque').Lote[]>(
    `/api/v1/estoque/medicamentos/${medicamentoId}/lotes/ajuste`,
  )
}

export async function fetchAlertasEstoque(tipo?: string) {
  const q = tipo ? `?tipo=${encodeURIComponent(tipo)}` : ''
  return api<import('@/types/estoque').AlertaEstoque[]>(`/api/v1/estoque/alertas${q}`)
}

export async function fetchEstoqueAbaixoMinimo() {
  return api<import('@/types/estoque').ItemEstoque[]>('/api/v1/estoque/abaixo-minimo')
}

export async function fetchEstoqueZerados() {
  return api<import('@/types/estoque').ItemEstoque[]>('/api/v1/estoque/zerados')
}

export async function registrarEntradaEstoque(input: import('@/types/estoque').EntradaEstoqueInput) {
  return api<import('@/types/estoque').EntradaEstoqueResult>('/api/v1/estoque/entrada', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function atualizarParametrosEstoque(
  medicamentoId: string,
  input: import('@/types/estoque').AtualizarItemEstoqueInput,
) {
  return api<import('@/types/estoque').ItemEstoque>(
    `/api/v1/estoque/medicamentos/${medicamentoId}/parametros`,
    { method: 'PUT', body: JSON.stringify(input) },
  )
}

export async function registrarAjusteSaldo(input: import('@/types/estoque').AjusteSaldoInput) {
  return api<import('@/types/estoque').AjusteSaldoResult>('/api/v1/estoque/ajuste', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function fetchMovimentacoesEstoque(params: {
  page?: number
  size?: number
  medicamentoId?: string
  tipo?: string
}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 20))
  if (params.medicamentoId) q.set('medicamentoId', params.medicamentoId)
  if (params.tipo) q.set('tipo', params.tipo)
  return api<import('@/types/api').Page<import('@/types/estoque-movimentacao').MovimentacaoEstoque>>(
    `/api/v1/estoque/movimentacoes?${q}`,
  )
}

// ── Compras / Fornecedores ─────────────────────────────────────────────────

export async function fetchFornecedores() {
  return api<import('@/types/compra').Fornecedor[]>('/api/v1/fornecedores')
}

export async function cadastrarFornecedor(input: import('@/types/compra').FornecedorInput) {
  return api<import('@/types/compra').Fornecedor>('/api/v1/fornecedores', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function fetchNotasFiscaisEntrada() {
  return api<import('@/types/compra').NotaFiscalEntrada[]>('/api/v1/compras/notas')
}

export async function registrarNotaFiscalEntrada(input: import('@/types/compra').NotaFiscalEntradaInput) {
  return api<import('@/types/compra').NotaFiscalEntradaResult>('/api/v1/compras/notas', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function fetchPedidosCompra() {
  return api<import('@/types/compra').PedidoCompra[]>('/api/v1/compras/pedidos')
}

export async function fetchPedidoCompra(id: string) {
  return api<import('@/types/compra').PedidoCompra>(`/api/v1/compras/pedidos/${id}`)
}

export async function criarPedidoCompra(input: import('@/types/compra').PedidoCompraInput) {
  return api<import('@/types/compra').PedidoCompra>('/api/v1/compras/pedidos', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function confirmarPedidoCompra(id: string) {
  return api<import('@/types/compra').PedidoCompra>(`/api/v1/compras/pedidos/${id}/confirmar`, {
    method: 'PUT',
  })
}

// ── Receitas ───────────────────────────────────────────────────────────────

export async function fetchReceitaPorId(receitaId: string) {
  return api<import('@/types/receita').Receita>(`/api/v1/receitas/${receitaId}`)
}

export async function fetchReceitaPorNumero(numero: string) {
  return api<import('@/types/receita').Receita>(
    `/api/v1/receitas?numero=${encodeURIComponent(numero)}`,
  )
}

export async function cadastrarReceita(input: import('@/types/receita').ReceitaInput) {
  return api<import('@/types/receita').Receita>('/api/v1/receitas', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function validarReceita(receitaId: string, input: import('@/types/receita').ValidarReceitaInput) {
  return api<import('@/types/receita').ValidarReceitaResult>(
    `/api/v1/receitas/${receitaId}/validar`,
    { method: 'PUT', body: JSON.stringify(input) },
  )
}
