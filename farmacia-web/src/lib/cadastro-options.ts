import type { FormaFarmaceutica, NivelControle, TipoMedicamento } from '@/types/api'

export const TIPOS_MEDICAMENTO: TipoMedicamento[] = [
  'GENERICO',
  'REFERENCIA',
  'SIMILAR',
  'BIOLOGICO',
  'FITOTERAPICO',
  'HOMEOPATICO',
  'OTC',
]

export const FORMAS_FARMACEUTICAS: FormaFarmaceutica[] = [
  'COMPRIMIDO',
  'CAPSULA',
  'XAROPE',
  'SOLUCAO',
  'SUSPENSAO',
  'INJETAVEL',
  'POMADA',
  'CREME',
  'GEL',
  'SUPOSITORIO',
  'COLIRIO',
  'SPRAY',
  'PATCH',
  'PO',
]

export const NIVEIS_CONTROLE: NivelControle[] = [
  'LIVRE',
  'RECEITA_SIMPLES',
  'ANTIMICROBIANO',
  'CONTROLADO_C1',
  'CONTROLADO_C2',
  'CONTROLADO_B1',
  'CONTROLADO_B2',
]

export const UFS_BR = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
  'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
]

export const TAB_LABELS: Record<string, string> = {
  medicamentos: 'Medicamentos',
  clientes: 'Clientes',
  fabricantes: 'Fabricantes',
  fornecedores: 'Fornecedores',
  categorias: 'Categorias',
  prescritores: 'Prescritores',
}

export type CadastroTab = keyof typeof TAB_LABELS

export function labelTipoMedicamento(tipo: TipoMedicamento): string {
  const map: Record<TipoMedicamento, string> = {
    GENERICO: 'Genérico',
    REFERENCIA: 'Referência',
    SIMILAR: 'Similar',
    BIOLOGICO: 'Biológico',
    FITOTERAPICO: 'Fitoterápico',
    HOMEOPATICO: 'Homeopático',
    OTC: 'OTC (isento)',
  }
  return map[tipo] ?? tipo
}

export function labelFormaFarmaceutica(forma: FormaFarmaceutica): string {
  const map: Record<FormaFarmaceutica, string> = {
    COMPRIMIDO: 'Comprimido',
    CAPSULA: 'Cápsula',
    XAROPE: 'Xarope',
    SOLUCAO: 'Solução',
    SUSPENSAO: 'Suspensão',
    INJETAVEL: 'Injetável',
    POMADA: 'Pomada',
    CREME: 'Creme',
    GEL: 'Gel',
    SUPOSITORIO: 'Supositório',
    COLIRIO: 'Colírio',
    SPRAY: 'Spray',
    PATCH: 'Adesivo (patch)',
    PO: 'Pó',
  }
  return map[forma] ?? forma
}

export function onlyDigits(value: string): string {
  return value.replace(/\D/g, '')
}

export function formatCpfDisplay(cpf: string): string {
  const d = onlyDigits(cpf)
  if (d.length !== 11) return cpf
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
}

/** Máscara CNPJ conforme padrão brasileiro: 00.000.000/0000-00 */
export function maskCnpjInput(value: string): string {
  const d = onlyDigits(value).slice(0, 14)
  if (d.length === 0) return ''
  if (d.length <= 2) return d
  if (d.length <= 5) return `${d.slice(0, 2)}.${d.slice(2)}`
  if (d.length <= 8) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5)}`
  if (d.length <= 12) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8)}`
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`
}

export function formatCnpjDisplay(cnpj: string): string {
  return maskCnpjInput(cnpj)
}

export interface SelectOptionLike {
  value: string
  label: string
  sublabel?: string
}

/** Ordem A–Z por nome comercial (pt-BR), para selects de medicamento. */
export function sortSelectOptionsAlphabetically<T extends SelectOptionLike>(
  options: T[],
): T[] {
  return [...options].sort((a, b) =>
    a.label.localeCompare(b.label, 'pt-BR', { sensitivity: 'base' }),
  )
}

export function medicamentosToSelectOptions<
  M extends { id: string; nomeComercial: string; nomeGenerico?: string },
>(
  medicamentos: M[],
  formatSublabel?: (m: M) => string,
): SelectOptionLike[] {
  const opts = medicamentos.map((m) => ({
    value: m.id,
    label: m.nomeComercial,
    sublabel: formatSublabel?.(m) ?? m.nomeGenerico,
  }))
  return sortSelectOptionsAlphabetically(opts)
}
