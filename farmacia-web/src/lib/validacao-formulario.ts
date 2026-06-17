/**
 * Validação reutilizável de formulários — padrão Farmácia CLARK.
 *
 * Motivo: antes, várias telas desabilitavam o botão "Cadastrar" quando faltavam dados,
 * sem mostrar mensagem no campo. Agora, ao clicar em salvar, todos os obrigatórios
 * são validados de uma vez, exibem erro em vermelho e a tela rola/foca no primeiro problema.
 */
import { onlyDigits } from '@/lib/cadastro-options'

/** Mensagem padrão do sistema para campos obrigatórios — exibida 2 s e limpa automaticamente. */
export const MSG_OBRIGATORIO = 'Lembre-se: Campo Obrigatório.'

/** Retorna MSG_OBRIGATORIO se vazio, null se preenchido. */
export function validarObrigatorio(valor: string | null | undefined, _nomeCampo?: string): string | null {
  return valor?.trim() ? null : MSG_OBRIGATORIO
}

/** Select / combobox sem valor selecionado. */
export function validarSelecao(valor: string | null | undefined, _nomeCampo?: string): string | null {
  return validarObrigatorio(valor)
}

export function validarCnpj(cnpj: string, obrigatorio = true): string | null {
  const digits = onlyDigits(cnpj)
  if (!digits) return obrigatorio ? MSG_OBRIGATORIO : null
  if (digits.length !== 14) return 'CNPJ deve conter 14 dígitos.'
  if (/^(\d)\1+$/.test(digits)) return 'CNPJ inválido.'
  const calc = (len: number) => {
    let sum = 0
    let weight = len - 8
    for (let i = len - 1; i >= 1; i--) {
      sum += Number(digits[len - 1 - i]) * weight--
      if (weight < 2) weight = 9
    }
    const rem = sum % 11
    return rem < 2 ? 0 : 11 - rem
  }
  if (calc(13) !== Number(digits[12]) || calc(14) !== Number(digits[13])) return 'CNPJ inválido.'
  return null
}

export function validarNumeroPositivo(
  valor: number | string,
  _nomeCampo?: string,
): string | null {
  const n = typeof valor === 'number' ? valor : parseFloat(String(valor).replace(',', '.'))
  if (!Number.isFinite(n) || n <= 0) return MSG_OBRIGATORIO
  return null
}

export function validarQuantidade(valor: string | number, _nomeCampo?: string): string | null {
  const n =
    typeof valor === 'number'
      ? valor
      : parseInt(String(valor).replace(/\D/g, ''), 10)
  if (!Number.isFinite(n) || n <= 0) return MSG_OBRIGATORIO
  return null
}

export function validarMotivoMinimo(
  motivo: string,
  min: number,
  nomeCampo = 'Motivo',
): string | null {
  const t = motivo.trim()
  if (!t) return MSG_OBRIGATORIO
  if (t.length < min) return `${nomeCampo} deve ter pelo menos ${min} caracteres.`
  return null
}

export function validarDataObrigatoria(
  data: string,
  nomeCampo: string,
  opcoes?: { min?: string },
): string | null {
  if (!data?.trim()) return MSG_OBRIGATORIO
  if (opcoes?.min && data < opcoes.min) {
    return `${nomeCampo} deve ser hoje ou uma data futura.`
  }
  return null
}

/** Rolagem suave + foco no primeiro Input/Select com mensagem .text-coral (prop error dos componentes UI). */
export function focarPrimeiroErro(scope?: ParentNode | null): void {
  requestAnimationFrame(() => {
    const root = scope ?? document
    const container =
      root.querySelector('.space-y-1\\.5:has(.text-coral)')
      ?? root.querySelector('.space-y-1\\.5:has(p.text-coral)')

    if (container) {
      const focusable = container.querySelector(
        'input:not([disabled]), textarea:not([disabled]), button[type="button"]:not([disabled])',
      ) as HTMLElement | null
      container.scrollIntoView({ behavior: 'smooth', block: 'center' })
      focusable?.focus({ preventScroll: true })
      return
    }

    root.querySelector('.text-coral')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}
