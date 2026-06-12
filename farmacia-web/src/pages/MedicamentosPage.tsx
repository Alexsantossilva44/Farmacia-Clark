import { useState, useDeferredValue } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Search, RefreshCw, Pill, ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { fetchMedicamentos } from '@/lib/api'
import { canGerenciarMedicamentos } from '@/lib/auth'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import {
  formatCurrency,
  formatEan,
  nivelControleLabel,
  nivelControleVariant,
} from '@/lib/format'
import { traduzirErroApi } from '@/lib/erros'

export function MedicamentosPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['medicamentos', page, deferredSearch],
    queryFn: () =>
      fetchMedicamentos(page, 15, deferredSearch.trim() || undefined),
    staleTime: 30_000,
  })

  const itens = data?.content ?? []
  const temLista = !isLoading && !isError && itens.length > 0

  return (
    <div className="page-shell">
      <header className="page-header-band">
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4 lg:gap-6">
          <div>
            <div className="flex items-center gap-2 text-coral mb-1.5">
              <Pill className="size-4" />
              <span className="text-xs font-semibold uppercase tracking-widest">Catálogo</span>
            </div>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight">Medicamentos</h1>
            <p className="text-[#8b9cb3] mt-1 text-sm">
              {data
                ? deferredSearch.trim()
                  ? `${data.totalElements} resultado(s) para "${deferredSearch.trim()}"`
                  : `${data.totalElements} itens cadastrados`
                : 'Carregando catálogo…'}
            </p>
            <p className="hidden sm:block text-xs text-[#8b9cb3] mt-1 max-w-xl">
              A coluna <strong className="text-white/80">Fabricante</strong> vem do vínculo em cada medicamento (
              <strong className="text-white/80">Cadastros → Medicamentos</strong>), não do texto do nome comercial.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row sm:flex-wrap items-stretch sm:items-center gap-3 w-full lg:w-auto shrink-0">
            {canGerenciarMedicamentos() && (
              <Link to="/cadastros?aba=medicamentos" className="w-full sm:w-auto">
                <Button variant="primary" size="md" className="w-full sm:w-auto">
                  <Plus className="size-4" />
                  Novo produto
                </Button>
              </Link>
            )}
            <div className="relative flex-1 sm:flex-initial min-w-0">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-[#8b9cb3]" />
              <input
                type="search"
                placeholder="Buscar nome ou EAN…"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value)
                  setPage(0)
                }}
                className="pl-10 pr-4 py-2.5 w-full sm:w-64 rounded-xl glass text-sm focus:outline-none focus:ring-2 focus:ring-mint/30"
              />
            </div>
            <Button variant="secondary" size="md" onClick={() => refetch()} disabled={isFetching}>
              <RefreshCw className={`size-4 ${isFetching ? 'animate-spin' : ''}`} />
            </Button>
          </div>
        </div>
      </header>

      <div className="page-scroll-body flex flex-col overflow-hidden !py-4 sm:!py-6">
        {isLoading && (
          <div className="flex-1 min-h-0 overflow-y-auto space-y-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-20 rounded-2xl glass animate-pulse bg-white/[0.02]" />
            ))}
          </div>
        )}

        {isError && (
          <div className="flex-1 flex items-center justify-center min-h-0">
            <Card glow="coral" className="text-center py-12 w-full max-w-lg">
              <p className="text-coral font-medium">{traduzirErroApi(error)}</p>
              <Button variant="secondary" className="mt-4" onClick={() => refetch()}>
                Tentar novamente
              </Button>
            </Card>
          </div>
        )}

        {!isLoading && !isError && itens.length === 0 && (
          <div className="flex-1 flex items-center justify-center min-h-0">
            <Card className="text-center py-16 w-full max-w-lg">
              <Pill className="size-12 text-[#8b9cb3] mx-auto mb-4 opacity-40" />
              <p className="font-medium">Nenhum medicamento encontrado</p>
              <p className="text-sm text-[#8b9cb3] mt-1">
                {search ? 'Ajuste a busca ou limpe o filtro.' : 'Cadastre produtos em Cadastros → Medicamentos.'}
              </p>
            </Card>
          </div>
        )}

        {temLista && (
          <>
            <div className="flex-1 min-h-0 overflow-y-auto table-scroll glass rounded-2xl border border-white/10">
              <table className="w-full text-sm min-w-[640px]">
                <thead className="sticky top-0 z-10 bg-bg-elevated/95 backdrop-blur-sm">
                  <tr className="border-b border-white/10 text-left text-xs uppercase tracking-wider text-[#8b9cb3]">
                    <th className="px-5 py-4 font-medium">Produto</th>
                    <th className="px-5 py-4 font-medium hidden md:table-cell">EAN-13</th>
                    <th className="px-5 py-4 font-medium hidden lg:table-cell">Fabricante</th>
                    <th className="px-5 py-4 font-medium">Controle</th>
                    <th className="px-5 py-4 font-medium text-right">PMC</th>
                  </tr>
                </thead>
                <tbody>
                  {itens.map((med, idx) => (
                    <tr
                      key={med.id}
                      className={`border-b border-white/5 transition-colors hover:bg-mint/5
                        ${idx % 2 === 0 ? 'bg-white/[0.01]' : ''}`}
                    >
                      <td className="px-5 py-4">
                        <p className="font-semibold">{med.nomeComercial}</p>
                        <p className="text-xs text-[#8b9cb3] mt-0.5">{med.nomeGenerico}</p>
                        <p className="text-xs text-white/40 mt-0.5">
                          {med.concentracao} · {med.apresentacao}
                        </p>
                      </td>
                      <td className="px-5 py-4 hidden md:table-cell">
                        <code className="font-mono text-xs text-mint/80">{formatEan(med.codigoEan)}</code>
                      </td>
                      <td className="px-5 py-4 hidden lg:table-cell text-[#8b9cb3]">
                        {med.fabricante?.nomeFantasia ?? med.fabricante?.razaoSocial ?? '—'}
                      </td>
                      <td className="px-5 py-4">
                        <Badge variant={nivelControleVariant(med.nivelControle)}>
                          {nivelControleLabel(med.nivelControle)}
                        </Badge>
                        {med.requerReceita && (
                          <span className="block text-[10px] text-amber mt-1 uppercase tracking-wide">
                            Receita obrigatória
                          </span>
                        )}
                      </td>
                      <td className="px-5 py-4 text-right font-mono font-medium">
                        {formatCurrency(med.precoMaximoConsumidor)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {data && data.totalPages > 1 && (
              <div className="shrink-0 flex items-center justify-between pt-4 mt-2 border-t border-white/10">
                <p className="text-sm text-[#8b9cb3]">
                  Página {data.number + 1} de {data.totalPages}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={data.first}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    <ChevronLeft className="size-4" />
                    Anterior
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Próxima
                    <ChevronRight className="size-4" />
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
