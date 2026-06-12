/**
 * Validação reutilizável de formulários — padrão Farmácia CLARK.
 *
 * Motivo: antes, várias telas desabilitavam o botão "Cadastrar" quando faltavam dados,
 * sem mostrar mensagem no campo. Agora, ao clicar em salvar, todos os obrigatórios
 * são validados de uma vez, exibem erro em vermelho e a tela rola/foca no primeiro problema.
 */
import { onlyDigits } from '@/lib/cadastro-options'

/** Mensagem padrão para campos obrigatórios vazios. */
export function validarObrigatorio(valor: string | null | undefined, nomeCampo: string): string | null {
  if (!valor?.trim()) return `${nomeCampo} é obrigatório.`
  return null
}

/** Select / combobox sem valor selecionado. */
export function validarSelecao(valor: string | null | undefined, nomeCampo: string): string | null {
  return validarObrigatorio(valor, nomeCampo)
}

export function validarCnpj(cnpj: string, obrigatorio = true): string | null {
  const digits = onlyDigits(cnpj)
  if (!digits) return obrigatorio ? 'CNPJ é obrigatório.' : null
  if (digits.length !== 14) return 'CNPJ deve conter 14 dígitos.'
  return null
}

export function validarNumeroPositivo(
  valor: number | string,
  nomeCampo: string,
): string | null {
  const n = typeof valor === 'number' ? valor : parseFloat(String(valor).replace(',', '.'))
  if (!Number.isFinite(n) || n <= 0) return `${nomeCampo} é obrigatório.`
  return null
}

export function validarQuantidade(valor: string | number, nomeCampo = 'Quantidade'): string | null {
  const n =
    typeof valor === 'number'
      ? valor
      : parseInt(String(valor).replace(/\D/g, ''), 10)
  if (!Number.isFinite(n) || n <= 0) return `${nomeCampo} é obrigatório.`
  return null
}

export function validarMotivoMinimo(
  motivo: string,
  min: number,
  nomeCampo = 'Motivo',
): string | null {
  const t = motivo.trim()
  if (!t) return `${nomeCampo} é obrigatório.`
  if (t.length < min) return `${nomeCampo} deve ter pelo menos ${min} caracteres.`
  return null
}

export function validarDataObrigatoria(
  data: string,
  nomeCampo: string,
  opcoes?: { min?: string },
): string | null {
  if (!data?.trim()) return `${nomeCampo} é obrigatória.`
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
