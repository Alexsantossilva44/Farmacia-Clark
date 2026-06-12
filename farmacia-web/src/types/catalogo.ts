/**
 * Tipos do catálogo auxiliar (fabricantes, categorias, prescritores).
 *
 * Alterações recentes nos payloads de cadastro:
 * - FabricanteInput.cnpj obrigatório (NOT NULL no banco).
 * - CategoriaInput.descricao e PrescritorInput.especialidade obrigatórios (@NotBlank na API).
 */
export interface Fabricante {
  id: string
  razaoSocial: string
  nomeFantasia?: string
  cnpj?: string
  ativo?: boolean
}

export interface Categoria {
  id: string
  nome: string
  descricao?: string
  ativo?: boolean
}

export interface Prescritor {
  id: string
  nome: string
  crm: string
  ufCrm: string
  especialidade?: string
  email?: string
  ativo?: boolean
}

/** Payload de cadastro: cnpj obrigatório (espelha @NotBlank da API e NOT NULL no banco). */
export interface FabricanteInput {
  razaoSocial: string
  nomeFantasia?: string
  cnpj: string
}

/** Payload de cadastro: descricao obrigatória (espelha @NotBlank da API). */
export interface CategoriaInput {
  nome: string
  descricao: string
}

/** Payload de cadastro: especialidade obrigatória (espelha @NotBlank da API). */
export interface PrescritorInput {
  nome: string
  crm: string
  ufCrm: string
  especialidade: string
  email?: string
}

/** IDs seed dev — espelham DevOperacionalSeed */
export const FABRICANTE_DEV_ID = '11111111-1111-1111-1111-111111111111'
export const CATEGORIA_DEV_ID = '22222222-2222-2222-2222-222222222222'
