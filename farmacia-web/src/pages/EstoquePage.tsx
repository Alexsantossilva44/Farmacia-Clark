import { useRef, useState, useDeferredValue } from 'react'
import { flushSync } from 'react-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Package,
  AlertTriangle,
  Search,
  Layers,
  TrendingDown,
  Ban,
  RefreshCw,
  List,
  PackagePlus,
  Settings2,
  Scale,
  History,
  X,
} from 'lucide-react'
import {
  atualizarParametrosEstoque,
  fetchAllMedicamentos,
  fetchAlertasEstoque,
  fetchEstoqueAbaixoMinimo,
  fetchEstoqueItens,
  fetchEstoqueSaldo,
  fetchEstoqueZerados,
  fetchLotesFefo,
  fetchMovimentacoesEstoque,
} from '@/lib/api'
import { canGerenciarEstoque } from '@/lib/auth'
import {
  formatDateBR,
  formatDateTimeBR,
  statusAlertaLabel,
  tipoAlertaLabel,
  tipoAlertaVariant,
  tipoMovimentacaoLabel,
  tipoMovimentacaoVariant,
} from '@/lib/format'
import { traduzirErroApi } from '@/lib/erros'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { EstoqueEntradaPanel } from '@/pages/estoque/EstoqueEntradaPanel'
import { EstoqueAjustePanel } from '@/pages/estoque/EstoqueAjustePanel'
import type { ItemEstoque } from '@/types/estoque'

type Tab = 'listagem' | 'entrada' | 'ajuste' | 'historico' | 'fefo' | 'alertas' | 'minimo' | 'zerados'

export function EstoquePage() {
  const qc = useQueryClient()
  const podeGerenciar = canGerenciarEstoque()

  const [tab, setTab] = useState<Tab>('listagem')
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [selectedMedId, setSelectedMedId] = useState<string | null>(null)
  const [editItem, setEditItem] = useState<ItemEstoque | null>(null)
  const [editMin, setEditMin] = useState('')
  const [editMax, setEditMax] = useState('')
  const [editError, setEditError] = useState('')
  const [movPage, setMovPage] = useState(0)
  const [itensPage, setItensPage] = useState(0)
  const [filtroMedId, setFiltroMedId] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')
  const [listagemMsg, setListagemMsg] = useState<string | null>(null)
  const [entradaSession, setEntradaSession] = useState(0)
  const [ajusteSession, setAjusteSession] = useState(0)
  const scrollRef = useRef<HTMLDivElement>(null)

  const itensQuery = useQuery({
    queryKey: ['estoque-itens', itensPage, deferredSearch],
    queryFn: () =>
      fetchEstoqueItens({
        page: itensPage,
        size: 20,
        busca: deferredSearch.trim() || undefined,
      }),
    staleTime: 30_000,
  })

  const alertasQuery = useQuery({
    queryKey: ['estoque-alertas'],
    queryFn: () => fetchAlertasEstoque(),
    staleTime: 30_000,
    enabled: tab === 'alertas' || tab === 'listagem',
  })

  const minimoQuery = useQuery({
    queryKey: ['estoque-minimo'],
    queryFn: fetchEstoqueAbaixoMinimo,
    staleTime: 30_000,
    enabled: tab === 'minimo',
  })

  const zeradosQuery = useQuery({
    queryKey: ['estoque-zerados'],
    queryFn: fetchEstoqueZerados,
    staleTime: 30_000,
    enabled: tab === 'zerados',
  })

  const medsQuery = useQuery({
    queryKey: ['medicamentos-estoque'],
    queryFn: fetchAllMedicamentos,
    staleTime: 60_000,
    enabled: tab === 'fefo',
  })

  const saldoQuery = useQuery({
    queryKey: ['estoque-saldo', selectedMedId],
    queryFn: () => fetchEstoqueSaldo(selectedMedId!),
    enabled: !!selectedMedId && tab === 'fefo',
  })

  const lotesQuery = useQuery({
    queryKey: ['estoque-lotes', selectedMedId],
    queryFn: () => fetchLotesFefo(selectedMedId!),
    enabled: !!selectedMedId && tab === 'fefo',
  })

  const movimentacoesQuery = useQuery({
    queryKey: ['estoque-movimentacoes', movPage, filtroMedId, filtroTipo],
    queryFn: () =>
      fetchMovimentacoesEstoque({
        page: movPage,
        size: 20,
        medicamentoId: filtroMedId || undefined,
        tipo: filtroTipo || undefined,
      }),
    staleTime: 20_000,
    enabled: tab === 'historico',
  })

  const medsFiltroQuery = useQuery({
    queryKey: ['medicamentos-filtro-mov'],
    queryFn: fetchAllMedicamentos,
    staleTime: 60_000,
    enabled: tab === 'historico',
  })

  const ajusteMutation = useMutation({
    mutationFn: () =>
      atualizarParametrosEstoque(editItem!.medicamentoId, {
        quantidadeMinima: parseInt(editMin, 10),
        quantidadeMaxima: parseInt(editMax, 10),
      }),
    onSuccess: () => {
      setEditItem(null)
      setEditError('')
      qc.invalidateQueries({ queryKey: ['estoque-itens'] })
      qc.invalidateQueries({ queryKey: ['estoque-minimo'] })
    },
    onError: (err: unknown) => setEditError(traduzirErroApi(err)),
  })

  const itensPagina = itensQuery.data?.content ?? []

  const filteredMeds = medsQuery.data?.filter((m) => {
    if (!search.trim()) return true
    const q = search.toLowerCase()
    return m.nomeComercial.toLowerCase().includes(q) || m.nomeGenerico.toLowerCase().includes(q)
  })

  const tabs: { id: Tab; label: string; icon: typeof Package; count?: number }[] = [
    { id: 'listagem', label: 'Listagem', icon: List, count: itensQuery.data?.totalElements },
    ...(podeGerenciar ? [{ id: 'entrada' as Tab, label: 'Entrada', icon: PackagePlus }] : []),
    ...(podeGerenciar ? [{ id: 'ajuste' as Tab, label: 'Ajuste', icon: Scale }] : []),
    { id: 'historico', label: 'Histórico', icon: History, count: movimentacoesQuery.data?.totalElements },
    { id: 'fefo', label: 'FEFO', icon: Layers },
    { id: 'alertas', label: 'Alertas', icon: AlertTriangle, count: alertasQuery.data?.length },
    { id: 'minimo', label: 'Abaixo mínimo', icon: TrendingDown, count: minimoQuery.data?.length },
    { id: 'zerados', label: 'Zerados', icon: Ban, count: zeradosQuery.data?.length },
  ]

  function refetchAll() {
    itensQuery.refetch()
    alertasQuery.refetch()
    if (tab === 'minimo') minimoQuery.refetch()
    if (tab === 'zerados') zeradosQuery.refetch()
    if (tab === 'historico') movimentacoesQuery.refetch()
    if (tab === 'fefo') {
      medsQuery.refetch()
      if (selectedMedId) {
        saldoQuery.refetch()
        lotesQuery.refetch()
      }
    }
  }

  function abrirAjuste(item: ItemEstoque) {
    setEditItem(item)
    setEditMin(String(item.quantidadeMinima))
    setEditMax(String(item.quantidadeMaxima))
    setEditError('')
  }

  function irParaFefo(medicamentoId: string) {
    setSelectedMedId(medicamentoId)
    setTab('fefo')
  }

  function voltarParaListagem(mensagem?: string) {
    flushSync(() => {
      setSelectedMedId(null)
      setTab('listagem')
      setEntradaSession((n) => n + 1)
      setAjusteSession((n) => n + 1)
      if (mensagem) setListagemMsg(mensagem)
    })
    requestAnimationFrame(() => {
      scrollRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
    })
    void itensQuery.refetch()
    alertasQuery.refetch()
  }

  function irParaEntrada(medicamentoId?: string) {
    setSelectedMedId(medicamentoId ?? null)
    setEntradaSession((n) => n + 1)
    setTab('entrada')
  }

  function irParaAjuste(medicamentoId: string) {
    setSelectedMedId(medicamentoId)
    setAjusteSession((n) => n + 1)
    setTab('ajuste')
  }

  return (
    <div className="page-shell">
      <header className="page-header-band">
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-amber mb-1.5">
              <Package className="size-4" />
              <span className="text-xs font-semibold uppercase tracking-widest">Operacional</span>
            </div>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight">Estoque & FEFO</h1>
            <p className="text-[#8b9cb3] mt-1 text-sm">
              Listagem, entradas, lotes por validade e alertas
            </p>
          </div>
          <Button variant="secondary" size="md" onClick={refetchAll}>
            <RefreshCw className="size-4" />
            Atualizar
          </Button>
        </div>

        <nav className="page-tabs">
          {tabs.map(({ id, label, icon: Icon, count }) => (
            <button
              key={id}
              type="button"
              onClick={() => {
                if (id === 'entrada') {
                  setSelectedMedId(null)
                  setEntradaSession((n) => n + 1)
                }
                if (id === 'ajuste') {
                  setSelectedMedId(null)
                  setAjusteSession((n) => n + 1)
                }
                setTab(id)
              }}
              className={`inline-flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-all
                ${tab === id
                  ? 'bg-mint/15 text-mint border border-mint/25'
                  : 'text-[#8b9cb3] border border-white/10 hover:text-white hover:bg-white/5'
                }`}
            >
              <Icon className="size-4" />
              {label}
              {count !== undefined && (
                <span className="font-mono text-xs px-1.5 py-0.5 rounded bg-white/5">
                  {itensQuery.isLoading && id === 'listagem' ? '…' : (count ?? 0)}
                </span>
              )}
            </button>
          ))}
        </nav>
      </header>

      <div
        ref={scrollRef}
        className="page-scroll-body"
      >
        {/* Listagem geral */}
        {tab === 'listagem' && (
          <section className="space-y-4">
            {listagemMsg && (
              <div className="flex items-start justify-between gap-3 px-4 py-3 rounded-xl bg-mint/10 border border-mint/25 text-sm text-mint">
                <span>{listagemMsg}</span>
                <button
                  type="button"
                  onClick={() => setListagemMsg(null)}
                  className="text-mint/80 hover:text-white shrink-0 p-0.5"
                  aria-label="Fechar aviso"
                >
                  <X className="size-4" />
                </button>
              </div>
            )}
            <div className="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between">
              <div className="relative flex-1 max-w-md">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-[#8b9cb3]" />
                <input
                  type="search"
                  placeholder="Buscar medicamento…"
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value)
                    setItensPage(0)
                  }}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl glass bg-white/[0.03] text-sm focus:outline-none focus:ring-2 focus:ring-mint/30"
                />
              </div>
              {podeGerenciar && (
                <Button variant="primary" size="sm" onClick={() => irParaEntrada()}>
                  <PackagePlus className="size-4" />
                  Nova entrada
                </Button>
              )}
            </div>

            {itensQuery.isLoading && <LoadingSkeleton rows={5} />}
            {itensQuery.isError && (
              <ErrorCard message={traduzirErroApi(itensQuery.error)} onRetry={() => itensQuery.refetch()} />
            )}
            {!itensQuery.isLoading && itensPagina.length === 0 && (
              <EmptyState
                icon={Package}
                title={search.trim() ? 'Nenhum medicamento encontrado' : 'Nenhum medicamento cadastrado'}
                sub={
                  search.trim()
                    ? 'Tente outro termo de busca.'
                    : podeGerenciar
                      ? 'Cadastre medicamentos em Cadastros ou registre a primeira entrada.'
                      : 'Aguardando cadastro de medicamentos e entradas.'
                }
              />
            )}
            {itensPagina.length > 0 && (
              <div className="glass rounded-2xl border border-white/10 table-scroll">
                <table className="w-full text-sm min-w-[640px]">
                  <thead className="sticky top-0 bg-bg-elevated/95 backdrop-blur-sm">
                    <tr className="text-left text-xs uppercase tracking-wider text-[#8b9cb3] border-b border-white/10">
                      <th className="px-5 py-3">Medicamento</th>
                      <th className="px-5 py-3 text-right">Saldo</th>
                      <th className="px-5 py-3 text-right hidden sm:table-cell">Mín.</th>
                      <th className="px-5 py-3 text-right hidden sm:table-cell">Máx.</th>
                      <th className="px-5 py-3 hidden md:table-cell">Status</th>
                      <th className="px-5 py-3 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {itensPagina.map((item, idx) => (
                      <tr
                        key={item.id ?? item.medicamentoId}
                        className={`border-b border-white/5 hover:bg-mint/5
                          ${idx % 2 === 0 ? 'bg-white/[0.01]' : ''}`}
                      >
                        <td className="px-5 py-3 font-medium">{item.medicamentoNome}</td>
                        <td
                          className={`px-5 py-3 text-right font-mono font-semibold
                            ${item.semEntrada ? 'text-[#8b9cb3]' : 'text-mint'}`}
                        >
                          {item.quantidadeAtual}
                        </td>
                        <td className="px-5 py-3 text-right font-mono hidden sm:table-cell text-[#8b9cb3]">
                          {item.quantidadeMinima}
                        </td>
                        <td className="px-5 py-3 text-right font-mono hidden sm:table-cell text-[#8b9cb3]">
                          {item.quantidadeMaxima}
                        </td>
                        <td className="px-5 py-3 hidden md:table-cell">
                          {item.semEntrada && <Badge variant="sky">Sem entrada</Badge>}
                          {!item.semEntrada && item.zerado && <Badge variant="coral">Zerado</Badge>}
                          {!item.semEntrada && !item.zerado && item.abaixoDoMinimo && (
                            <Badge variant="amber">Abaixo mín.</Badge>
                          )}
                          {!item.semEntrada && !item.zerado && !item.abaixoDoMinimo && (
                            <Badge variant="mint">OK</Badge>
                          )}
                        </td>
                        <td className="px-5 py-3 text-right space-x-2 whitespace-nowrap">
                          {podeGerenciar && item.semEntrada && (
                            <button
                              type="button"
                              onClick={() => irParaEntrada(item.medicamentoId)}
                              className="text-xs text-mint hover:underline font-medium"
                            >
                              Entrada
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => irParaFefo(item.medicamentoId)}
                            className="text-xs text-mint hover:underline"
                          >
                            FEFO
                          </button>
                          {podeGerenciar && !item.semEntrada && (
                            <button
                              type="button"
                              onClick={() => irParaAjuste(item.medicamentoId)}
                              className="text-xs text-amber hover:underline"
                            >
                              Ajuste
                            </button>
                          )}
                          {podeGerenciar && !item.semEntrada && (
                            <button
                              type="button"
                              onClick={() => abrirAjuste(item)}
                              className="text-xs text-[#8b9cb3] hover:text-white hover:underline"
                            >
                              Mín/Máx
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {itensQuery.data && itensQuery.data.totalPages > 1 && (
              <div className="flex items-center justify-between text-sm">
                <span className="text-[#8b9cb3]">
                  Página {itensQuery.data.number + 1} de {itensQuery.data.totalPages}
                  {' · '}
                  {itensQuery.data.totalElements} medicamento(s)
                </span>
                <div className="flex gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={itensQuery.data.first}
                    onClick={() => setItensPage((p) => p - 1)}
                  >
                    Anterior
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={itensQuery.data.last}
                    onClick={() => setItensPage((p) => p + 1)}
                  >
                    Próxima
                  </Button>
                </div>
              </div>
            )}
          </section>
        )}

        {tab === 'entrada' && podeGerenciar && (
          <EstoqueEntradaPanel
            key={`entrada-${entradaSession}-${selectedMedId ?? 'nova'}`}
            medicamentoIdInicial={selectedMedId}
            onSucesso={({ mensagem }) => voltarParaListagem(mensagem)}
          />
        )}

        {tab === 'ajuste' && podeGerenciar && (
          <EstoqueAjustePanel
            key={`ajuste-${ajusteSession}-${selectedMedId ?? 'novo'}`}
            medicamentoIdInicial={selectedMedId}
            onSucesso={({ mensagem }) => voltarParaListagem(mensagem)}
          />
        )}

        {tab === 'historico' && (
          <section className="space-y-4">
            <div className="flex flex-col sm:flex-row gap-3">
              <Select
                label="Medicamento"
                value={filtroMedId}
                onChange={(v) => {
                  setFiltroMedId(v)
                  setMovPage(0)
                }}
                options={[
                  { value: '', label: 'Todos' },
                  ...(medsFiltroQuery.data?.map((m) => ({
                    value: m.id,
                    label: m.nomeComercial,
                  })) ?? []),
                ]}
                loading={medsFiltroQuery.isLoading}
              />
              <Select
                label="Tipo"
                value={filtroTipo}
                onChange={(v) => {
                  setFiltroTipo(v)
                  setMovPage(0)
                }}
                options={[
                  { value: '', label: 'Todos' },
                  { value: 'ENTRADA_COMPRA', label: 'Entrada (compra)' },
                  { value: 'SAIDA_VENDA', label: 'Saída (venda)' },
                  { value: 'AJUSTE_POSITIVO', label: 'Ajuste (+)' },
                  { value: 'AJUSTE_NEGATIVO', label: 'Ajuste (−)' },
                ]}
              />
            </div>

            {movimentacoesQuery.isLoading && <LoadingSkeleton rows={5} />}
            {movimentacoesQuery.isError && (
              <ErrorCard
                message={traduzirErroApi(movimentacoesQuery.error)}
                onRetry={() => movimentacoesQuery.refetch()}
              />
            )}
            {!movimentacoesQuery.isLoading && movimentacoesQuery.data?.empty && (
              <EmptyState
                icon={History}
                title="Nenhuma movimentação"
                sub="As entradas, vendas e ajustes aparecerão aqui."
              />
            )}
            {(movimentacoesQuery.data?.content.length ?? 0) > 0 && (
              <>
                <div className="glass rounded-2xl overflow-x-auto border border-white/10">
                  <table className="w-full text-sm min-w-[720px]">
                    <thead>
                      <tr className="text-left text-xs uppercase tracking-wider text-[#8b9cb3] border-b border-white/10">
                        <th className="px-5 py-3">Data/Hora</th>
                        <th className="px-5 py-3">Medicamento</th>
                        <th className="px-5 py-3">Lote</th>
                        <th className="px-5 py-3">Tipo</th>
                        <th className="px-5 py-3 text-right">Qtd</th>
                        <th className="px-5 py-3 text-right">Saldo</th>
                        <th className="px-5 py-3 hidden lg:table-cell">Motivo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {movimentacoesQuery.data!.content.map((mov, idx) => (
                        <tr
                          key={mov.id}
                          className={`border-b border-white/5 ${idx % 2 === 0 ? 'bg-white/[0.01]' : ''}`}
                        >
                          <td className="px-5 py-3 text-[#8b9cb3] whitespace-nowrap">
                            {formatDateTimeBR(mov.dataHora)}
                          </td>
                          <td className="px-5 py-3 font-medium">{mov.medicamentoNome}</td>
                          <td className="px-5 py-3 font-mono text-xs">{mov.numeroLote}</td>
                          <td className="px-5 py-3">
                            <Badge variant={tipoMovimentacaoVariant(mov.tipo)}>
                              {tipoMovimentacaoLabel(mov.tipo)}
                            </Badge>
                          </td>
                          <td className="px-5 py-3 text-right font-mono">{mov.quantidade}</td>
                          <td className="px-5 py-3 text-right font-mono text-[#8b9cb3]">
                            {mov.saldoAnterior} →{' '}
                            <span className="text-mint font-semibold">{mov.saldoPosterior}</span>
                          </td>
                          <td className="px-5 py-3 hidden lg:table-cell text-xs text-[#8b9cb3] max-w-[200px] truncate">
                            {mov.motivoAjuste ?? '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {movimentacoesQuery.data && movimentacoesQuery.data.totalPages > 1 && (
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-[#8b9cb3]">
                      Página {movimentacoesQuery.data.number + 1} de {movimentacoesQuery.data.totalPages}
                    </span>
                    <div className="flex gap-2">
                      <Button
                        variant="secondary"
                        size="sm"
                        disabled={movimentacoesQuery.data.first}
                        onClick={() => setMovPage((p) => p - 1)}
                      >
                        Anterior
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        disabled={movimentacoesQuery.data.last}
                        onClick={() => setMovPage((p) => p + 1)}
                      >
                        Próxima
                      </Button>
                    </div>
                  </div>
                )}
              </>
            )}
          </section>
        )}

        {tab === 'alertas' && (
          <section className="space-y-3">
            {alertasQuery.isLoading && <LoadingSkeleton rows={4} />}
            {alertasQuery.data?.length === 0 && (
              <EmptyState
                icon={AlertTriangle}
                title="Nenhum alerta aberto"
                sub="Alertas de vencimento e ruptura aparecem aqui automaticamente."
              />
            )}
            {alertasQuery.data?.map((a) => (
              <Card key={a.id} hover>
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <Badge variant={tipoAlertaVariant(a.tipo)}>{tipoAlertaLabel(a.tipo)}</Badge>
                    <p className="font-semibold mt-2">{a.medicamentoNome}</p>
                    {a.numeroLote && (
                      <p className="text-xs font-mono text-[#8b9cb3] mt-1">Lote {a.numeroLote}</p>
                    )}
                    <p className="text-sm text-[#8b9cb3] mt-2 leading-relaxed">{a.mensagem}</p>
                  </div>
                  <Badge variant="amber" dot>{statusAlertaLabel(a.status)}</Badge>
                </div>
              </Card>
            ))}
          </section>
        )}

        {(tab === 'minimo' || tab === 'zerados') && (
          <ItemEstoqueTable
            loading={tab === 'minimo' ? minimoQuery.isLoading : zeradosQuery.isLoading}
            error={tab === 'minimo' ? minimoQuery.error : zeradosQuery.error}
            items={tab === 'minimo' ? minimoQuery.data : zeradosQuery.data}
            onRetry={() => (tab === 'minimo' ? minimoQuery.refetch() : zeradosQuery.refetch())}
            onSelect={irParaFefo}
          />
        )}

        {tab === 'fefo' && (
          <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,280px)_1fr] gap-5 min-h-0">
            <div className="flex flex-col min-h-0">
              <div className="relative mb-3 shrink-0">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-[#8b9cb3]" />
                <input
                  type="search"
                  placeholder="Filtrar medicamento…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl glass bg-white/[0.03] text-sm focus:outline-none focus:ring-2 focus:ring-mint/30"
                />
              </div>
              <div className="flex-1 min-h-0 overflow-y-auto glass rounded-2xl border border-white/10 divide-y divide-white/5">
                {filteredMeds?.map((m) => (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => setSelectedMedId(m.id)}
                    className={`w-full text-left px-4 py-3 transition-colors
                      ${selectedMedId === m.id ? 'bg-mint/10 text-mint' : 'hover:bg-white/5'}`}
                  >
                    <p className="text-sm font-medium truncate">{m.nomeComercial}</p>
                    <p className="text-xs text-[#8b9cb3] truncate">{m.nomeGenerico}</p>
                  </button>
                ))}
              </div>
            </div>

            <div className="min-h-0">
              {!selectedMedId && (
                <EmptyState icon={Layers} title="Selecione um medicamento" sub="Visualize saldo e lotes FEFO." />
              )}
              {selectedMedId && saldoQuery.isLoading && <LoadingSkeleton rows={3} />}
              {selectedMedId && saldoQuery.isError && (
                <EmptyState
                  icon={Layers}
                  title="Sem registro de estoque"
                  sub="Este medicamento ainda não teve entrada. Use a aba Entrada."
                />
              )}
              {selectedMedId && saldoQuery.data && (
                <>
                  <Card glow="mint" className="mb-4">
                    <p className="text-xs text-[#8b9cb3] uppercase tracking-wider">Saldo consolidado</p>
                    <p className="text-4xl font-bold font-mono text-mint mt-1">
                      {saldoQuery.data.quantidadeAtual}
                      <span className="text-lg text-[#8b9cb3] font-sans font-normal ml-2">un.</span>
                    </p>
                    <p className="text-sm text-[#8b9cb3] mt-2">
                      Mín: {saldoQuery.data.quantidadeMinima} · Máx: {saldoQuery.data.quantidadeMaxima}
                    </p>
                    {saldoQuery.data.abaixoDoMinimo && (
                      <Badge variant="coral" className="mt-3">Abaixo do mínimo</Badge>
                    )}
                  </Card>

                  <div className="glass rounded-2xl overflow-hidden border border-white/10">
                    <div className="px-5 py-3 border-b border-white/10 flex items-center gap-2">
                      <Layers className="size-4 text-mint" />
                      <span className="font-semibold text-sm">Lotes FEFO</span>
                    </div>
                    {lotesQuery.isLoading && (
                      <div className="p-8 text-center text-sm text-[#8b9cb3]">Carregando lotes…</div>
                    )}
                    {!lotesQuery.isLoading && lotesQuery.data?.length === 0 && (
                      <div className="p-8 text-center text-sm text-[#8b9cb3]">Nenhum lote disponível</div>
                    )}
                    {(lotesQuery.data?.length ?? 0) > 0 && (
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="text-left text-xs uppercase tracking-wider text-[#8b9cb3] border-b border-white/10">
                            <th className="px-5 py-3">Lote</th>
                            <th className="px-5 py-3">Validade</th>
                            <th className="px-5 py-3">Qtd</th>
                            <th className="px-5 py-3">Dias</th>
                          </tr>
                        </thead>
                        <tbody>
                          {lotesQuery.data!.map((l, idx) => (
                            <tr
                              key={l.id}
                              className={`border-b border-white/5 hover:bg-mint/5
                                ${idx === 0 ? 'bg-mint/5' : ''}`}
                            >
                              <td className="px-5 py-3 font-mono text-mint">{l.numeroLote}</td>
                              <td className="px-5 py-3">{formatDateBR(l.dataValidade)}</td>
                              <td className="px-5 py-3 font-mono font-semibold">{l.quantidadeAtual}</td>
                              <td className="px-5 py-3">
                                <Badge variant={l.diasParaVencer <= 30 ? 'amber' : 'mint'}>
                                  {l.diasParaVencer}d
                                </Badge>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Modal ajuste mín/máx */}
      {editItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <Card className="w-full max-w-md p-5 sm:p-6 relative">
            <button
              type="button"
              onClick={() => setEditItem(null)}
              className="absolute top-4 right-4 p-1 rounded-lg text-[#8b9cb3] hover:text-white hover:bg-white/5"
            >
              <X className="size-5" />
            </button>
            <div className="flex items-center gap-2 mb-4">
              <Settings2 className="size-5 text-mint" />
              <h2 className="font-semibold">Parâmetros mín./máx.</h2>
            </div>
            <p className="text-sm text-[#8b9cb3] mb-4">{editItem.medicamentoNome}</p>
            {editError && <p className="text-sm text-coral mb-3">{editError}</p>}
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Estoque mínimo"
                type="number"
                min={0}
                value={editMin}
                onChange={(e) => setEditMin(e.target.value)}
              />
              <Input
                label="Estoque máximo"
                type="number"
                min={1}
                value={editMax}
                onChange={(e) => setEditMax(e.target.value)}
              />
            </div>
            <div className="flex gap-2 mt-5">
              <Button loading={ajusteMutation.isPending} onClick={() => ajusteMutation.mutate()}>
                Salvar
              </Button>
              <Button variant="ghost" onClick={() => setEditItem(null)}>
                Cancelar
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}

function ItemEstoqueTable({
  items,
  loading,
  error,
  onRetry,
  onSelect,
}: {
  items?: ItemEstoque[]
  loading: boolean
  error: Error | null
  onRetry: () => void
  onSelect: (id: string) => void
}) {
  if (loading) return <LoadingSkeleton rows={5} />
  if (error) return <ErrorCard message={traduzirErroApi(error)} onRetry={onRetry} />
  if (!items?.length) return <EmptyState icon={Package} title="Nenhum item nesta lista" sub="" />

  return (
    <div className="glass rounded-2xl overflow-hidden border border-white/10">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-xs uppercase tracking-wider text-[#8b9cb3] border-b border-white/10">
            <th className="px-5 py-3">Medicamento</th>
            <th className="px-5 py-3 text-right">Atual</th>
            <th className="px-5 py-3 text-right">Mínimo</th>
            <th className="px-5 py-3"></th>
          </tr>
        </thead>
        <tbody>
          {items.map((item, idx) => (
            <tr key={item.id} className={`border-b border-white/5 ${idx % 2 === 0 ? 'bg-white/[0.01]' : ''}`}>
              <td className="px-5 py-3 font-medium">{item.medicamentoNome}</td>
              <td className="px-5 py-3 text-right font-mono">{item.quantidadeAtual}</td>
              <td className="px-5 py-3 text-right font-mono text-[#8b9cb3]">{item.quantidadeMinima}</td>
              <td className="px-5 py-3 text-right">
                <button type="button" onClick={() => onSelect(item.medicamentoId)} className="text-xs text-mint hover:underline">
                  Ver FEFO
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function LoadingSkeleton({ rows }: { rows: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-20 rounded-2xl glass animate-pulse bg-white/[0.02]" />
      ))}
    </div>
  )
}

function ErrorCard({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <Card glow="coral" className="text-center py-10">
      <p className="text-coral">{message}</p>
      <Button variant="secondary" className="mt-4" onClick={onRetry}>Tentar novamente</Button>
    </Card>
  )
}

function EmptyState({
  icon: Icon,
  title,
  sub,
}: {
  icon: typeof Package
  title: string
  sub: string
}) {
  return (
    <Card className="text-center py-16">
      <Icon className="size-12 text-[#8b9cb3] mx-auto mb-4 opacity-40" />
      <p className="font-medium">{title}</p>
      {sub && <p className="text-sm text-[#8b9cb3] mt-1">{sub}</p>}
    </Card>
  )
}
