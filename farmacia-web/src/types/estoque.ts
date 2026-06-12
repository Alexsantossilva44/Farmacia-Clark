export type TipoAlerta =
  | 'ESTOQUE_MINIMO'
  | 'VENCIMENTO_PROXIMO'
  | 'LOTE_VENCIDO'
  | 'ESTOQUE_ZERADO'

export type StatusAlerta = 'ABERTO' | 'RESOLVIDO' | 'IGNORADO'

export type StatusLote = 'ATIVO' | 'VENCIDO' | 'BLOQUEADO' | 'ESGOTADO' | 'DEVOLVIDO'

export interface DisponivelVenda {
  medicamentoId: string
  quantidadeDisponivelVenda: number
}

export interface ItemEstoque {
  id?: string
  medicamentoId: string
  medicamentoNome: string
  quantidadeAtual: number
  quantidadeMinima: number
  quantidadeMaxima: number
  abaixoDoMinimo: boolean
  zerado: boolean
  semEntrada?: boolean
  /** Lotes ativos e não vencidos — saldo que o PDV pode dispensar. */
  quantidadeDisponivelVenda?: number
}

export interface Lote {
  id: string
  medicamentoId: string
  numeroLote: string
  dataValidade: string
  quantidadeAtual: number
  status: StatusLote
  diasParaVencer: number
}

export interface AlertaEstoque {
  id: string
  medicamentoId: string
  medicamentoNome: string
  loteId?: string
  numeroLote?: string
  tipo: TipoAlerta
  mensagem: string
  status: StatusAlerta
}

export interface EntradaEstoqueInput {
  medicamentoId: string
  numeroLote: string
  dataValidade: string
  dataFabricacao?: string
  quantidade: number
  precoCusto?: number
  quantidadeMinima?: number
  quantidadeMaxima?: number
  observacao?: string
}

export interface AtualizarItemEstoqueInput {
  quantidadeMinima?: number
  quantidadeMaxima?: number
}

export type TipoAjusteSaldo = 'AJUSTE_POSITIVO' | 'AJUSTE_NEGATIVO'

export interface AjusteSaldoInput {
  medicamentoId: string
  loteId: string
  tipo: TipoAjusteSaldo
  quantidade: number
  motivo: string
}

export interface AjusteSaldoResult {
  itemEstoque: ItemEstoque
  lote: Lote
}

export interface EntradaEstoqueResult {
  itemEstoque: ItemEstoque
  lote: Lote
}
