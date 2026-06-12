import { useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ShoppingCart,
  Search,
  Plus,
  Minus,
  Trash2,
  CreditCard,
  CheckCircle2,
  AlertTriangle,
  Store,
  Receipt,
} from 'lucide-react'
import { fetchAllMedicamentos, fetchEstoqueDisponivelVenda, fetchPdvContexto, realizarVenda } from '@/lib/api'
import {
  buildSaldoPdvMapFromDisponivel,
  cartExcedeEstoque,
  getSaldoPdv,
  saldoPdvLabel,
  validarCarrinhoEstoque,
} from '@/lib/pdv-estoque'
import { getFuncionarioId } from '@/lib/auth'
import {
  formatCurrency,
  formaPagamentoLabel,
  nivelControleLabel,
  nivelControleVariant,
  roundMoney,
} from '@/lib/format'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Input } from '@/components/ui/Input'
import type { Medicamento } from '@/types/api'
import type { CartItem, FormaPagamento, VendaRealizada } from '@/types/venda'
import {
  cartRequiresCpf,
  cartRequiresReceita,
  cartSubtotal,
} from '@/types/venda'
import { traduzirErroApi } from '@/lib/erros'

const FORMAS_PAGAMENTO: FormaPagamento[] = [
  'DINHEIRO',
  'PIX',
  'CARTAO_DEBITO',
  'CARTAO_CREDITO',
]

export function VendasPage() {
  const cartRef = useRef<HTMLElement>(null)
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamento>('DINHEIRO')
  const [compradorCpf, setCompradorCpf] = useState('')
  const [compradorNome, setCompradorNome] = useState('')
  const [receitaId, setReceitaId] = useState('')
  const [success, setSuccess] = useState<{
    venda: VendaRealizada
    formaPagamento: FormaPagamento
  } | null>(null)
  const [error, setError] = useState('')

  const pdvQuery = useQuery({
    queryKey: ['pdv-contexto'],
    queryFn: () => fetchPdvContexto(),
    staleTime: 60_000,
  })

  const medsQuery = useQuery({
    queryKey: ['medicamentos-pdv'],
    queryFn: fetchAllMedicamentos,
    staleTime: 30_000,
  })

  const estoqueQuery = useQuery({
    queryKey: ['estoque-disponivel-venda-pdv'],
    queryFn: () => fetchEstoqueDisponivelVenda(),
    staleTime: 15_000,
  })

  const saldos = useMemo(
    () => buildSaldoPdvMapFromDisponivel(estoqueQuery.data ?? []),
    [estoqueQuery.data],
  )

  const total = cartSubtotal(cart)
  const carrinhoInvalidoEstoque = cart.length > 0 && cartExcedeEstoque(cart, saldos)

  const vendaMutation = useMutation({
    mutationFn: realizarVenda,
    onSuccess: (data) => {
      setSuccess({ venda: data, formaPagamento })
      setCart([])
      setCompradorCpf('')
      setCompradorNome('')
      setReceitaId('')
      setError('')
      void queryClient.invalidateQueries({ queryKey: ['estoque-disponivel-venda-pdv'] })
      void queryClient.invalidateQueries({ queryKey: ['estoque-itens'] })
    },
    onError: (err: unknown) => setError(traduzirErroApi(err)),
  })

  const filteredMeds = medsQuery.data?.filter((m) => {
    if (!m.ativo) return false
    if (!search.trim()) return true
    const q = search.toLowerCase()
    return (
      m.nomeComercial.toLowerCase().includes(q) ||
      m.nomeGenerico.toLowerCase().includes(q) ||
      m.codigoEan?.includes(q)
    )
  })

  function addToCart(med: Medicamento) {
    const saldo = getSaldoPdv(saldos, med.id)
    if (saldo <= 0) {
      setError(
        `"${med.nomeComercial}" está sem estoque. Registre entrada em Estoque ou Compras antes de vender.`,
      )
      return
    }

    setCart((prev) => {
      const existing = prev.find((i) => i.medicamento.id === med.id)
      if (existing) {
        if (existing.quantidade >= saldo) {
          setError(`Estoque máximo: ${saldo} un. para "${med.nomeComercial}".`)
          return prev
        }
        setError('')
        return prev.map((i) =>
          i.medicamento.id === med.id
            ? { ...i, quantidade: Math.min(i.quantidade + 1, saldo) }
            : i,
        )
      }
      setError('')
      return [
        ...prev,
        {
          medicamento: med,
          quantidade: 1,
          precoUnitario: med.precoMaximoConsumidor,
          desconto: 0,
        },
      ]
    })
  }

  function updateQty(medId: string, delta: number) {
    const saldo = getSaldoPdv(saldos, medId)
    setCart((prev) =>
      prev
        .map((i) => {
          if (i.medicamento.id !== medId) return i
          const next = Math.max(1, Math.min(saldo, i.quantidade + delta))
          if (delta > 0 && next === i.quantidade && i.quantidade >= saldo) {
            setError(`Estoque máximo: ${saldo} un. para "${i.medicamento.nomeComercial}".`)
          } else {
            setError('')
          }
          return { ...i, quantidade: next }
        })
        .filter((i) => i.quantidade > 0),
    )
  }

  function removeItem(medId: string) {
    setCart((prev) => prev.filter((i) => i.medicamento.id !== medId))
  }

  function handleFinalizar() {
    setError('')
    const funcionarioId = getFuncionarioId()
    const pdv = pdvQuery.data

    if (!funcionarioId) {
      setError('Sessão inválida — faça login novamente.')
      return
    }
    if (!pdv?.caixaAberto) {
      setError('Caixa fechado no PDV. Abra o caixa antes de vender.')
      return
    }
    if (cart.length === 0) {
      setError('Adicione ao menos um item ao carrinho.')
      return
    }
    if (cartRequiresReceita(cart) && !receitaId.trim()) {
      setError('Informe o ID da receita para medicamentos que exigem prescrição.')
      return
    }
    if (cartRequiresCpf(cart) && !compradorCpf.trim()) {
      setError('CPF do comprador é obrigatório para controlados/antimicrobianos.')
      return
    }
    const estoqueCheck = validarCarrinhoEstoque(cart, saldos)
    if (!estoqueCheck.ok) {
      setError(estoqueCheck.message ?? 'Estoque insuficiente no carrinho.')
      return
    }
    vendaMutation.mutate({
      pdv: { id: pdv.pdvId },
      funcionario: { id: funcionarioId },
      itens: cart.map((i) => ({
        medicamento: { id: i.medicamento.id },
        quantidade: i.quantidade,
        precoUnitario: roundMoney(i.precoUnitario),
        desconto: roundMoney(i.desconto),
      })),
      pagamentos: [{ forma: formaPagamento, valor: roundMoney(total) }],
      ...(receitaId.trim() ? { receita: { id: receitaId.trim() } } : {}),
      ...(compradorCpf.trim() ? { compradorCpf: compradorCpf.replace(/\D/g, '') } : {}),
      ...(compradorNome.trim() ? { compradorNome: compradorNome.trim() } : {}),
      tipoAtendimento: 'BALCAO',
    })
  }

  return (
    <div className="page-shell">
      <header className="page-header-band">
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-coral mb-1">
              <ShoppingCart className="size-4" />
              <span className="text-xs font-semibold uppercase tracking-widest">PDV</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">Nova venda</h1>
            {pdvQuery.data && (
              <div className="flex items-center gap-2 mt-2">
                <Store className="size-4 text-[#8b9cb3]" />
                <span className="text-sm text-[#8b9cb3]">
                  {pdvQuery.data.numero} · {pdvQuery.data.descricao ?? 'Caixa'}
                </span>
                <Badge variant={pdvQuery.data.caixaAberto ? 'mint' : 'coral'} dot>
                  {pdvQuery.data.caixaAberto ? 'Caixa aberto' : 'Caixa fechado'}
                </Badge>
              </div>
            )}
          </div>
          <Card className="py-3 px-5 shrink-0 w-full sm:w-auto" glow="mint">
            <p className="text-xs text-[#8b9cb3] uppercase tracking-wider">Total do carrinho</p>
            <p className="text-3xl font-bold font-mono text-mint">{formatCurrency(total)}</p>
          </Card>
        </div>

        {!pdvQuery.data?.caixaAberto && pdvQuery.isSuccess && (
          <div className="mt-4 px-4 py-3 rounded-xl bg-coral/10 border border-coral/25 text-sm text-coral flex items-center gap-2">
            <AlertTriangle className="size-4 shrink-0" />
            Caixa fechado — reinicie a API com perfil dev para abrir automaticamente.
          </div>
        )}
      </header>

      {/* Área rolável — produtos + carrinho */}
      <div className="page-scroll-body pb-24 xl:pb-6">
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(280px,380px)] gap-6">
        {/* Busca + produtos */}
        <section>
          <div className="relative mb-4">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-[#8b9cb3]" />
            <input
              type="search"
              placeholder="Buscar por nome ou EAN…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-3 rounded-xl glass text-sm focus:outline-none focus:ring-2 focus:ring-mint/30"
            />
          </div>

          {(medsQuery.isLoading || estoqueQuery.isLoading) && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="h-28 rounded-2xl glass animate-pulse bg-white/[0.02]" />
              ))}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {filteredMeds?.map((med) => {
              const saldo = getSaldoPdv(saldos, med.id)
              const semEstoque = saldo <= 0
              const noCarrinho = cart.find((i) => i.medicamento.id === med.id)?.quantidade ?? 0
              const limiteAtingido = noCarrinho >= saldo && saldo > 0

              return (
              <Card
                key={med.id}
                hover={!semEstoque}
                className={`group min-w-0 ${
                  semEstoque
                    ? 'opacity-55 cursor-not-allowed'
                    : 'cursor-pointer'
                }`}
                onClick={() => addToCart(med)}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className={`font-semibold truncate transition-colors ${
                      semEstoque ? 'text-[#8b9cb3]' : 'group-hover:text-mint'
                    }`}>
                      {med.nomeComercial}
                    </p>
                    <p className="text-xs text-[#8b9cb3] truncate">{med.nomeGenerico}</p>
                    <div className="flex flex-wrap items-center gap-1.5 mt-2">
                      <Badge variant={nivelControleVariant(med.nivelControle)}>
                        {nivelControleLabel(med.nivelControle)}
                      </Badge>
                      <span
                        className={`text-[10px] font-medium px-1.5 py-0.5 rounded-md border ${
                          semEstoque
                            ? 'text-coral border-coral/30 bg-coral/10'
                            : saldo <= 5
                              ? 'text-amber border-amber/30 bg-amber/10'
                              : 'text-[#8b9cb3] border-white/10 bg-white/[0.03]'
                        }`}
                      >
                        {estoqueQuery.isLoading ? '…' : saldoPdvLabel(saldo)}
                      </span>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="font-mono font-bold text-mint">
                      {formatCurrency(med.precoMaximoConsumidor)}
                    </p>
                    <p className="text-[10px] text-[#8b9cb3] mt-1">PMC</p>
                    {!semEstoque && (
                      <Plus
                        className={`size-5 mt-2 ml-auto transition-colors ${
                          limiteAtingido
                            ? 'text-[#8b9cb3]/40'
                            : 'text-[#8b9cb3] group-hover:text-mint'
                        }`}
                      />
                    )}
                  </div>
                </div>
              </Card>
            )})}
          </div>
        </section>

        {/* Carrinho + pagamento */}
        <aside ref={cartRef} className="min-w-0 lg:sticky lg:top-0 lg:self-start scroll-mt-24">
          <Card glow="coral" className="p-0 overflow-hidden flex flex-col lg:max-h-[calc(100dvh-12rem)]">
            <div className="px-5 py-4 border-b border-white/10 flex items-center gap-2 shrink-0">
              <Receipt className="size-4 text-coral" />
              <h2 className="font-semibold">Carrinho</h2>
              <span className="ml-auto text-xs text-[#8b9cb3]">{cart.length} itens</span>
            </div>

            {cart.length === 0 ? (
              <p className="px-5 py-10 text-center text-sm text-[#8b9cb3]">
                Clique em um medicamento para adicionar
              </p>
            ) : (
              <ul className="divide-y divide-white/5 flex-1 min-h-0 overflow-y-auto overscroll-contain">
                {cart.map((item) => {
                  const saldo = getSaldoPdv(saldos, item.medicamento.id)
                  const excede = item.quantidade > saldo
                  return (
                  <li key={item.medicamento.id} className="px-5 py-3">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="text-sm font-medium truncate">{item.medicamento.nomeComercial}</p>
                        <p className="text-xs font-mono text-mint mt-0.5">
                          {formatCurrency(item.precoUnitario)} × {item.quantidade}
                        </p>
                        <p className={`text-[10px] mt-0.5 ${excede ? 'text-coral' : 'text-[#8b9cb3]'}`}>
                          Disponível: {saldo} un.
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => removeItem(item.medicamento.id)}
                        className="text-[#8b9cb3] hover:text-coral p-1"
                        aria-label="Remover"
                      >
                        <Trash2 className="size-3.5" />
                      </button>
                    </div>
                    <div className="flex items-center gap-2 mt-2">
                      <button
                        type="button"
                        onClick={() => updateQty(item.medicamento.id, -1)}
                        className="size-7 rounded-lg glass flex items-center justify-center hover:border-mint/30"
                      >
                        <Minus className="size-3" />
                      </button>
                      <span className="text-sm font-mono w-8 text-center">{item.quantidade}</span>
                      <button
                        type="button"
                        onClick={() => updateQty(item.medicamento.id, 1)}
                        disabled={item.quantidade >= saldo}
                        className="size-7 rounded-lg glass flex items-center justify-center hover:border-mint/30 disabled:opacity-40 disabled:cursor-not-allowed"
                      >
                        <Plus className="size-3" />
                      </button>
                      <span className="ml-auto text-sm font-semibold font-mono">
                        {formatCurrency(item.precoUnitario * item.quantidade - item.desconto)}
                      </span>
                    </div>
                  </li>
                )})}
              </ul>
            )}

            <div className="px-5 py-4 border-t border-white/10 space-y-3 shrink-0">
              {cart.length > 0 && (
                <div className="flex items-center justify-between text-sm">
                  <span className="text-[#8b9cb3]">Subtotal do carrinho</span>
                  <span className="font-mono font-bold text-mint">{formatCurrency(total)}</span>
                </div>
              )}

              {(cartRequiresReceita(cart) || cartRequiresCpf(cart)) && (
                <div className="space-y-2 pb-2 border-b border-white/5">
                  {cartRequiresReceita(cart) && (
                    <Input
                      label="ID da receita (UUID)"
                      value={receitaId}
                      onChange={(e) => setReceitaId(e.target.value)}
                      placeholder="3fa85f64-5717-4562-b3fc-2c963f66afa6"
                      className="font-mono text-xs"
                    />
                  )}
                  {cartRequiresCpf(cart) && (
                    <>
                      <Input
                        label="CPF do comprador"
                        value={compradorCpf}
                        onChange={(e) => setCompradorCpf(e.target.value)}
                        placeholder="000.000.000-00"
                      />
                      <Input
                        label="Nome do comprador"
                        value={compradorNome}
                        onChange={(e) => setCompradorNome(e.target.value)}
                      />
                    </>
                  )}
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-[#8b9cb3] mb-1.5">
                  Forma de pagamento
                </label>
                <div className="grid grid-cols-2 gap-1.5">
                  {FORMAS_PAGAMENTO.map((f) => (
                    <button
                      key={f}
                      type="button"
                      onClick={() => setFormaPagamento(f)}
                      className={`px-2 py-2 text-xs rounded-lg border transition-colors
                        ${formaPagamento === f
                          ? 'border-mint/40 bg-mint/10 text-mint'
                          : 'border-white/10 text-[#8b9cb3] hover:border-white/20'
                        }`}
                    >
                      {formaPagamentoLabel(f)}
                    </button>
                  ))}
                </div>
              </div>

              {carrinhoInvalidoEstoque && !error && (
                <div className="px-3 py-2 rounded-lg bg-coral/10 border border-coral/25 text-xs text-coral flex items-start gap-2">
                  <AlertTriangle className="size-3.5 shrink-0 mt-0.5" />
                  Ajuste as quantidades: um ou mais itens excedem o estoque disponível.
                </div>
              )}

              {error && (
                <div className="px-3 py-2 rounded-lg bg-coral/10 border border-coral/25 text-xs text-coral">
                  {error}
                </div>
              )}

              <Button
                size="lg"
                className="w-full"
                loading={vendaMutation.isPending}
                disabled={
                  cart.length === 0
                  || !pdvQuery.data?.caixaAberto
                  || carrinhoInvalidoEstoque
                  || estoqueQuery.isLoading
                }
                onClick={handleFinalizar}
              >
                <CreditCard className="size-4" />
                Finalizar venda · {formatCurrency(total)}
              </Button>
            </div>
          </Card>
        </aside>
        </div>
      </div>

      {cart.length > 0 && (
        <div
          className="lg:hidden fixed bottom-0 inset-x-0 z-30 flex items-center gap-3 px-4 py-3
            border-t border-white/10 bg-bg-elevated/95 backdrop-blur-xl"
        >
          <div className="flex-1 min-w-0">
            <p className="text-xs text-[#8b9cb3]">{cart.length} {cart.length === 1 ? 'item' : 'itens'}</p>
            <p className="font-mono font-bold text-mint text-lg">{formatCurrency(total)}</p>
          </div>
          <Button
            variant="secondary"
            size="md"
            onClick={() => cartRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
          >
            Carrinho
          </Button>
          <Button
            size="md"
            loading={vendaMutation.isPending}
            disabled={
              !pdvQuery.data?.caixaAberto
              || carrinhoInvalidoEstoque
              || estoqueQuery.isLoading
            }
            onClick={handleFinalizar}
          >
            <CreditCard className="size-4" />
            Finalizar
          </Button>
        </div>
      )}

      {success && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <Card glow="mint" className="max-w-md w-full p-8 text-center">
            <CheckCircle2 className="size-14 text-mint mx-auto mb-4" />
            <h2 className="text-2xl font-bold">Venda finalizada</h2>
            <p className="font-mono text-lg text-mint mt-2">{success.venda.numeroCupom}</p>
            <p className="text-sm text-[#8b9cb3] mt-3">
              Pagamento:{' '}
              <span className="text-white font-medium">
                {formaPagamentoLabel(success.formaPagamento)}
              </span>
            </p>
            <p className="text-3xl font-bold font-mono mt-4">{formatCurrency(success.venda.total)}</p>
            {success.venda.avisos.length > 0 && (
              <div className="mt-4 text-left space-y-1">
                {success.venda.avisos.map((aviso) => (
                  <p key={aviso} className="text-xs text-amber flex items-start gap-1.5">
                    <AlertTriangle className="size-3.5 shrink-0 mt-0.5" />
                    {aviso}
                  </p>
                ))}
              </div>
            )}
            <Button className="mt-6 w-full" onClick={() => setSuccess(null)}>
              Nova venda
            </Button>
          </Card>
        </div>
      )}
    </div>
  )
}
