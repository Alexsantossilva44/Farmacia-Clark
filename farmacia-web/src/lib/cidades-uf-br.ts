import cidadesUf from './cidades-uf-br.json'
import { MSG_OBRIGATORIO } from '@/lib/validacao-formulario'

const MAPA_CIDADES_UF = cidadesUf as Record<string, string>

/** Índice UF → chaves normalizadas dos municípios (IBGE). */
const CIDADES_POR_UF: Record<string, string[]> = {}
for (const [cidade, uf] of Object.entries(MAPA_CIDADES_UF)) {
  if (!CIDADES_POR_UF[uf]) CIDADES_POR_UF[uf] = []
  CIDADES_POR_UF[uf].push(cidade)
}
for (const uf of Object.keys(CIDADES_POR_UF)) {
  CIDADES_POR_UF[uf].sort((a, b) => a.localeCompare(b, 'pt-BR'))
}

export interface OpcaoCidade {
  value: string
  label: string
}

const PREPOSICOES = new Set(['de', 'da', 'do', 'das', 'dos', 'e'])

export function formatarNomeCidadeExibicao(chave: string): string {
  return chave
    .split(' ')
    .map((parte, i) => {
      if (!parte) return ''
      if (i > 0 && PREPOSICOES.has(parte)) return parte
      return parte.charAt(0).toUpperCase() + parte.slice(1)
    })
    .join(' ')
}

/** Opções de municípios para uma UF (ordenadas por nome de exibição). */
export function listarOpcoesCidadesPorUf(uf: string): OpcaoCidade[] {
  const chaves = CIDADES_POR_UF[uf] ?? []
  return chaves.map((chave) => ({
    value: chave,
    label: formatarNomeCidadeExibicao(chave),
  }))
}

/** Filtra municípios da UF pelo texto digitado. */
export function filtrarOpcoesCidades(opcoes: OpcaoCidade[], filtro: string): OpcaoCidade[] {
  const q = normalizarNomeCidade(filtro)
  if (!q) return opcoes
  return opcoes.filter(
    (o) => normalizarNomeCidade(o.label).includes(q) || o.value.includes(q),
  )
}

/** True se o município pertence à UF informada. */
export function municipioPertenceAUf(cidade: string, uf: string): boolean {
  if (!uf || !cidade.trim()) return false
  const chave = normalizarNomeCidade(cidade)
  return MAPA_CIDADES_UF[chave] === uf
}

/** Nomes oficiais dos estados (quando o usuário confunde com município). */
const MAPA_ESTADOS_UF: Record<string, string> = {
  acre: 'AC',
  alagoas: 'AL',
  amapa: 'AP',
  amazonas: 'AM',
  bahia: 'BA',
  ceara: 'CE',
  'distrito federal': 'DF',
  'espirito santo': 'ES',
  goias: 'GO',
  maranhao: 'MA',
  'mato grosso': 'MT',
  'mato grosso do sul': 'MS',
  'minas gerais': 'MG',
  para: 'PA',
  paraiba: 'PB',
  parana: 'PR',
  pernambuco: 'PE',
  piaui: 'PI',
  'rio de janeiro': 'RJ',
  'rio grande do norte': 'RN',
  'rio grande do sul': 'RS',
  rondonia: 'RO',
  roraima: 'RR',
  'santa catarina': 'SC',
  'sao paulo': 'SP',
  sergipe: 'SE',
  tocantins: 'TO',
}

export function normalizarNomeCidade(cidade: string): string {
  return cidade
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ')
}

/** Município reconhecido na base IBGE. */
export function resolverUfPorMunicipio(cidade: string): string | null {
  const chave = normalizarNomeCidade(cidade)
  if (!chave) return null
  return MAPA_CIDADES_UF[chave] ?? null
}

/** True quando o texto é nome de estado, mas não um município homônimo (ex.: Minas Gerais). */
export function ehNomeDeEstado(cidade: string): boolean {
  const chave = normalizarNomeCidade(cidade)
  if (!chave) return false
  return chave in MAPA_ESTADOS_UF && !(chave in MAPA_CIDADES_UF)
}

/** Retorna UF por município ou, em último caso, por nome do estado. */
export function resolverUfPorCidade(cidade: string): string | null {
  const chave = normalizarNomeCidade(cidade)
  if (!chave) return null
  return MAPA_CIDADES_UF[chave] ?? MAPA_ESTADOS_UF[chave] ?? null
}

/** UF passou a ser obrigatória no endereço do cliente (regra de negócio + submit). */
export function validarUfEndereco(uf: string): string | null {
  if (!uf?.trim()) return MSG_OBRIGATORIO
  return null
}

/** Exige município preenchido e coerente com a UF (lista IBGE), não só validação opcional ao digitar. */
export function validarCidadeObrigatoria(cidade: string, uf: string): string | null {
  if (!cidade?.trim()) return MSG_OBRIGATORIO
  const ufErr = validarUfEndereco(uf)
  if (ufErr) return 'Selecione a UF antes da cidade.'
  return validarCidadeEndereco(cidade, uf)
}

export function validarCidadeEndereco(cidade: string, uf?: string): string | null {
  const chave = normalizarNomeCidade(cidade)
  if (!chave) return null

  if (uf) {
    if (!MAPA_CIDADES_UF[chave]) {
      return 'Selecione um município da lista.'
    }
    if (MAPA_CIDADES_UF[chave] !== uf) {
      return 'Município não pertence à UF selecionada.'
    }
    return null
  }

  if (ehNomeDeEstado(cidade)) {
    return 'Informe o município (ex.: Belo Horizonte), não o nome do estado.'
  }
  if (!MAPA_CIDADES_UF[chave]) {
    return 'Município não reconhecido. Verifique a grafia ou preencha a UF manualmente.'
  }
  return null
}
