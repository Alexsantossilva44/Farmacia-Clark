import type { NivelControle } from '@/types/api'

const currency = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

export function formatCurrency(value: number): string {
  return currency.format(value)
}

/** Arredonda valor monetário para 2 casas (evita 194.799999… no JSON). */
export function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100
}

/** Valor exibido em campo de quantidade (sem zero à esquerda). */
export function quantidadeInputValue(qtd: number | undefined | null): string {
  if (qtd == null || qtd <= 0) return ''
  return String(qtd)
}

/** Normaliza digitação: "0300" → "300". */
export function normalizeQuantidadeInput(raw: string): string {
  const digits = raw.replace(/\D/g, '')
  if (digits === '') return ''
  return String(parseInt(digits, 10) || 0)
}

export function parseQuantidadeInput(raw: string): number {
  const digits = raw.replace(/\D/g, '')
  if (digits === '') return 0
  return parseInt(digits, 10) || 0
}

/** Valor exibido em campo monetário (evita 0 fixo e zeros à esquerda). */
export function precoInputValue(preco?: number | null): string | number {
  if (preco == null || preco <= 0) return ''
  return preco
}

export function parsePrecoInput(raw: string): number | undefined {
  const trimmed = raw.trim().replace(',', '.')
  if (trimmed === '') return undefined
  const n = parseFloat(trimmed)
  return Number.isFinite(n) && n >= 0 ? n : undefined
}

/** Sugestão de custo de compra (~60% do PMC), alinhada ao seed de desenvolvimento. */
export function sugerirPrecoCustoFromPmc(pmc: number): number {
  return roundMoney(pmc * 0.6)
}

export function formatEan(ean: string): string {
  if (ean.length !== 13) return ean
  return `${ean.slice(0, 1)} ${ean.slice(1, 7)} ${ean.slice(7, 12)} ${ean.slice(12)}`
}

const NIVEL_LABELS: Record<NivelControle, string> = {
  LIVRE: 'Livre',
  RECEITA_SIMPLES: 'Receita simples',
  ANTIMICROBIANO: 'Antimicrobiano',
  CONTROLADO_B1: 'Lista B1',
  CONTROLADO_B2: 'Lista B2',
  CONTROLADO_C1: 'Lista C1',
  CONTROLADO_C2: 'Lista C2',
}

export function nivelControleLabel(nivel: NivelControle): string {
  return NIVEL_LABELS[nivel] ?? nivel
}

export type BadgeVariant = 'mint' | 'sky' | 'amber' | 'coral' | 'violet'

export function nivelControleVariant(nivel: NivelControle): BadgeVariant {
  switch (nivel) {
    case 'LIVRE':
      return 'mint'
    case 'RECEITA_SIMPLES':
      return 'sky'
    case 'ANTIMICROBIANO':
      return 'amber'
    case 'CONTROLADO_B1':
    case 'CONTROLADO_B2':
      return 'coral'
    case 'CONTROLADO_C1':
    case 'CONTROLADO_C2':
      return 'violet'
    default:
      return 'sky'
  }
}

export function roleLabel(role: string): string {
  const map: Record<string, string> = {
    ROLE_ADMIN: 'Administrador',
    ROLE_GERENTE: 'Gerente',
    ROLE_FARMACEUTICO: 'Farmacêutico',
    ROLE_BALCONISTA: 'Balconista',
    ROLE_ESTOQUISTA: 'Estoquista',
  }
  return map[role] ?? role.replace('ROLE_', '')
}

const FORMA_PAGAMENTO_LABELS: Record<string, string> = {
  DINHEIRO: 'Dinheiro',
  CARTAO_DEBITO: 'Cartão débito',
  CARTAO_CREDITO: 'Cartão crédito',
  PIX: 'PIX',
  CONVENIO: 'Convênio',
  VALE_FARMACIA: 'Vale farmácia',
  CREDIARIO: 'Crediário',
}

export function formaPagamentoLabel(forma: string): string {
  return FORMA_PAGAMENTO_LABELS[forma] ?? forma
}

const TIPO_RECEITA_LABELS: Record<string, string> = {
  SIMPLES: 'Simples',
  AZUL: 'Azul (C1)',
  AMARELA: 'Amarela (C2)',
  BRANCA_ESPECIAL: 'Branca especial (B1)',
  ANTIMICROBIANO: 'Antimicrobiano',
}

export function tipoReceitaLabel(tipo: string): string {
  return TIPO_RECEITA_LABELS[tipo] ?? tipo
}

const STATUS_RECEITA_VARIANT: Record<string, BadgeVariant> = {
  PENDENTE: 'amber',
  APROVADA: 'mint',
  REJEITADA: 'coral',
  UTILIZADA: 'sky',
  VENCIDA: 'coral',
  SUSPENSA: 'violet',
}

export function statusReceitaVariant(status: string): BadgeVariant {
  return STATUS_RECEITA_VARIANT[status] ?? 'sky'
}

const STATUS_RECEITA_LABELS: Record<string, string> = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  REJEITADA: 'Rejeitada',
  UTILIZADA: 'Utilizada',
  VENCIDA: 'Vencida',
  SUSPENSA: 'Suspensa',
}

export function statusReceitaLabel(status: string): string {
  return STATUS_RECEITA_LABELS[status] ?? status
}

const STATUS_ALERTA_LABELS: Record<string, string> = {
  ABERTO: 'Aberto',
  RESOLVIDO: 'Resolvido',
  IGNORADO: 'Ignorado',
}

export function statusAlertaLabel(status: string): string {
  return STATUS_ALERTA_LABELS[status] ?? status
}

const TIPO_ALERTA_LABELS: Record<string, string> = {
  VENCIMENTO_PROXIMO: 'Vencimento próximo',
  LOTE_VENCIDO: 'Lote vencido',
  ESTOQUE_MINIMO: 'Abaixo do mínimo',
  ESTOQUE_ZERADO: 'Estoque zerado',
}

export function tipoAlertaLabel(tipo: string): string {
  return TIPO_ALERTA_LABELS[tipo] ?? tipo
}

export function tipoAlertaVariant(tipo: string): BadgeVariant {
  if (tipo.includes('VENC')) return 'amber'
  if (tipo.includes('ZERADO') || tipo === 'ESTOQUE_MINIMO') return 'coral'
  return 'sky'
}

/** Data de hoje no fuso local, formato `YYYY-MM-DD` (para `min`/`max` em `<input type="date">`). */
export function dataIsoHojeLocal(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Evita "lote Lote …" quando o número já traz a palavra lote. */
export function rotuloNumeroLote(numeroLote: string): string {
  const n = numeroLote.trim()
  if (/^lote\b/i.test(n)) return n
  return `lote ${n}`
}

export function mensagemEntradaRegistrada(
  medicamentoNome: string,
  numeroLote: string,
  saldo: number,
): string {
  const nome = medicamentoNome.trim() || 'Medicamento'
  return `Entrada registrada — ${nome}, ${rotuloNumeroLote(numeroLote)}, saldo ${saldo} un.`
}

export function formatDateBR(iso: string): string {
  if (!iso) return '—'
  const [y, m, d] = iso.split('T')[0].split('-')
  return `${d}/${m}/${y}`
}

export function formatDateTimeBR(iso: string): string {
  if (!iso) return '—'
  const [datePart, timePart] = iso.split('T')
  const [y, m, d] = datePart.split('-')
  const hm = timePart ? timePart.slice(0, 5) : ''
  return hm ? `${d}/${m}/${y} ${hm}` : `${d}/${m}/${y}`
}

const TIPO_MOVIMENTACAO_LABELS: Record<string, string> = {
  ENTRADA_COMPRA: 'Entrada (compra)',
  ENTRADA_DEVOLUCAO_CLIENTE: 'Devolução cliente',
  SAIDA_VENDA: 'Saída (venda)',
  SAIDA_VENCIMENTO: 'Saída (vencimento)',
  SAIDA_PERDA: 'Saída (perda)',
  AJUSTE_POSITIVO: 'Ajuste (+)',
  AJUSTE_NEGATIVO: 'Ajuste (−)',
  TRANSFERENCIA: 'Transferência',
}

export function tipoMovimentacaoLabel(tipo: string): string {
  return TIPO_MOVIMENTACAO_LABELS[tipo] ?? tipo
}

export function tipoMovimentacaoVariant(tipo: string): BadgeVariant {
  if (tipo.startsWith('ENTRADA') || tipo === 'AJUSTE_POSITIVO') return 'mint'
  if (tipo.startsWith('SAIDA') || tipo === 'AJUSTE_NEGATIVO') return 'coral'
  return 'sky'
}
