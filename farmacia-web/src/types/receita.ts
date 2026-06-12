export type TipoReceita =
  | 'SIMPLES'
  | 'AZUL'
  | 'AMARELA'
  | 'BRANCA_ESPECIAL'
  | 'ANTIMICROBIANO'

export type StatusReceita =
  | 'PENDENTE'
  | 'APROVADA'
  | 'REJEITADA'
  | 'UTILIZADA'
  | 'VENCIDA'
  | 'SUSPENSA'

export interface Receita {
  id: string
  numeroReceita: string
  dataEmissao: string
  dataValidade: string
  tipo: TipoReceita
  status: StatusReceita
  cid?: string
  retida?: boolean
  clienteId?: string
  clienteNome?: string
  prescritorId?: string
  prescritorNome?: string
  prescritorCrm?: string
  dataValidacao?: string
  motivoRejeicao?: string
}

export interface ReceitaInput {
  numeroReceita: string
  tipo: TipoReceita
  dataEmissao?: string
  prescritorId: string
  clienteId?: string
  cid?: string
}

export interface ValidarReceitaInput {
  itens: Array<{
    medicamentoId: string
    quantidade: number
  }>
}

export interface ValidarReceitaResult {
  aprovada: boolean
  status: string
  violacoes: string[]
  receitaId: string
}

/** Prescritor seed dev — espelha DevOperacionalSeed */
export const PRESCRITOR_DEV_ID = '66666666-6666-6666-6666-666666666666'
