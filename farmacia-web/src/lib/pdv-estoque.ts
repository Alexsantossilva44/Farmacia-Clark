import type { ItemEstoque, DisponivelVenda } from '@/types/estoque'
import type { CartItem } from '@/types/venda'

export type SaldoPdvMap = Map<string, number>

/** Saldo dispensável (lotes ativos e não vencidos), alinhado ao backend. */
export function buildSaldoPdvMap(itens: ItemEstoque[]): SaldoPdvMap {
  const map = new Map<string, number>()
  for (const item of itens) {
    const disp =
      item.quantidadeDisponivelVenda ?? item.quantidadeAtual ?? 0
    map.set(item.medicamentoId, Math.max(0, disp))
  }
  return map
}

export function buildSaldoPdvMapFromDisponivel(itens: DisponivelVenda[]): SaldoPdvMap {
  const map = new Map<string, number>()
  for (const item of itens) {
    map.set(item.medicamentoId, Math.max(0, item.quantidadeDisponivelVenda))
  }
  return map
}

export function getSaldoPdv(map: SaldoPdvMap, medicamentoId: string): number {
  return map.get(medicamentoId) ?? 0
}

export function saldoPdvLabel(saldo: number): string {
  if (saldo <= 0) return 'Sem estoque'
  return saldo === 1 ? '1 un. em estoque' : `${saldo} un. em estoque`
}

export interface ValidacaoCarrinhoEstoque {
  ok: boolean
  message?: string
}

export function validarCarrinhoEstoque(
  cart: CartItem[],
  saldos: SaldoPdvMap,
): ValidacaoCarrinhoEstoque {
  for (const item of cart) {
    const disponivel = getSaldoPdv(saldos, item.medicamento.id)
    if (item.quantidade > disponivel) {
      if (disponivel <= 0) {
        return {
          ok: false,
          message: `"${item.medicamento.nomeComercial}" não possui estoque disponível. Remova do carrinho ou registre entrada em Estoque/Compras.`,
        }
      }
      return {
        ok: false,
        message: `"${item.medicamento.nomeComercial}": carrinho tem ${item.quantidade} un., disponível ${disponivel}.`,
      }
    }
  }
  return { ok: true }
}

export function cartExcedeEstoque(cart: CartItem[], saldos: SaldoPdvMap): boolean {
  return !validarCarrinhoEstoque(cart, saldos).ok
}
