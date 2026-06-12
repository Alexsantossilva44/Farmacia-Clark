export type NivelControle =
  | 'LIVRE'
  | 'RECEITA_SIMPLES'
  | 'CONTROLADO_C1'
  | 'CONTROLADO_C2'
  | 'CONTROLADO_B1'
  | 'CONTROLADO_B2'
  | 'ANTIMICROBIANO'

export type TipoMedicamento =
  | 'REFERENCIA'
  | 'GENERICO'
  | 'SIMILAR'
  | 'BIOLOGICO'
  | 'FITOTERAPICO'
  | 'HOMEOPATICO'
  | 'OTC'

export type FormaFarmaceutica =
  | 'COMPRIMIDO'
  | 'CAPSULA'
  | 'XAROPE'
  | 'SOLUCAO'
  | 'SUSPENSAO'
  | 'INJETAVEL'
  | 'POMADA'
  | 'CREME'
  | 'GEL'
  | 'SUPOSITORIO'
  | 'COLIRIO'
  | 'SPRAY'
  | 'PATCH'
  | 'PO'

export interface FabricanteResumo {
  id: string
  razaoSocial: string
  nomeFantasia: string
}

export interface CategoriaResumo {
  id: string
  nome: string
}

export interface PrincipioAtivoResumo {
  id: string
  nome: string
  dcb: string
}

export interface Medicamento {
  id: string
  codigoEan: string
  codigoAnvisa: string
  nomeComercial: string
  nomeGenerico: string
  tipo: TipoMedicamento
  formaFarmaceutica: FormaFarmaceutica
  concentracao: string
  apresentacao: string
  classeTerapeutica: string
  requerReceita: boolean
  nivelControle: NivelControle
  precoMaximoConsumidor: number
  ativo: boolean
  fabricante?: FabricanteResumo
  categoria?: CategoriaResumo
  principiosAtivos?: PrincipioAtivoResumo[]
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface TokenResponse {
  token: string
  tipo: string
  expiraEmSegundos: number
}

export interface LoginInput {
  email: string
  senha: string
}

export interface ApiProblem {
  status: number
  title: string
  detail?: string
  userMessage?: string
  fields?: Array<{ name: string; userMessage: string }>
}

export interface AuthContexto {
  email: string
  nome: string
  role: string | null
  possuiRegistroFarmaceutico: boolean
  crf: string | null
  ufCrf: string | null
}
