export interface Fornecedor {
  id: string
  razaoSocial: string
  nomeFantasia?: string
  cnpj: string
  ativo: boolean
}

export interface NotaFiscalEntrada {
  id: string
  pedidoCompraId?: string
  fornecedorId: string
  fornecedorNome: string
  numeroNota: string
  serie?: string
  chaveAcesso: string
  dataEmissao: string
  dataEntrada: string
  valorTotal: number
  status: string
  createdAt?: string
  quantidadeItens: number
}

export interface ItemNotaFiscalInput {
  medicamentoId: string
  numeroLote: string
  dataValidade: string
  dataFabricacao?: string
  quantidade: number
  precoUnitario?: number
}

export interface NotaFiscalEntradaInput {
  fornecedorId: string
  pedidoCompraId?: string
  numeroNota: string
  serie?: string
  chaveAcesso: string
  dataEmissao: string
  dataEntrada?: string
  valorTotal?: number
  itens: ItemNotaFiscalInput[]
}

export interface FornecedorInput {
  razaoSocial: string
  nomeFantasia?: string
  cnpj: string
}

export interface ItemPedidoCompraInput {
  medicamentoId: string
  quantidadeSolicitada: number
  precoUnitario?: number
}

export interface PedidoCompraInput {
  fornecedorId: string
  dataPedido?: string
  dataEntregaPrevista?: string
  observacao?: string
  itens: ItemPedidoCompraInput[]
}

export interface ItemPedidoCompra {
  id: string
  medicamentoId: string
  medicamentoNome: string
  quantidadeSolicitada: number
  quantidadeRecebida: number
  quantidadePendente: number
  precoUnitario?: number
}

export interface PedidoCompra {
  id: string
  fornecedorId: string
  fornecedorNome: string
  dataPedido: string
  dataEntregaPrevista?: string
  status: string
  valorTotal: number
  observacao?: string
  quantidadePendente?: number
  itens?: ItemPedidoCompra[]
}

export interface DivergenciaConferencia {
  tipo: string
  medicamentoId: string
  medicamentoNome: string
  quantidadeEsperada?: number
  quantidadeRecebida?: number
  precoEsperado?: number
  precoRecebido?: number
  mensagem: string
}

export interface ConferenciaNota {
  conferida: boolean
  statusNota: string
  pedidoCompraId: string
  statusPedido: string
  divergencias: DivergenciaConferencia[]
}

export interface NotaFiscalEntradaResult {
  nota: NotaFiscalEntrada
  conferencia?: ConferenciaNota
}
