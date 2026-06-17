import { useRef, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  FileText,
  Search,
  Plus,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  ClipboardList,
  AlertTriangle,
} from 'lucide-react'
import {
  cadastrarReceita,
  fetchAuthContexto,
  fetchAllMedicamentos,
  fetchPrescritores,
  fetchReceitaPorNumero,
  validarReceita,
} from '@/lib/api'
import { canValidarReceita, getFuncionarioId, getUserEmail } from '@/lib/auth'
import { formatDateBR, nivelControleLabel, statusReceitaLabel, statusReceitaVariant, tipoReceitaLabel } from '@/lib/format'
import { traduzirErroApi } from '@/lib/erros'
import { focarPrimeiroErro, validarObrigatorio, validarSelecao } from '@/lib/validacao-formulario'
import { useErrosCampo, calcularProgressoCampos } from '@/hooks/useErrosCampo'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import type { Receita, TipoReceita, ValidarReceitaResult } from '@/types/receita'
import { PRESCRITOR_DEV_ID } from '@/types/receita'

/**
 * Receituário: buscar, cadastrar e validar receitas.
 *
 * Alteração: abas Buscar e Nova usam fieldErrors + focarPrimeiroErro em vez de
 * disabled no botão — usuário vê qual campo falta (número, prescritor).
 */
type Tab = 'buscar' | 'nova' | 'validar'

const TIPOS_RECEITA: TipoReceita[] = [
  'SIMPLES',
  'AZUL',
  'AMARELA',
  'BRANCA_ESPECIAL',
  'ANTIMICROBIANO',
]

const inputCompact = 'py-2.5 text-sm'

export function ReceitasPage() {
  const [tab, setTab] = useState<Tab>('buscar')
  const [numeroBusca, setNumeroBusca] = useState('')
  const [receita, setReceita] = useState<Receita | null>(null)
  const [error, setError] = useState('')
  const [validacaoResult, setValidacaoResult] = useState<ValidarReceitaResult | null>(null)

  const [numeroNova, setNumeroNova] = useState('')
  const [tipoNova, setTipoNova] = useState<TipoReceita>('SIMPLES')
  const [prescritorId, setPrescritorId] = useState(PRESCRITOR_DEV_ID)
  const [cid, setCid] = useState('')
  const novaFormRef = useRef<HTMLDivElement>(null)
  const buscaFormRef = useRef<HTMLDivElement>(null)
  const { fieldErrors, setErroTemporario, limparErros } = useErrosCampo()

  const [medId, setMedId] = useState('')
  const [quantidade, setQuantidade] = useState(1)

  const podeValidar = canValidarReceita()
  const funcionarioId = getFuncionarioId()
  const userEmail = getUserEmail()

  const authQuery = useQuery({
    queryKey: ['auth-contexto', funcionarioId],
    queryFn: fetchAuthContexto,
    staleTime: 5 * 60_000,
    enabled: podeValidar && !!funcionarioId,
  })

  const possuiRegistroFarmaceutico = authQuery.data?.possuiRegistroFarmaceutico ?? false
  const avisoSemCrf = podeValidar && authQuery.isSuccess && !possuiRegistroFarmaceutico

  const medsQuery = useQuery({
    queryKey: ['medicamentos-receita'],
    queryFn: fetchAllMedicamentos,
    staleTime: 60_000,
    enabled: tab === 'validar' && !!receita,
  })

  const prescritoresQuery = useQuery({
    queryKey: ['prescritores'],
    queryFn: fetchPrescritores,
    staleTime: 60_000,
    enabled: tab === 'nova',
  })

  const prescritorOpts =
    prescritoresQuery.data?.map((p) => ({
      value: p.id,
      label: p.nome,
      sublabel: `CRM ${p.crm}/${p.ufCrm}`,
    })) ?? []

  const validarBusca = (): boolean => {
    const err = validarObrigatorio(numeroBusca)
    setErroTemporario('numeroBusca', err ?? undefined)
    if (err) focarPrimeiroErro(buscaFormRef.current)
    return !err
  }

  const validarNovaReceita = (): boolean => {
    const numeroErr = validarObrigatorio(numeroNova)
    const prescritorErr = validarSelecao(prescritorId)
    setErroTemporario('numeroNova', numeroErr ?? undefined)
    setErroTemporario('prescritorId', prescritorErr ?? undefined)
    const valido = !numeroErr && !prescritorErr
    if (!valido) focarPrimeiroErro(novaFormRef.current)
    return valido
  }

  const buscarMutation = useMutation({
    mutationFn: () => fetchReceitaPorNumero(numeroBusca.trim()),
    onSuccess: (data) => {
      setReceita(data)
      setError('')
      setValidacaoResult(null)
      if (podeValidar && possuiRegistroFarmaceutico && data.status === 'PENDENTE') setTab('validar')
    },
    onError: (err: unknown) => {
      setReceita(null)
      setError(traduzirErroApi(err))
    },
  })

  const cadastroMutation = useMutation({
    mutationFn: () =>
      cadastrarReceita({
        numeroReceita: numeroNova.trim(),
        tipo: tipoNova,
        prescritorId,
        cid: cid.trim() || undefined,
      }),
    onSuccess: (data) => {
      setReceita(data)
      setError('')
      setNumeroNova('')
      setTab('buscar')
      setNumeroBusca(data.numeroReceita)
    },
    onError: (err: unknown) => setError(traduzirErroApi(err)),
  })

  const validarMutation = useMutation({
    mutationFn: () =>
      validarReceita(receita!.id, {
        itens: [{ medicamentoId: medId, quantidade }],
      }),
    onSuccess: (result) => {
      setValidacaoResult(result)
      setError('')
      if (receita) {
        setReceita({
          ...receita,
          status: result.aprovada ? 'APROVADA' : 'REJEITADA',
          motivoRejeicao: result.violacoes.join('; '),
        })
      }
    },
    onError: (err: unknown) => setError(traduzirErroApi(err)),
  })

  function calcularProgressoNovaReceita(): number {
    return calcularProgressoCampos([
      numeroNova.trim().length > 0,
      prescritorId.trim().length > 0,
    ])
  }

  const tabs: { id: Tab; label: string; icon: typeof FileText; hide?: boolean }[] = [
    { id: 'buscar', label: 'Consultar', icon: Search },
    { id: 'nova', label: 'Nova receita', icon: Plus },
    { id: 'validar', label: 'Validar', icon: ShieldCheck, hide: !podeValidar },
  ]

  return (
    <div className="page-shell max-w-7xl w-full mx-auto">
      <header className="page-header-band !max-w-none mb-0">
        <div className="flex items-center gap-2 text-sky mb-0.5">
          <FileText className="size-4" />
          <span className="text-xs font-semibold uppercase tracking-widest">Receituário</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight leading-tight">Receitas médicas</h1>
        <p className="text-[#8b9cb3] mt-0.5 text-xs sm:text-sm line-clamp-1">
          Cadastro, consulta e validação farmacêutica (Portaria 344/98)
        </p>
      </header>

      <div className="page-tabs shrink-0 px-4 sm:px-6 lg:px-10 !mt-0 border-b border-white/10 pb-4">
        {tabs.filter((t) => !t.hide).map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`inline-flex items-center gap-1.5 sm:gap-2 px-3 sm:px-4 py-2 sm:py-2.5 rounded-xl text-xs sm:text-sm font-medium transition-all
              ${tab === id
                ? 'bg-sky/15 text-sky border border-sky/30'
                : 'glass text-[#8b9cb3] hover:text-white'
              }`}
          >
            <Icon className="size-3.5 sm:size-4" />
            {label}
          </button>
        ))}
      </div>

      {avisoSemCrf && tab === 'validar' && (
        <div className="shrink-0 mx-4 sm:mx-6 lg:mx-10 mb-3 px-3 py-2.5 sm:px-4 sm:py-3 rounded-xl bg-amber/10 border border-amber/25 text-xs sm:text-sm text-amber flex items-start gap-2">
          <AlertTriangle className="size-4 shrink-0 mt-0.5" />
          <p>
            {userEmail === 'farmaceutico@farmacia.com' ? (
              <>
                Não foi possível localizar seu registro de farmacêutico (CRF) no banco.
                Reinicie a API com perfil <strong className="text-white font-medium">dev</strong>{' '}
                (<code className="text-mint">docker compose up -d</code> e{' '}
                <code className="text-mint">mvn spring-boot:run -pl farmacia-api</code>).
              </>
            ) : (
              <>
                Seu usuário não possui registro de farmacêutico (CRF) vinculado. Apenas
                profissionais com CRF podem validar receitas. Faça login com{' '}
                <strong className="text-white font-medium">farmaceutico@farmacia.com</strong>{' '}
                (senha <strong className="text-white font-medium">farm123</strong>).
              </>
            )}
          </p>
        </div>
      )}

      {error && (
        <div className="shrink-0 mx-4 sm:mx-6 lg:mx-10 mb-3 px-3 py-2 rounded-xl bg-coral/10 border border-coral/25 text-xs sm:text-sm text-coral line-clamp-2">
          {error}
        </div>
      )}

      <div className="page-scroll-body !pt-4 flex flex-col overflow-hidden">
        {tab === 'buscar' && (
          <Card glow="mint" className="h-full p-4 sm:p-5 flex flex-col min-h-0 overflow-hidden">
            <div ref={buscaFormRef}>
            <label className="block text-sm font-medium text-[#8b9cb3] mb-1.5 shrink-0">
              Número da receita
            </label>
            <div className="flex gap-2 sm:gap-3 shrink-0">
              <div className="flex-1 min-w-0 space-y-1.5">
                <input
                  value={numeroBusca}
                  onChange={(e) => {
                    setNumeroBusca(e.target.value)
                    if (fieldErrors.numeroBusca) setErroTemporario('numeroBusca', validarObrigatorio(e.target.value) ?? undefined)
                  }}
                  onBlur={() => setErroTemporario('numeroBusca', validarObrigatorio(numeroBusca) ?? undefined)}
                  placeholder="RX-2026-0001"
                  className={`w-full flex-1 min-w-0 px-3 sm:px-4 rounded-xl glass ${inputCompact} focus:outline-none focus:ring-2 focus:ring-sky/30 ${fieldErrors.numeroBusca ? 'border-coral/50 ring-coral/20' : ''}`}
                />
                {fieldErrors.numeroBusca && (
                  <p className="text-sm text-coral">{fieldErrors.numeroBusca}</p>
                )}
              </div>
              <Button
                size="md"
                loading={buscarMutation.isPending}
                onClick={() => {
                  if (!validarBusca()) return
                  buscarMutation.mutate()
                }}
                disabled={buscarMutation.isPending}
                className="shrink-0 self-start"
              >
                <Search className="size-4" />
                <span className="hidden sm:inline">Buscar</span>
              </Button>
            </div>
            </div>
            {receita && (
              <div className="flex-1 min-h-0 overflow-hidden mt-3">
                <ReceitaDetalhe receita={receita} compact />
              </div>
            )}
          </Card>
        )}

        {tab === 'nova' && (
          <Card className="h-full p-4 sm:p-5 flex flex-col min-h-0 overflow-hidden">
            <div ref={novaFormRef} className="flex-1 min-h-0 flex flex-col gap-3 sm:gap-3.5 overflow-hidden">
              <Input
                label="Número da receita *"
                value={numeroNova}
                onChange={(e) => {
                  setNumeroNova(e.target.value)
                  if (fieldErrors.numeroNova) setErroTemporario('numeroNova', validarObrigatorio(e.target.value) ?? undefined)
                }}
                onBlur={() => setErroTemporario('numeroNova', validarObrigatorio(numeroNova) ?? undefined)}
                error={fieldErrors.numeroNova}
                placeholder="RX-2026-0001"
                className={inputCompact}
              />
              <div className="shrink-0">
                <label className="block text-sm font-medium text-[#8b9cb3] mb-1.5">Tipo</label>
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-1.5">
                  {TIPOS_RECEITA.map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setTipoNova(t)}
                      className={`px-2 py-1.5 sm:py-2 text-[10px] sm:text-xs rounded-lg border transition-colors leading-tight
                        ${tipoNova === t
                          ? 'border-sky/40 bg-sky/10 text-sky'
                          : 'border-white/10 text-[#8b9cb3] hover:border-white/20'
                        }`}
                    >
                      {tipoReceitaLabel(t)}
                    </button>
                  ))}
                </div>
              </div>
              <Select
                label="Prescritor *"
                value={prescritorId}
                onChange={(v) => {
                  setPrescritorId(v)
                  if (fieldErrors.prescritorId) setErroTemporario('prescritorId', validarSelecao(v) ?? undefined)
                }}
                onBlur={() => setErroTemporario('prescritorId', validarSelecao(prescritorId) ?? undefined)}
                options={prescritorOpts}
                loading={prescritoresQuery.isLoading}
                className={inputCompact}
                error={fieldErrors.prescritorId}
              />
              {prescritorOpts.length === 0 && !prescritoresQuery.isLoading && (
                <p className="text-[10px] sm:text-xs text-[#8b9cb3] -mt-1">
                  Cadastre prescritores em <strong className="text-white">Cadastros → Prescritores</strong>.
                </p>
              )}
              <Input
                label="CID (opcional)"
                value={cid}
                onChange={(e) => setCid(e.target.value)}
                className={inputCompact}
              />
            </div>
            <Button
              size="lg"
              className="w-full shrink-0 mt-3 sm:mt-4 relative overflow-hidden"
              loading={cadastroMutation.isPending}
              onClick={() => {
                if (!validarNovaReceita()) return
                cadastroMutation.mutate()
              }}
              disabled={cadastroMutation.isPending}
            >
              <div
                className="absolute inset-y-0 right-0 bg-black/20 transition-all duration-500"
                style={{ width: `${100 - calcularProgressoNovaReceita()}%` }}
              />
              <span className="relative flex items-center gap-1.5">
                <Plus className="size-4" />
                Cadastrar receita
              </span>
            </Button>
          </Card>
        )}

        {tab === 'validar' && (
          <div className="h-full min-h-0 overflow-hidden grid grid-cols-1 lg:grid-cols-2 gap-3 sm:gap-4">
            {!receita ? (
              <Card className="h-full flex flex-col items-center justify-center p-6 text-center lg:col-span-2">
                <ClipboardList className="size-8 sm:size-10 text-[#8b9cb3] mb-2 opacity-50" />
                <p className="text-sm text-[#8b9cb3]">Busque uma receita PENDENTE para validar</p>
              </Card>
            ) : (
              <>
                <Card className="p-4 sm:p-5 min-h-0 overflow-hidden flex flex-col">
                  <ReceitaDetalhe receita={receita} compact />
                  {validacaoResult && (
                    <div className="mt-3 pt-3 border-t border-white/10 shrink-0">
                      <div className="flex items-center gap-2">
                        {validacaoResult.aprovada ? (
                          <CheckCircle2 className="size-5 text-mint shrink-0" />
                        ) : (
                          <XCircle className="size-5 text-coral shrink-0" />
                        )}
                        <p className="font-semibold text-sm">
                          {validacaoResult.aprovada ? 'Aprovada' : 'Rejeitada'}
                        </p>
                      </div>
                      {validacaoResult.violacoes.length > 0 && (
                        <ul className="mt-2 space-y-0.5">
                          {validacaoResult.violacoes.map((v) => (
                            <li key={v} className="text-xs text-coral line-clamp-2">
                              • {v}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </Card>

                {receita.status === 'PENDENTE' && (
                  <Card glow="mint" className="p-4 sm:p-5 flex flex-col min-h-0 overflow-hidden">
                    <h3 className="font-semibold text-sm flex items-center gap-2 shrink-0 mb-3">
                      <ShieldCheck className="size-4 text-mint" />
                      Validação farmacêutica
                    </h3>
                    <div className="flex-1 min-h-0 flex flex-col gap-3">
                      <Select
                        label="Medicamento"
                        value={medId}
                        onChange={setMedId}
                        loading={medsQuery.isLoading}
                        disabled={avisoSemCrf}
                        placeholder="Selecione…"
                        className={inputCompact}
                        options={(medsQuery.data ?? []).map((m) => ({
                          value: m.id,
                          label: m.nomeComercial,
                          sublabel: nivelControleLabel(m.nivelControle),
                        }))}
                      />
                      <Input
                        label="Quantidade solicitada"
                        type="number"
                        min={1}
                        value={String(quantidade)}
                        onChange={(e) => setQuantidade(Number(e.target.value))}
                        className={inputCompact}
                        disabled={avisoSemCrf}
                      />
                    </div>
                    <Button
                      size="lg"
                      className="w-full shrink-0 mt-3"
                      loading={validarMutation.isPending}
                      disabled={avisoSemCrf || !medId}
                      onClick={() => validarMutation.mutate()}
                    >
                      Validar receita
                    </Button>
                  </Card>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function ReceitaDetalhe({ receita, compact = false }: { receita: Receita; compact?: boolean }) {
  return (
    <div className={compact ? '' : 'mt-6 pt-6 border-t border-white/10'}>
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className={`font-mono font-bold truncate ${compact ? 'text-base sm:text-lg' : 'text-lg'}`}>
            {receita.numeroReceita}
          </p>
          <p className="text-xs sm:text-sm text-[#8b9cb3] mt-0.5">{tipoReceitaLabel(receita.tipo)}</p>
        </div>
        <Badge variant={statusReceitaVariant(receita.status)} className="shrink-0">
          {statusReceitaLabel(receita.status)}
        </Badge>
      </div>
      <dl className={`grid grid-cols-2 gap-2 sm:gap-3 mt-3 ${compact ? 'text-xs sm:text-sm' : 'text-sm'}`}>
        <div>
          <dt className="text-[#8b9cb3] text-[10px] sm:text-xs uppercase tracking-wider">Emissão</dt>
          <dd>{formatDateBR(receita.dataEmissao)}</dd>
        </div>
        <div>
          <dt className="text-[#8b9cb3] text-[10px] sm:text-xs uppercase tracking-wider">Validade</dt>
          <dd>{formatDateBR(receita.dataValidade)}</dd>
        </div>
        {receita.prescritorNome && (
          <div className="col-span-2 min-w-0">
            <dt className="text-[#8b9cb3] text-[10px] sm:text-xs uppercase tracking-wider">Prescritor</dt>
            <dd className="truncate">{receita.prescritorNome} · CRM {receita.prescritorCrm}</dd>
          </div>
        )}
        {receita.motivoRejeicao && (
          <div className="col-span-2">
            <dt className="text-[#8b9cb3] text-[10px] sm:text-xs uppercase tracking-wider">Motivo</dt>
            <dd className="text-coral line-clamp-2">{receita.motivoRejeicao}</dd>
          </div>
        )}
      </dl>
      {!compact && (
        <p className="text-xs font-mono text-white/30 mt-4 truncate">ID: {receita.id}</p>
      )}
    </div>
  )
}
