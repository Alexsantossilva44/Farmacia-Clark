export interface MovimentacaoEstoque {
  id: string
  medicamentoId: string
  medicamentoNome: string
  loteId: string
  numeroLote: string
  tipo: string
  quantidade: number
  saldoAnterior: number
  saldoPosterior: number
  referenciaId?: string
  motivoAjuste?: string
  dataHora: string
}

export type TipoMovimentacao =
  | 'ENTRADA_COMPRA'
  | 'ENTRADA_DEVOLUCAO_CLIENTE'
  | 'SAIDA_VENDA'
  | 'SAIDA_VENCIMENTO'
  | 'SAIDA_PERDA'
  | 'AJUSTE_POSITIVO'
  | 'AJUSTE_NEGATIVO'
  | 'TRANSFERENCIA'
