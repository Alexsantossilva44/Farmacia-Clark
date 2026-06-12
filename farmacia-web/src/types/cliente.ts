export interface Endereco {
  logradouro?: string
  numero?: string
  complemento?: string
  bairro?: string
  cidade?: string
  uf?: string
  cep?: string
}

export interface Cliente {
  id: string
  nome: string
  cpf: string
  dataNascimento?: string
  sexo?: string
  telefone?: string
  email?: string
  endereco?: Endereco
  alergias?: string
  observacoes?: string
  ativo?: boolean
  dataCadastro?: string
}
