import type { Endereco } from './cliente'
import type {
  FormaFarmaceutica,
  NivelControle,
  TipoMedicamento,
} from './api'

export interface MedicamentoInput {
  codigoEan?: string
  codigoAnvisa?: string
  nomeComercial: string
  nomeGenerico?: string
  tipo: TipoMedicamento
  formaFarmaceutica?: FormaFarmaceutica
  concentracao?: string
  apresentacao?: string
  classeTerapeutica?: string
  requerReceita: boolean
  nivelControle: NivelControle
  precoMaximoConsumidor: number
  fabricante: { id: string }
  categoria: { id: string }
}

export interface ClienteInput {
  nome: string
  cpf: string
  dataNascimento?: string
  sexo?: string
  telefone?: string
  email?: string
  endereco?: Endereco
  alergias?: string
  observacoes?: string
}

export interface ClienteAtualizacaoInput {
  nome?: string
  dataNascimento?: string
  sexo?: string
  telefone?: string
  email?: string
  endereco?: Endereco
  alergias?: string
  observacoes?: string
  ativo?: boolean
}
