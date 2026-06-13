import { onlyDigits } from '@/lib/cadastro-options'

/**
 * Validações do cadastro de clientes (nome, CPF, contato, endereço, data de nascimento).
 *
 * Alteração — nome completo:
 * - Limite reduzido de 150 → 100 caracteres (migration V6, ClienteValidacao, DTOs e input do front).
 * - sanitizeNomePessoa corta na digitação/cola; validarNomePessoa exibe erro se ultrapassar.
 */

/** Nome e sobrenome: só letras, um espaço entre cada parte. Máx. 100 caracteres (coluna clientes.nome). */
const NOME_PESSOA = /^[\p{L}]+(?: [\p{L}]+)+$/u

/** Espelha ClienteValidacao.NOME_MAX_LENGTH e VARCHAR(100) em clientes.nome. */
export const NOME_PESSOA_MAX = 100

export function sanitizeNomePessoa(value: string): string {
  return value
    .replace(/[^\p{L}\s]/gu, '') // remove números, hífen e símbolos
    .replace(/^\s+/, '')
    .replace(/\s{2,}/g, ' ') // colapsa espaços duplos
    .slice(0, NOME_PESSOA_MAX) // trava 100 chars no front (igual maxLength do input)
}

export function validarNomePessoa(nome: string): string | null {
  const normalizado = sanitizeNomePessoa(nome).trim()
  if (!normalizado) return 'Nome completo é obrigatório.'
  // Defesa extra: slice já limita, mas valida antes do submit/API.
  if (normalizado.length > NOME_PESSOA_MAX) {
    return `Nome completo deve ter no máximo ${NOME_PESSOA_MAX} caracteres.`
  }
  if (!NOME_PESSOA.test(normalizado)) {
    return 'Use apenas letras e um espaço entre nome e sobrenome (sem hífen ou números).'
  }
  return null
}

export function maskDataNascimentoBr(value: string): string {
  const d = onlyDigits(value).slice(0, 8)
  if (d.length <= 2) return d
  if (d.length <= 4) return `${d.slice(0, 2)}/${d.slice(2)}`
  return `${d.slice(0, 2)}/${d.slice(2, 4)}/${d.slice(4)}`
}

export function dataIsoParaBr(iso: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso.trim())
  if (!match) return iso
  return `${match[3]}/${match[2]}/${match[1]}`
}

export function dataBrParaIso(br: string): string | null {
  const err = validarDataNascimentoBr(br)
  if (err) return null
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(br.trim())!
  return `${match[3]}-${match[2]}-${match[1]}`
}

/** Valida data DD/MM/AAAA incluindo dias inexistentes (ex.: 31/02). */
export function validarDataNascimentoBr(br: string): string | null {
  if (!br.trim()) return null

  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(br.trim())
  if (!match) return 'Informe a data completa no formato DD/MM/AAAA.'

  const day = Number(match[1])
  const month = Number(match[2])
  const year = Number(match[3])

  if (month < 1 || month > 12) return 'Mês inválido.'

  const date = new Date(year, month - 1, day)
  if (
    date.getFullYear() !== year
    || date.getMonth() !== month - 1
    || date.getDate() !== day
  ) {
    return 'Data inválida — verifique dia e mês (ex.: fevereiro não tem dia 31).'
  }

  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  if (date > hoje) return 'Data de nascimento não pode ser futura.'

  if (year < 1900) return 'Data de nascimento inválida.'

  if (!temIdadeMinima(day, month, year, 18)) {
    return 'Cliente deve ter 18 anos ou mais para cadastro.'
  }

  return null
}

/** True se a pessoa completou `idadeMin` anos (inclusive no dia do aniversário). */
export function temIdadeMinima(day: number, month: number, year: number, idadeMin = 18): boolean {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const dataAniversarioMinimo = new Date(year + idadeMin, month - 1, day)
  dataAniversarioMinimo.setHours(0, 0, 0, 0)
  return dataAniversarioMinimo <= hoje
}

const DATA_NASCIMENTO_MIN_ISO = '1900-01-01'

/** Data mínima permitida no calendário (YYYY-MM-DD). */
export function dataIsoMinNascimentoCadastro(): string {
  return DATA_NASCIMENTO_MIN_ISO
}

/** Última data selecionável: quem nasce neste dia já completou 18 anos hoje (YYYY-MM-DD). */
export function dataIsoMaxNascimentoCadastro(): string {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const limite = new Date(hoje)
  limite.setFullYear(limite.getFullYear() - 18)
  const y = limite.getFullYear()
  const m = String(limite.getMonth() + 1).padStart(2, '0')
  const d = String(limite.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** Converte DD/MM/AAAA completo para ISO; retorna '' se inválido ou incompleto. */
export function dataBrParaIsoSafe(br: string): string {
  if (br.trim().length !== 10) return ''
  return dataBrParaIso(br) ?? ''
}

/** @deprecated use validarDataNascimentoBr — mantido para compatibilidade interna */
export function validarDataNascimentoIso(iso: string): string | null {
  if (!iso.trim()) return null
  return validarDataNascimentoBr(dataIsoParaBr(iso))
}

export function validarTelefone(telefone: string, obrigatorio = false): string | null {
  if (!telefone.trim()) {
    return obrigatorio ? 'Telefone é obrigatório.' : null
  }
  const digits = onlyDigits(telefone)
  if (digits.length < 10 || digits.length > 11) {
    return 'Telefone deve conter 10 ou 11 dígitos.'
  }
  return null
}

/** Máscara BR: (11) 99999-9999 ou (11) 9999-9999 */
export function formatTelefoneDisplay(value: string): string {
  const d = onlyDigits(value).slice(0, 11)
  if (d.length === 0) return ''
  if (d.length <= 2) return `(${d}`
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
}

/** Remove qualquer espaço (incl. NBSP e unicode) — e-mail não aceita espaços. */
export function sanitizeEmailInput(value: string): string {
  return value.replace(/[\s\u00A0\u1680\u2000-\u200A\u202F\u205F\u3000\uFEFF]+/g, '')
}

/**
 * Formato universal simplificado (RFC 5321): rótulos de domínio + TLD só letras (2–63).
 * A regex antiga aceitava falsos positivos, ex.: helena@gmail.com.lixo...1 (TLD numérico).
 */
const EMAIL_FORMATO_UNIVERSAL =
  /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\.[a-zA-Z]{2,63}$/

function emailFormatoUniversalValido(email: string): boolean {
  if (email.length > 254) return false // RFC 5321 — tamanho máximo do endereço
  if (!EMAIL_FORMATO_UNIVERSAL.test(email)) return false
  const at = email.indexOf('@')
  if (at < 1) return false
  const local = email.slice(0, at)
  const domain = email.slice(at + 1)
  // Pontos consecutivos ou nas extremidades invalidam o endereço.
  if (local.startsWith('.') || local.endsWith('.') || local.includes('..')) return false
  if (domain.startsWith('.') || domain.endsWith('.') || domain.includes('..')) return false
  return true
}

export function validarEmail(email: string, obrigatorio = false): string | null {
  if (!email.trim()) {
    return obrigatorio ? 'E-mail é obrigatório.' : null
  }
  if (/\s/.test(email)) {
    return 'E-mail não pode conter espaços.'
  }
  const normalizado = email.trim().toLowerCase()
  // Alteração: validação estrita de formato universal (TLD alfabético; domínio sem lixo após .com).
  if (!emailFormatoUniversalValido(normalizado)) {
    return 'E-mail inválido.'
  }
  return null
}

export function validarSexo(sexo: string): string | null {
  if (!sexo?.trim()) return 'Sexo é obrigatório.'
  return null
}

export function normalizarEmail(email: string): string {
  return email.trim().toLowerCase()
}

export function validarCpf(cpf: string): string | null {
  const digits = onlyDigits(cpf)
  if (digits.length !== 11) return 'CPF deve conter 11 dígitos.'
  if (/^(\d)\1+$/.test(digits)) return 'CPF inválido.'

  const calc = (length: number) => {
    let sum = 0
    for (let i = 0; i < length; i++) {
      sum += Number(digits[i]) * (length + 1 - i)
    }
    const rest = sum % 11
    return rest < 2 ? 0 : 11 - rest
  }

  if (calc(9) !== Number(digits[9]) || calc(10) !== Number(digits[10])) {
    return 'CPF inválido.'
  }
  return null
}

export function validarLogradouro(logradouro: string): string | null {
  if (!logradouro.trim()) return 'Logradouro é obrigatório.'
  return null
}

export function validarBairro(bairro: string): string | null {
  if (!bairro.trim()) return 'Bairro é obrigatório.'
  return null
}

export function validarCep(cep: string): string | null {
  const digits = onlyDigits(cep)
  if (!digits) return 'CEP é obrigatório.'
  if (digits.length !== 8) return 'CEP deve conter 8 dígitos.'
  return null
}

export function dataIsoHoje(): string {
  const hoje = new Date()
  const y = hoje.getFullYear()
  const m = String(hoje.getMonth() + 1).padStart(2, '0')
  const d = String(hoje.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
