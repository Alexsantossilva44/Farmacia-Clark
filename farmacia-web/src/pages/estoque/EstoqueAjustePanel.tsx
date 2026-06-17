import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Scale } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import {
  fetchEstoqueSaldo,
  fetchAllMedicamentos,
  fetchLotesParaAjuste,
  registrarAjusteSaldo,
} from '@/lib/api'
import {
  formatDateBR,
  normalizeQuantidadeInput,
  parseQuantidadeInput,
} from '@/lib/format'
import { medicamentosToSelectOptions } from '@/lib/cadastro-options'
import { traduzirErroApi } from '@/lib/erros'
import { useErro } from '@/hooks/useErro'
import { useErrosCampo } from '@/hooks/useErrosCampo'
import {
  focarPrimeiroErro,
  validarMotivoMinimo,
  validarQuantidade,
  validarSelecao,
} from '@/lib/validacao-formulario'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Card } from '@/components/ui/Card'
import type { TipoAjusteSaldo } from '@/types/estoque'

const inputCompact = 'py-2.5 text-sm'
const MOTIVO_MIN = 10

/**
 * Ajuste de inventário (perda, quebra, sobra).
 *
 * Alteração: validação no clique com motivo mínimo (auditoria) e mensagem se não houver lote cadastrado.
 */
import type { EstoqueOperacaoSucesso } from '@/pages/estoque/EstoqueEntradaPanel'

interface Props {
  medicamentoIdInicial?: string | null
  onSucesso?: (result: EstoqueOperacaoSucesso) => void
}

export function EstoqueAjustePanel({ medicamentoIdInicial, onSucesso }: Props) {
  const qc = useQueryClient()
  const [medicamentoId, setMedicamentoId] = useState(medicamentoIdInicial ?? '')
  const [loteId, setLoteId] = useState('')
  const [tipo, setTipo] = useState<TipoAjusteSaldo>('AJUSTE_NEGATIVO')
  const [quantidadeInput, setQuantidadeInput] = useState('1')
  const [motivo, setMotivo] = useState('')
  const { error, showError, clearError } = useErro()
  const { fieldErrors, setErroTemporario, limparErros } = useErrosCampo()
  const [success, setSuccess] = useState('')
  const formRef = useRef<HTMLDivElement>(null)

  function limparFormulario(medicamentoPreset?: string | null) {
    setMedicamentoId(medicamentoPreset ?? '')
    setLoteId('')
    setTipo('AJUSTE_NEGATIVO')
    setQuantidadeInput('1')
    setMotivo('')
    clearError()
    setSuccess('')
    limparErros()
  }

  useEffect(() => {
    if (!medicamentoIdInicial) return
    setMedicamentoId(medicamentoIdInicial)
  }, [medicamentoIdInicial])

  useEffect(() => {
    setLoteId('')
  }, [medicamentoId])

  const medsQuery = useQuery({
    queryKey: ['medicamentos-estoque-ajuste'],
    queryFn: fetchAllMedicamentos,
    staleTime: 60_000,
  })

  const saldoQuery = useQuery({
    queryKey: ['estoque-saldo-ajuste', medicamentoId],
    queryFn: () => fetchEstoqueSaldo(medicamentoId),
    enabled: !!medicamentoId,
    retry: false,
  })

  const lotesQuery = useQuery({
    queryKey: ['estoque-lotes-ajuste', medicamentoId],
    queryFn: () => fetchLotesParaAjuste(medicamentoId),
    enabled: !!medicamentoId,
  })

  const medOpts = medicamentosToSelectOptions(medsQuery.data ?? [])

  const loteOpts =
    lotesQuery.data?.map((l) => ({
      value: l.id,
      label: `${l.numeroLote} — ${l.quantidadeAtual} un.`,
      sublabel: `Val. ${formatDateBR(l.dataValidade)} · ${l.status}`,
    })) ?? []

  const ajusteMutation = useMutation({
    mutationFn: () =>
      registrarAjusteSaldo({
        medicamentoId,
        loteId,
        tipo,
        quantidade: parseQuantidadeInput(quantidadeInput),
        motivo: motivo.trim(),
      }),
    onSuccess: (data) => {
      const qtd = parseQuantidadeInput(quantidadeInput)
      const sinal = tipo === 'AJUSTE_POSITIVO' ? '+' : '−'
      const mensagem = `Ajuste registrado (${sinal}${qtd}) — saldo atual: ${data.itemEstoque.quantidadeAtual} un.`
      qc.invalidateQueries({ queryKey: ['estoque-itens'] })
      qc.invalidateQueries({ queryKey: ['estoque-alertas'] })
      qc.invalidateQueries({ queryKey: ['estoque-minimo'] })
      qc.invalidateQueries({ queryKey: ['estoque-zerados'] })
      qc.invalidateQueries({ queryKey: ['estoque-saldo-ajuste', medicamentoId] })
      qc.invalidateQueries({ queryKey: ['estoque-lotes-ajuste', medicamentoId] })
      qc.invalidateQueries({ queryKey: ['estoque-movimentacoes'] })
      qc.invalidateQueries({ queryKey: ['estoque-disponivel-venda-pdv'] })

      if (onSucesso) {
        onSucesso({ medicamentoId: data.itemEstoque.medicamentoId, mensagem })
        return
      }

      limparFormulario()
      setSuccess(mensagem)
      clearError()
    },
    onError: (err: unknown) => {
      setSuccess('')
      showError(traduzirErroApi(err))
    },
  })

  /** Inclui regra de lote inexistente (orienta registrar entrada antes). */
  function validarFormulario(): boolean {
    const medErr = validarSelecao(medicamentoId, 'Medicamento')
    let loteErr: string | null = null
    if (!medErr && medicamentoId) {
      if (!lotesQuery.data?.length) {
        loteErr = 'Nenhum lote disponível — registre entrada primeiro.'
      } else {
        loteErr = validarSelecao(loteId, 'Lote')
      }
    }
    const qtdErr = validarQuantidade(parseQuantidadeInput(quantidadeInput), 'Quantidade')
    const motivoErr = validarMotivoMinimo(motivo, MOTIVO_MIN, 'Motivo')

    setErroTemporario('medicamentoId', medErr ?? undefined)
    setErroTemporario('loteId', loteErr ?? undefined)
    setErroTemporario('quantidadeInput', qtdErr ?? undefined)
    setErroTemporario('motivo', motivoErr ?? undefined)

    const valido = !medErr && !loteErr && !qtdErr && !motivoErr
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  function tentarAjustar() {
    if (!validarFormulario()) return
    ajusteMutation.mutate()
  }

  return (
    <Card className="p-5 sm:p-6 max-w-2xl">
      <div className="flex items-center gap-2 mb-1">
        <Scale className="size-5 text-amber" />
        <h2 className="font-semibold">Ajuste de saldo</h2>
      </div>
      <p className="text-sm text-[#8b9cb3] mb-4">
        Correção de inventário, perda ou quebra. Motivo obrigatório para auditoria.
      </p>

      {error && <p className="text-sm text-coral mb-3">{error}</p>}
      {success && <p className="text-sm text-mint mb-3">{success}</p>}

      <div ref={formRef} className="space-y-3">
        <Select
          label="Medicamento *"
          value={medicamentoId}
          onChange={setMedicamentoId}
          options={medOpts}
          loading={medsQuery.isLoading}
          placeholder="Selecione o produto…"
          className={inputCompact}
          error={fieldErrors.medicamentoId}
        />

        {medicamentoId && saldoQuery.data && (
          <p className="text-sm text-[#8b9cb3]">
            Saldo consolidado:{' '}
            <span className="font-mono font-semibold text-mint">
              {saldoQuery.data.quantidadeAtual}
            </span>{' '}
            un.
          </p>
        )}
        {medicamentoId && saldoQuery.isError && (
          <p className="text-sm text-amber">
            Sem registro de estoque — use ajuste positivo após cadastrar um lote via Entrada.
          </p>
        )}

        <Select
          label="Lote *"
          value={loteId}
          onChange={setLoteId}
          options={loteOpts}
          loading={lotesQuery.isLoading}
          placeholder={
            medicamentoId
              ? lotesQuery.data?.length
                ? 'Selecione o lote…'
                : 'Nenhum lote — registre entrada primeiro'
              : 'Selecione o medicamento…'
          }
          disabled={!medicamentoId || lotesQuery.data?.length === 0}
          className={inputCompact}
          error={fieldErrors.loteId}
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Select
            label="Tipo de ajuste *"
            value={tipo}
            onChange={(v) => setTipo(v as TipoAjusteSaldo)}
            options={[
              { value: 'AJUSTE_NEGATIVO', label: 'Saída (−)', sublabel: 'Perda, quebra, inventário' },
              { value: 'AJUSTE_POSITIVO', label: 'Entrada (+)', sublabel: 'Sobra encontrada' },
            ]}
            className={inputCompact}
          />
          <Input
            label="Quantidade *"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            value={quantidadeInput}
            onChange={(e) => setQuantidadeInput(normalizeQuantidadeInput(e.target.value))}
            placeholder="Ex.: 10"
            className={`font-mono ${inputCompact}`}
            error={fieldErrors.quantidadeInput}
          />
        </div>

        <div>
          <Input
            label={`Motivo * (mín. ${MOTIVO_MIN} caracteres)`}
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            placeholder="Ex.: Inventário anual — contagem física divergente da sistema"
            className={inputCompact}
            error={fieldErrors.motivo}
          />
          <p className="text-xs text-[#8b9cb3] mt-1">
            {motivo.trim().length}/{MOTIVO_MIN} caracteres
          </p>
        </div>
      </div>

      <Button
        className="mt-5"
        loading={ajusteMutation.isPending}
        disabled={ajusteMutation.isPending}
        onClick={tentarAjustar}
      >
        Confirmar ajuste
      </Button>
    </Card>
  )
}
