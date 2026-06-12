import type { Medicamento, NivelControle } from './api'

export interface PdvContexto {
  pdvId: string
  numero: string
  descricao: string
  status: string
  caixaAberto: boolean
}

export interface CartItem {
  medicamento: Medicamento
  quantidade: number
  precoUnitario: number
  desconto: number
}

export interface VendaInput {
  pdv: { id: string }
  funcionario: { id: string }
  cliente?: { id: string }
  receita?: { id: string }
  itens: Array<{
    medicamento: { id: string }
    quantidade: number
    precoUnitario: number
    desconto: number
  }>
  pagamentos: Array<{
    forma: FormaPagamento
    valor: number
  }>
  compradorCpf?: string
  compradorNome?: string
  tipoAtendimento?: TipoAtendimento
  observacao?: string
}

export interface VendaRealizada {
  vendaId: string
  numeroCupom: string
  total: number
  avisos: string[]
}

export type FormaPagamento =
  | 'DINHEIRO'
  | 'CARTAO_DEBITO'
  | 'CARTAO_CREDITO'
  | 'PIX'
  | 'CONVENIO'
  | 'VALE_FARMACIA'
  | 'CREDIARIO'

export type TipoAtendimento = 'BALCAO' | 'DELIVERY' | 'TELEFONE'

export function isControlado(nivel: NivelControle): boolean {
  return [
    'CONTROLADO_B1',
    'CONTROLADO_B2',
    'CONTROLADO_C1',
    'CONTROLADO_C2',
  ].includes(nivel)
}

export function cartRequiresReceita(items: CartItem[]): boolean {
  return items.some((i) => i.medicamento.requerReceita)
}

export function cartRequiresCpf(items: CartItem[]): boolean {
  return items.some(
    (i) =>
      isControlado(i.medicamento.nivelControle) ||
      i.medicamento.nivelControle === 'ANTIMICROBIANO',
  )
}

export function cartSubtotal(items: CartItem[]): number {
  return items.reduce(
    (sum, i) => sum + i.precoUnitario * i.quantidade - i.desconto,
    0,
  )
}
