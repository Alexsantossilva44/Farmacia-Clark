import { useState, useEffect, useMemo, useRef } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FileInput, Plus, RefreshCw, Truck, Trash2, ClipboardList, CheckCircle2, AlertTriangle } from 'lucide-react'
import {
  cadastrarFornecedor,
  confirmarPedidoCompra,
  criarPedidoCompra,
  fetchFornecedores,
  fetchAllMedicamentos,
  fetchNotasFiscaisEntrada,
  fetchPedidosCompra,
  registrarNotaFiscalEntrada,
} from '@/lib/api'
import { canGerenciarCompras } from '@/lib/auth'
import { dataIsoHojeLocal, formatCurrency, formatDateBR, roundMoney } from '@/lib/format'
import { traduzirErroApi } from '@/lib/erros'
import { useErro } from '@/hooks/useErro'
import { useErrosCampo, MSG_OBRIGATORIO } from '@/hooks/useErrosCampo'
import {
  focarPrimeiroErro,
  validarSelecao,
} from '@/lib/validacao-formulario'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { maskCnpjInput, medicamentosToSelectOptions } from '@/lib/cadastro-options'
import type { ConferenciaNota, ItemNotaFiscalInput, ItemPedidoCompraInput } from '@/types/compra'
import type { Medicamento } from '@/types/api'

type Tab = 'pedidos' | 'notas' | 'nova-pedido' | 'nova'

const itemVazio = (): ItemNotaFiscalInput => ({
  medicamentoId: '',
  numeroLote: '',
  dataValidade: '',
  quantidade: 1,
})

const itemPedidoVazio = (): ItemPedidoCompraInput => ({
  medicamentoId: '',
  quantidadeSolicitada: 1,
})

function precoInputValue(preco?: number) {
  return preco == null ? '' : preco
}

function parsePrecoInput(raw: string): number | undefined {
  const trimmed = raw.trim()
  if (trimmed === '') return undefined
  const n = parseFloat(trimmed)
  return Number.isFinite(n) ? n : undefined
}

function medicamentosPorId(medicamentos?: Medicamento[]): Record<string, Medicamento> {
  if (!medicamentos) return {}
  return Object.fromEntries(medicamentos.map((m) => [m.id, m]))
}

function medOptsComPreco(medicamentos?: Medicamento[]) {
  if (!medicamentos) return []
  return medicamentosToSelectOptions(medicamentos, (m) =>
    `${m.nomeGenerico} · PMC ${formatCurrency(m.precoMaximoConsumidor)}`,
  )
}

function ReferenciaPrecoMedicamento({
  medicamento,
  precoInformado,
}: {
  medicamento?: Medicamento
  precoInformado?: number
}) {
  if (!medicamento) return null
  const acimaDoPmc =
    precoInformado != null && precoInformado > 0 && precoInformado > medicamento.precoMaximoConsumidor
  return (
    <p className={`text-xs mt-1 ${acimaDoPmc ? 'text-amber' : 'text-[#8b9cb3]'}`}>
      PMC (venda):{' '}
      <span className="font-mono text-mint">{formatCurrency(medicamento.precoMaximoConsumidor)}</span>
      {acimaDoPmc && ' — preço de compra acima do PMC cadastrado'}
    </p>
  )
}

export function ComprasPage() {
  const qc = useQueryClient()
  const podeGerenciar = canGerenciarCompras()
  const [tab, setTab] = useState<Tab>('pedidos')
  const [pedidoParaNf, setPedidoParaNf] = useState<string | null>(null)

  const notasQuery = useQuery({
    queryKey: ['compras-notas'],
    queryFn: fetchNotasFiscaisEntrada,
    staleTime: 30_000,
  })

  const pedidosQuery = useQuery({
    queryKey: ['compras-pedidos'],
    queryFn: fetchPedidosCompra,
    staleTime: 30_000,
  })

  function refetchAll() {
    notasQuery.refetch()
    pedidosQuery.refetch()
  }

  const confirmarMutation = useMutation({
    mutationFn: confirmarPedidoCompra,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['compras-pedidos'] })
    },
  })

  if (!podeGerenciar) {
    return (
      <div className="p-6 sm:p-10 text-center text-[#8b9cb3]">
        Acesso restrito a estoquistas, gerentes e administradores.
      </div>
    )
  }

  return (
    <div className="page-shell">
      <header className="page-header-band">
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-sky mb-1.5">
              <Truck className="size-4" />
              <span className="text-xs font-semibold uppercase tracking-widest">Suprimentos</span>
            </div>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight">Compras & NF-e</h1>
            <p className="text-[#8b9cb3] mt-1 text-sm">
              Pedidos de compra, NF-e de entrada e conferência automática
            </p>
          </div>
          <Button variant="secondary" size="md" onClick={refetchAll}>
            <RefreshCw className="size-4" />
            Atualizar
          </Button>
        </div>

        <nav className="page-tabs">
          {[
            { id: 'pedidos' as Tab, label: 'Pedidos', icon: ClipboardList, count: pedidosQuery.data?.length },
            { id: 'notas' as Tab, label: 'Notas recebidas', icon: FileInput, count: notasQuery.data?.length },
            { id: 'nova-pedido' as Tab, label: 'Novo pedido', icon: Plus },
            { id: 'nova' as Tab, label: 'Nova NF-e', icon: FileInput },
          ].map(({ id, label, icon: Icon, count }) => (
            <button
              key={id}
              type="button"
              onClick={() => setTab(id)}
              className={`inline-flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-all
                ${tab === id
                  ? 'bg-sky/15 text-sky border border-sky/25'
                  : 'text-[#8b9cb3] border border-white/10 hover:text-white hover:bg-white/5'
                }`}
            >
              <Icon className="size-4" />
              {label}
              {count !== undefined && (
                <span className="font-mono text-xs px-1.5 py-0.5 rounded bg-white/5">{count ?? 0}</span>
              )}
            </button>
          ))}
        </nav>
      </header>

      <div className="page-scroll-body">
        {tab === 'pedidos' && (
          <PedidosPanel
            pedidos={pedidosQuery.data}
            loading={pedidosQuery.isLoading}
            error={pedidosQuery.error}
            onConfirmar={(id) => confirmarMutation.mutate(id)}
            confirmando={confirmarMutation.isPending}
            onReceber={(id) => {
              setPedidoParaNf(id)
              setTab('nova')
            }}
          />
        )}

        {tab === 'notas' && (
          <section className="space-y-4">
            {notasQuery.isLoading && <p className="text-sm text-[#8b9cb3]">Carregando…</p>}
            {notasQuery.isError && (
              <p className="text-sm text-coral">{traduzirErroApi(notasQuery.error)}</p>
            )}
            {!notasQuery.isLoading && notasQuery.data?.length === 0 && (
              <Card className="p-8 text-center text-[#8b9cb3]">
                Nenhuma NF-e registrada. Use a aba <strong className="text-white">Nova NF-e</strong>.
              </Card>
            )}
            {(notasQuery.data?.length ?? 0) > 0 && (
              <div className="glass rounded-2xl border border-white/10 table-scroll">
                <table className="w-full text-sm min-w-[600px]">
                  <thead>
                    <tr className="text-left text-xs uppercase tracking-wider text-[#8b9cb3] border-b border-white/10">
                      <th className="px-5 py-3">Nota</th>
                      <th className="px-5 py-3">Fornecedor</th>
                      <th className="px-5 py-3 hidden md:table-cell">Emissão</th>
                      <th className="px-5 py-3 hidden lg:table-cell">Chave</th>
                      <th className="px-5 py-3 text-right">Valor</th>
                      <th className="px-5 py-3 text-right">Itens</th>
                      <th className="px-5 py-3">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {notasQuery.data!.map((n, idx) => (
                      <tr
                        key={n.id}
                        className={`border-b border-white/5 ${idx % 2 === 0 ? 'bg-white/[0.01]' : ''}`}
                      >
                        <td className="px-5 py-3 font-mono font-medium">
                          {n.numeroNota}
                          {n.serie ? ` / ${n.serie}` : ''}
                        </td>
                        <td className="px-5 py-3">{n.fornecedorNome}</td>
                        <td className="px-5 py-3 hidden md:table-cell">{formatDateBR(n.dataEmissao)}</td>
                        <td className="px-5 py-3 hidden lg:table-cell font-mono text-xs text-[#8b9cb3] truncate max-w-[180px]">
                          {n.chaveAcesso}
                        </td>
                        <td className="px-5 py-3 text-right font-mono">{formatCurrency(n.valorTotal)}</td>
                        <td className="px-5 py-3 text-right font-mono">{n.quantidadeItens}</td>
                        <td className="px-5 py-3">
                          <Badge variant={n.status === 'DIVERGENCIA' ? 'amber' : n.status === 'CONFERIDA' ? 'mint' : 'sky'}>
                            {n.status}
                          </Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {tab === 'nova-pedido' && (
          <NovoPedidoPanel
            onSucesso={() => {
              qc.invalidateQueries({ queryKey: ['compras-pedidos'] })
              setTab('pedidos')
            }}
          />
        )}

        {tab === 'nova' && (
          <NovaNotaPanel
            pedidoIdInicial={pedidoParaNf}
            onSucesso={() => {
              setPedidoParaNf(null)
              qc.invalidateQueries({ queryKey: ['compras-notas'] })
              qc.invalidateQueries({ queryKey: ['compras-pedidos'] })
              qc.invalidateQueries({ queryKey: ['estoque-itens'] })
              qc.invalidateQueries({ queryKey: ['estoque-disponivel-venda-pdv'] })
              qc.invalidateQueries({ queryKey: ['estoque-movimentacoes'] })
              setTab('notas')
            }}
          />
        )}
      </div>
    </div>
  )
}

function PedidosPanel({
  pedidos,
  loading,
  error,
  onConfirmar,
  confirmando,
  onReceber,
}: {
  pedidos?: import('@/types/compra').PedidoCompra[]
  loading: boolean
  error: Error | null
  onConfirmar: (id: string) => void
  confirmando: boolean
  onReceber: (id: string) => void
}) {
  if (loading) return <p className="text-sm text-[#8b9cb3]">Carregando…</p>
  if (error) return <p className="text-sm text-coral">{traduzirErroApi(error)}</p>
  if (!pedidos?.length) {
    return (
      <Card className="p-8 text-center text-[#8b9cb3]">
        Nenhum pedido cadastrado. Use <strong className="text-white">Novo pedido</strong>.
      </Card>
    )
  }

  return (
    <div className="glass rounded-2xl border border-white/10 table-scroll">
      <table className="w-full text-sm min-w-[640px]">
        <thead>
          <tr className="text-left text-xs uppercase tracking-wider text-[#8b9cb3] border-b border-white/10">
            <th className="px-5 py-3">Fornecedor</th>
            <th className="px-5 py-3 hidden sm:table-cell">Data</th>
            <th className="px-5 py-3 text-right">Valor</th>
            <th className="px-5 py-3 text-right hidden md:table-cell">Pendente</th>
            <th className="px-5 py-3">Status</th>
            <th className="px-5 py-3 text-right">Ações</th>
          </tr>
        </thead>
        <tbody>
          {pedidos.map((p, idx) => (
            <tr key={p.id} className={`border-b border-white/5 ${idx % 2 === 0 ? 'bg-white/[0.01]' : ''}`}>
              <td className="px-5 py-3 font-medium">{p.fornecedorNome}</td>
              <td className="px-5 py-3 hidden sm:table-cell">{formatDateBR(p.dataPedido)}</td>
              <td className="px-5 py-3 text-right font-mono">{formatCurrency(p.valorTotal)}</td>
              <td className="px-5 py-3 text-right font-mono hidden md:table-cell">{p.quantidadePendente ?? '—'}</td>
              <td className="px-5 py-3">
                <Badge variant={p.status === 'RECEBIDO' ? 'mint' : p.status === 'RASCUNHO' ? 'sky' : 'amber'}>
                  {p.status}
                </Badge>
              </td>
              <td className="px-5 py-3 text-right space-x-2 whitespace-nowrap">
                {p.status === 'RASCUNHO' && (
                  <button type="button" disabled={confirmando} onClick={() => onConfirmar(p.id)} className="text-xs text-mint hover:underline">
                    Confirmar
                  </button>
                )}
                {['CONFIRMADO', 'PARCIALMENTE_RECEBIDO'].includes(p.status) && (
                  <button type="button" onClick={() => onReceber(p.id)} className="text-xs text-sky hover:underline">
                    Receber NF
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** Painel "Novo pedido" — validação de fornecedor e itens no submit (padrão CLARK). */
function NovoPedidoPanel({ onSucesso }: { onSucesso: () => void }) {
  const qc = useQueryClient()
  const formRef = useRef<HTMLDivElement>(null)
  const [fornecedorId, setFornecedorId] = useState('')
  const [itens, setItens] = useState<ItemPedidoCompraInput[]>([itemPedidoVazio()])
  const { error, showError, clearError } = useErro()
  const [showFornecedor, setShowFornecedor] = useState(false)
  const [novoFornRazao, setNovoFornRazao] = useState('')
  const [novoFornCnpj, setNovoFornCnpj] = useState('')
  const { fieldErrors, setErroTemporario, limparErros } = useErrosCampo()

  const fornQuery = useQuery({ queryKey: ['fornecedores'], queryFn: fetchFornecedores })
  const medsQuery = useQuery({
    queryKey: ['medicamentos-compras'],
    queryFn: fetchAllMedicamentos,
  })

  const mutation = useMutation({
    mutationFn: () =>
      criarPedidoCompra({
        fornecedorId,
        itens: itens.map((i) => ({
          medicamentoId: i.medicamentoId,
          quantidadeSolicitada: i.quantidadeSolicitada,
          ...(i.precoUnitario != null ? { precoUnitario: roundMoney(i.precoUnitario) } : {}),
        })),
      }),
    onSuccess: onSucesso,
    onError: (err: unknown) => showError(traduzirErroApi(err)),
  })

  const fornOpts =
    fornQuery.data?.map((f) => ({
      value: f.id,
      label: f.nomeFantasia ?? f.razaoSocial,
      sublabel: f.cnpj,
    })) ?? []
  const medOpts = medOptsComPreco(medsQuery.data)
  const medsPorId = medicamentosPorId(medsQuery.data)

  const fornMutation = useMutation({
    mutationFn: () =>
      cadastrarFornecedor({
        razaoSocial: novoFornRazao.trim(),
        cnpj: novoFornCnpj.replace(/\D/g, ''),
      }),
    onSuccess: (f) => {
      setFornecedorId(f.id)
      setShowFornecedor(false)
      setNovoFornRazao('')
      setNovoFornCnpj('')
      qc.invalidateQueries({ queryKey: ['fornecedores'] })
    },
    onError: (err: unknown) => showError(traduzirErroApi(err)),
  })

  /** Valida cabeçalho e cada linha do pedido; erros por chave plana em fieldErrors. */
  function validarPedido(): boolean {
    const fornErr = validarSelecao(fornecedorId)
    setErroTemporario('fornecedorId', fornErr ?? undefined)
    let todosItensValidos = true
    itens.forEach((item, idx) => {
      const medErr = validarSelecao(item.medicamentoId) ?? undefined
      const qtdErr = item.quantidadeSolicitada <= 0 ? MSG_OBRIGATORIO : undefined
      setErroTemporario(`itens.${idx}.medicamentoId`, medErr)
      setErroTemporario(`itens.${idx}.quantidade`, qtdErr)
      if (medErr || qtdErr) todosItensValidos = false
    })
    const valido = !fornErr && todosItensValidos
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  function tentarSalvarPedido() {
    if (!validarPedido()) return
    mutation.mutate()
  }

  return (
    <Card className="p-5 sm:p-6 max-w-2xl space-y-4">
      <div ref={formRef}>
      <h2 className="font-semibold">Novo pedido de compra</h2>
      {error && <p className="text-sm text-coral">{error}</p>}
      <div className="flex gap-2 items-end">
        <div className="flex-1">
          <Select
            label="Fornecedor *"
            value={fornecedorId}
            onChange={setFornecedorId}
            options={fornOpts}
            loading={fornQuery.isLoading}
            placeholder="Selecione…"
            error={fieldErrors['fornecedorId']}
          />
          <p className="text-xs text-[#8b9cb3] mt-1">
            Cadastre em <strong className="text-white/80">Cadastros → Fornecedores</strong> (não confundir com
            Fabricantes).
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => setShowFornecedor((v) => !v)}>
          <Plus className="size-4" />
          Novo
        </Button>
      </div>
      {showFornecedor && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3 rounded-xl border border-white/10 bg-white/[0.02]">
          <Input label="Razão social" value={novoFornRazao} onChange={(e) => setNovoFornRazao(e.target.value)} />
          <Input
            label="CNPJ *"
            value={novoFornCnpj}
            onChange={(e) => setNovoFornCnpj(maskCnpjInput(e.target.value))}
            className="font-mono"
            placeholder="00.000.000/0000-00"
            inputMode="numeric"
            maxLength={18}
          />
          <Button size="sm" loading={fornMutation.isPending} onClick={() => fornMutation.mutate()}>
            Salvar fornecedor
          </Button>
        </div>
      )}
      {itens.map((item, idx) => (
        <div key={idx} className="p-3 rounded-xl border border-white/10 space-y-2">
          <Select
            label="Medicamento *"
            value={item.medicamentoId}
            onChange={(v) =>
              setItens((p) => p.map((it, i) => (i === idx ? { ...it, medicamentoId: v } : it)))
            }
            options={medOpts}
            loading={medsQuery.isLoading}
            error={fieldErrors[`itens.${idx}.medicamentoId`]}
          />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <Input
              label="Qtd solicitada *"
              type="number"
              min={1}
              value={item.quantidadeSolicitada}
              onChange={(e) =>
                setItens((p) =>
                  p.map((it, i) =>
                    i === idx ? { ...it, quantidadeSolicitada: parseInt(e.target.value, 10) || 0 } : it,
                  ),
                )
              }
              error={fieldErrors[`itens.${idx}.quantidade`]}
            />
            <div>
              <Input
                label="Preço un. compra (R$)"
                type="number"
                min={0}
                step={0.01}
                placeholder="Opcional"
                value={precoInputValue(item.precoUnitario)}
                onChange={(e) =>
                  setItens((p) =>
                    p.map((it, i) =>
                      i === idx ? { ...it, precoUnitario: parsePrecoInput(e.target.value) } : it,
                    ),
                  )
                }
              />
              <ReferenciaPrecoMedicamento
                medicamento={medsPorId[item.medicamentoId]}
                precoInformado={item.precoUnitario}
              />
            </div>
          </div>
        </div>
      ))}
      <Button variant="ghost" size="sm" onClick={() => setItens((p) => [...p, itemPedidoVazio()])}>
        <Plus className="size-4" /> Item
      </Button>
      <Button
        loading={mutation.isPending}
        disabled={mutation.isPending}
        onClick={tentarSalvarPedido}
      >
        Salvar rascunho
      </Button>
      </div>
    </Card>
  )
}

function NovaNotaPanel({
  pedidoIdInicial,
  onSucesso,
}: {
  pedidoIdInicial?: string | null
  onSucesso: () => void
}) {
  const qc = useQueryClient()
  const [fornecedorId, setFornecedorId] = useState('')
  const [pedidoCompraId, setPedidoCompraId] = useState(pedidoIdInicial ?? '')
  const [conferencia, setConferencia] = useState<ConferenciaNota | null>(null)
  const [numeroNota, setNumeroNota] = useState('')
  const [serie, setSerie] = useState('')
  const [chaveAcesso, setChaveAcesso] = useState('')
  const [dataEmissao, setDataEmissao] = useState('')
  const [itens, setItens] = useState<ItemNotaFiscalInput[]>([itemVazio()])
  const { error, showError, clearError } = useErro()
  const [showFornecedor, setShowFornecedor] = useState(false)
  const [novoFornRazao, setNovoFornRazao] = useState('')
  const [novoFornCnpj, setNovoFornCnpj] = useState('')

  const fornQuery = useQuery({ queryKey: ['fornecedores'], queryFn: fetchFornecedores })
  const pedidosQuery = useQuery({
    queryKey: ['pedidos-para-nf'],
    queryFn: fetchPedidosCompra,
  })
  const medsQuery = useQuery({
    queryKey: ['medicamentos-compras'],
    queryFn: fetchAllMedicamentos,
  })

  useEffect(() => {
    if (!pedidoIdInicial) return
    const ped = pedidosQuery.data?.find((p) => p.id === pedidoIdInicial)
    if (ped) {
      setFornecedorId(ped.fornecedorId)
      setPedidoCompraId(ped.id)
    }
  }, [pedidoIdInicial, pedidosQuery.data])

  const fornOpts =
    fornQuery.data?.map((f) => ({
      value: f.id,
      label: f.nomeFantasia ?? f.razaoSocial,
      sublabel: f.cnpj,
    })) ?? []

  const medOpts = medOptsComPreco(medsQuery.data)
  const medsPorId = medicamentosPorId(medsQuery.data)
  const hoje = useMemo(() => dataIsoHojeLocal(), [])

  const pedidoOpts =
    pedidosQuery.data
      ?.filter(
        (p) =>
          p.fornecedorId === fornecedorId &&
          ['CONFIRMADO', 'PARCIALMENTE_RECEBIDO'].includes(p.status),
      )
      .map((p) => ({
        value: p.id,
        label: `${formatDateBR(p.dataPedido)} — pend. ${p.quantidadePendente ?? 0} un.`,
        sublabel: p.status,
      })) ?? []

  const notaMutation = useMutation({
    mutationFn: () =>
      registrarNotaFiscalEntrada({
        fornecedorId,
        pedidoCompraId: pedidoCompraId || undefined,
        numeroNota: numeroNota.trim(),
        serie: serie.trim() || undefined,
        chaveAcesso: chaveAcesso.replace(/\D/g, ''),
        dataEmissao,
        itens: itens.map((i) => ({
          ...i,
          precoUnitario: roundMoney(i.precoUnitario!),
        })),
      }),
    onSuccess: (data) => {
      clearError()
      if (data.conferencia) {
        setConferencia(data.conferencia)
      } else {
        onSucesso()
      }
    },
    onError: (err: unknown) => showError(traduzirErroApi(err)),
  })

  const fornMutation = useMutation({
    mutationFn: () =>
      cadastrarFornecedor({
        razaoSocial: novoFornRazao.trim(),
        cnpj: novoFornCnpj.replace(/\D/g, ''),
      }),
    onSuccess: (f) => {
      setFornecedorId(f.id)
      setShowFornecedor(false)
      setNovoFornRazao('')
      setNovoFornCnpj('')
      qc.invalidateQueries({ queryKey: ['fornecedores'] })
    },
    onError: (err: unknown) => showError(traduzirErroApi(err)),
  })

  const valido =
    fornecedorId &&
    numeroNota.trim() &&
    chaveAcesso.replace(/\D/g, '').length === 44 &&
    dataEmissao &&
    itens.every(
      (i) =>
        i.medicamentoId &&
        i.numeroLote.trim() &&
        i.dataValidade &&
        i.dataValidade >= hoje &&
        i.quantidade > 0 &&
        i.precoUnitario != null &&
        i.precoUnitario >= 0,
    )

  function atualizarItem(idx: number, patch: Partial<ItemNotaFiscalInput>) {
    setItens((prev) => prev.map((it, i) => (i === idx ? { ...it, ...patch } : it)))
  }

  function atualizarValidadeItem(idx: number, value: string) {
    if (!value) {
      atualizarItem(idx, { dataValidade: '' })
      return
    }
    atualizarItem(idx, { dataValidade: value < hoje ? hoje : value })
  }

  return (
    <Card className="p-5 sm:p-6 max-w-3xl space-y-4">
      <h2 className="font-semibold flex items-center gap-2">
        <FileInput className="size-5 text-sky" />
        Registrar NF-e de entrada
      </h2>
      {error && <p className="text-sm text-coral">{error}</p>}

      {conferencia && (
        <Card className={`p-4 border ${conferencia.conferida ? 'border-mint/30 bg-mint/5' : 'border-amber/30 bg-amber/5'}`}>
          <div className="flex items-center gap-2 mb-2">
            {conferencia.conferida ? (
              <CheckCircle2 className="size-5 text-mint" />
            ) : (
              <AlertTriangle className="size-5 text-amber" />
            )}
            <h3 className="font-semibold">
              {conferencia.conferida ? 'Conferência OK' : 'Divergências na conferência'}
            </h3>
          </div>
          <p className="text-sm text-[#8b9cb3] mb-2">
            NF: {conferencia.statusNota} · Pedido: {conferencia.statusPedido}
          </p>
          {conferencia.divergencias.length > 0 && (
            <ul className="text-sm space-y-1 mb-3">
              {conferencia.divergencias.map((d, i) => (
                <li key={i} className="text-amber">
                  <strong>{d.medicamentoNome}</strong> — {d.mensagem}
                </li>
              ))}
            </ul>
          )}
          <Button size="sm" onClick={() => { setConferencia(null); onSucesso() }}>
            Continuar
          </Button>
        </Card>
      )}

      <div className="flex gap-2 items-end">
        <div className="flex-1">
          <Select
            label="Fornecedor *"
            value={fornecedorId}
            onChange={setFornecedorId}
            options={fornOpts}
            loading={fornQuery.isLoading}
            placeholder="Selecione…"
          />
        </div>
        <Button variant="secondary" size="sm" onClick={() => setShowFornecedor((v) => !v)}>
          <Plus className="size-4" />
          Novo
        </Button>
      </div>

      {showFornecedor && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3 rounded-xl border border-white/10 bg-white/[0.02]">
          <Input label="Razão social" value={novoFornRazao} onChange={(e) => setNovoFornRazao(e.target.value)} />
          <Input
            label="CNPJ *"
            value={novoFornCnpj}
            onChange={(e) => setNovoFornCnpj(maskCnpjInput(e.target.value))}
            className="font-mono"
            placeholder="00.000.000/0000-00"
            inputMode="numeric"
            maxLength={18}
          />
          <Button size="sm" loading={fornMutation.isPending} onClick={() => fornMutation.mutate()}>
            Salvar fornecedor
          </Button>
        </div>
      )}

      {fornecedorId && (
        <Select
          label="Pedido de compra (conferência)"
          value={pedidoCompraId}
          onChange={setPedidoCompraId}
          options={[{ value: '', label: 'Sem pedido vinculado' }, ...pedidoOpts]}
          loading={pedidosQuery.isLoading}
          placeholder="Opcional — vincula NF ao pedido"
        />
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Input label="Número da nota *" value={numeroNota} onChange={(e) => setNumeroNota(e.target.value)} />
        <Input label="Série" value={serie} onChange={(e) => setSerie(e.target.value)} />
      </div>
      <Input
        label="Chave de acesso NF-e * (44 dígitos)"
        value={chaveAcesso}
        onChange={(e) => setChaveAcesso(e.target.value)}
        className="font-mono text-sm"
      />
      <Input label="Data emissão *" type="date" value={dataEmissao} onChange={(e) => setDataEmissao(e.target.value)} />

      <div className="space-y-3 pt-2 border-t border-white/10">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-medium">Itens da nota</h3>
          <Button variant="ghost" size="sm" onClick={() => setItens((p) => [...p, itemVazio()])}>
            <Plus className="size-4" />
            Item
          </Button>
        </div>
        {itens.map((item, idx) => (
          <div key={idx} className="p-3 rounded-xl border border-white/10 space-y-2 bg-white/[0.02]">
            <div className="flex justify-between items-center">
              <span className="text-xs text-[#8b9cb3]">Item {idx + 1}</span>
              {itens.length > 1 && (
                <button
                  type="button"
                  onClick={() => setItens((p) => p.filter((_, i) => i !== idx))}
                  className="text-coral hover:text-coral/80"
                >
                  <Trash2 className="size-4" />
                </button>
              )}
            </div>
            <Select
              label="Medicamento"
              value={item.medicamentoId}
              onChange={(v) => atualizarItem(idx, { medicamentoId: v })}
              options={medOpts}
              loading={medsQuery.isLoading}
            />
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              <Input
                label="Lote"
                value={item.numeroLote}
                onChange={(e) => atualizarItem(idx, { numeroLote: e.target.value })}
                className="font-mono text-sm"
              />
              <Input
                label="Validade"
                type="date"
                min={hoje}
                value={item.dataValidade}
                onChange={(e) => atualizarValidadeItem(idx, e.target.value)}
              />
              <Input
                label="Qtd"
                type="number"
                min={1}
                value={item.quantidade}
                onChange={(e) => atualizarItem(idx, { quantidade: parseInt(e.target.value, 10) || 0 })}
              />
              <div>
                <Input
                  label="Preço un. (R$)"
                  type="number"
                  min={0}
                  step={0.01}
                  value={precoInputValue(item.precoUnitario)}
                  onChange={(e) =>
                    atualizarItem(idx, { precoUnitario: parsePrecoInput(e.target.value) })
                  }
                />
                <ReferenciaPrecoMedicamento
                  medicamento={medsPorId[item.medicamentoId]}
                  precoInformado={item.precoUnitario}
                />
              </div>
            </div>
          </div>
        ))}
      </div>

      <Button loading={notaMutation.isPending} disabled={!valido} onClick={() => notaMutation.mutate()}>
        Registrar NF-e e dar entrada no estoque
      </Button>
    </Card>
  )
}
