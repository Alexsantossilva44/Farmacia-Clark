import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PackagePlus } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { Medicamento } from '@/types/api'
import { medicamentosToSelectOptions } from '@/lib/cadastro-options'
import { fetchAllMedicamentos, registrarEntradaEstoque } from '@/lib/api'
import { traduzirErroApi } from '@/lib/erros'
import { useErro } from '@/hooks/useErro'
import { useErrosCampo } from '@/hooks/useErrosCampo'
import {
  focarPrimeiroErro,
  validarDataObrigatoria,
  validarObrigatorio,
  validarQuantidade,
  validarSelecao,
} from '@/lib/validacao-formulario'
import {
  dataIsoHojeLocal,
  formatCurrency,
  mensagemEntradaRegistrada,
  normalizeQuantidadeInput,
  parsePrecoInput,
  parseQuantidadeInput,
  precoInputValue,
  roundMoney,
  sugerirPrecoCustoFromPmc,
} from '@/lib/format'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Card } from '@/components/ui/Card'

/**
 * Entrada de mercadoria (lote + validade + quantidade).
 *
 * Alteração: substituiu flag `valido` que só desabilitava o botão — validarFormulario
 * explica cada obrigatório e focarPrimeiroErro posiciona o cursor.
 */
const inputCompact = 'py-2.5 text-sm'

export interface EstoqueOperacaoSucesso {
  medicamentoId: string
  mensagem: string
}

interface Props {
  medicamentoIdInicial?: string | null
  onSucesso?: (result: EstoqueOperacaoSucesso) => void
}

const FORM_INICIAL = {
  medicamentoId: '',
  numeroLote: '',
  dataValidade: '',
  dataFabricacao: '',
  quantidadeInput: '1',
  precoCusto: '',
  qtdMin: '5',
  qtdMax: '500',
  observacao: '',
}

export function EstoqueEntradaPanel({ medicamentoIdInicial, onSucesso }: Props) {
  const qc = useQueryClient()
  const [medicamentoId, setMedicamentoId] = useState(
    medicamentoIdInicial ?? FORM_INICIAL.medicamentoId,
  )
  const [numeroLote, setNumeroLote] = useState(FORM_INICIAL.numeroLote)
  const [dataValidade, setDataValidade] = useState(FORM_INICIAL.dataValidade)
  const [dataFabricacao, setDataFabricacao] = useState(FORM_INICIAL.dataFabricacao)
  const [quantidadeInput, setQuantidadeInput] = useState(FORM_INICIAL.quantidadeInput)
  const [precoCusto, setPrecoCusto] = useState(FORM_INICIAL.precoCusto)
  const [qtdMin, setQtdMin] = useState(FORM_INICIAL.qtdMin)
  const [qtdMax, setQtdMax] = useState(FORM_INICIAL.qtdMax)
  const [observacao, setObservacao] = useState(FORM_INICIAL.observacao)
  const { error, showError, clearError } = useErro()
  const { fieldErrors, setErroTemporario, limparErros } = useErrosCampo()
  const [success, setSuccess] = useState('')
  const formRef = useRef<HTMLFormElement>(null)

  function limparFormulario(medicamentoPreset?: string | null) {
    const preset = medicamentoPreset ?? ''
    setMedicamentoId(preset)
    setNumeroLote(FORM_INICIAL.numeroLote)
    setDataValidade(FORM_INICIAL.dataValidade)
    setDataFabricacao(FORM_INICIAL.dataFabricacao)
    setQuantidadeInput(FORM_INICIAL.quantidadeInput)
    setPrecoCusto(FORM_INICIAL.precoCusto)
    setQtdMin(FORM_INICIAL.qtdMin)
    setQtdMax(FORM_INICIAL.qtdMax)
    setObservacao(FORM_INICIAL.observacao)
    clearError()
    setSuccess('')
    limparErros()
  }

  const medsQuery = useQuery({
    queryKey: ['medicamentos-estoque-entrada'],
    queryFn: fetchAllMedicamentos,
    staleTime: 60_000,
  })

  const medOpts = medicamentosToSelectOptions(medsQuery.data ?? [])

  const hoje = useMemo(() => dataIsoHojeLocal(), [])

  const maxFabricacao = useMemo(() => {
    if (dataValidade && dataValidade <= hoje) return dataValidade
    return hoje
  }, [dataValidade, hoje])

  const medicamentoSelecionado = useMemo(
    () => medsQuery.data?.find((m) => m.id === medicamentoId),
    [medsQuery.data, medicamentoId],
  )

  function aplicarPrecoCustoSugerido(med: Medicamento) {
    setPrecoCusto(String(sugerirPrecoCustoFromPmc(med.precoMaximoConsumidor)))
  }

  function handleMedicamentoChange(id: string) {
    setMedicamentoId(id)
    const med = medsQuery.data?.find((m) => m.id === id)
    if (med) aplicarPrecoCustoSugerido(med)
  }

  function handleValidadeChange(value: string) {
    if (!value) {
      setDataValidade('')
      return
    }
    const normalizada = value < hoje ? hoje : value
    setDataValidade(normalizada)
    if (dataFabricacao && dataFabricacao > normalizada) {
      setDataFabricacao(normalizada)
    }
  }

  function handleFabricacaoChange(value: string) {
    if (!value) {
      setDataFabricacao('')
      return
    }
    let next = value > hoje ? hoje : value
    if (dataValidade && next > dataValidade) next = dataValidade
    setDataFabricacao(next)
  }

  useEffect(() => {
    if (!medicamentoIdInicial) return
    setMedicamentoId(medicamentoIdInicial)
    const med = medsQuery.data?.find((m) => m.id === medicamentoIdInicial)
    if (med) aplicarPrecoCustoSugerido(med)
  }, [medicamentoIdInicial, medsQuery.data])

  useEffect(() => {
    if (dataValidade && dataValidade < hoje) setDataValidade(hoje)
  }, [dataValidade, hoje])

  useEffect(() => {
    if (!dataFabricacao) return
    let next = dataFabricacao
    if (next > hoje) next = hoje
    if (dataValidade && next > dataValidade) next = dataValidade
    if (next !== dataFabricacao) setDataFabricacao(next)
  }, [dataFabricacao, dataValidade, hoje])

  const entradaMutation = useMutation({
    mutationFn: () =>
      registrarEntradaEstoque({
        medicamentoId,
        numeroLote: numeroLote.trim(),
        dataValidade,
        dataFabricacao: dataFabricacao || undefined,
        quantidade: parseQuantidadeInput(quantidadeInput),
        precoCusto: (() => {
          const p = parsePrecoInput(precoCusto)
          return p != null ? roundMoney(p) : undefined
        })(),
        quantidadeMinima: qtdMin ? parseInt(qtdMin, 10) : undefined,
        quantidadeMaxima: qtdMax ? parseInt(qtdMax, 10) : undefined,
        observacao: observacao.trim() || undefined,
      }),
    onSuccess: (data) => {
      const nomeMed =
        data.itemEstoque.medicamentoNome?.trim()
        || medicamentoSelecionado?.nomeComercial
        || ''
      const mensagem = mensagemEntradaRegistrada(
        nomeMed,
        data.lote.numeroLote,
        data.itemEstoque.quantidadeAtual,
      )
      qc.invalidateQueries({ queryKey: ['estoque-itens'] })
      qc.invalidateQueries({ queryKey: ['estoque-alertas'] })
      qc.invalidateQueries({ queryKey: ['estoque-minimo'] })
      qc.invalidateQueries({ queryKey: ['estoque-zerados'] })
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

  const fabricacaoOk =
    !dataFabricacao
    || (dataFabricacao <= hoje && (!dataValidade || dataFabricacao <= dataValidade))

  /** Medicamento, lote, qtd e validade mínima (hoje) — erro inline antes de registrarEntradaEstoque. */
  function validarFormulario(): boolean {
    const medErr = validarSelecao(medicamentoId, 'Medicamento')
    const loteErr = validarObrigatorio(numeroLote, 'Número do lote')
    const qtdErr = validarQuantidade(parseQuantidadeInput(quantidadeInput), 'Quantidade')
    const validadeErr = validarDataObrigatoria(dataValidade, 'Validade', { min: hoje })
    let fabricacaoErr: string | null = null
    if (dataFabricacao && !fabricacaoOk) {
      fabricacaoErr = 'Data de fabricação inválida em relação à validade.'
    }

    setErroTemporario('medicamentoId', medErr ?? undefined)
    setErroTemporario('numeroLote', loteErr ?? undefined)
    setErroTemporario('quantidadeInput', qtdErr ?? undefined)
    setErroTemporario('dataValidade', validadeErr ?? undefined)
    setErroTemporario('dataFabricacao', fabricacaoErr ?? undefined)

    const valido = !medErr && !loteErr && !qtdErr && !validadeErr && !fabricacaoErr
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  function tentarRegistrar() {
    if (!validarFormulario()) return
    entradaMutation.mutate()
  }

  return (
    <Card className="p-5 sm:p-6 max-w-2xl">
      <div className="flex items-center gap-2 mb-4">
        <PackagePlus className="size-5 text-mint" />
        <h2 className="font-semibold">Entrada de mercadoria</h2>
      </div>

      {error && <p className="text-sm text-coral mb-3">{error}</p>}
      {success && <p className="text-sm text-mint mb-3">{success}</p>}

      <p className="text-[10px] text-[#8b9cb3] mb-3 leading-relaxed">
        Informe validade e fabricação conforme a embalagem ou NF-e. O sistema não preenche datas
        automaticamente pelo número do lote — se os campos mudarem sozinhos, é o navegador
        reaproveitando um cadastro anterior (use valores da nota atual).
      </p>

      <form
        ref={formRef}
        className="space-y-3"
        autoComplete="off"
        onSubmit={(e) => e.preventDefault()}
      >
        <Select
          label="Medicamento *"
          value={medicamentoId}
          onChange={handleMedicamentoChange}
          options={medOpts}
          loading={medsQuery.isLoading}
          placeholder="Selecione o produto…"
          className={inputCompact}
          error={fieldErrors.medicamentoId}
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="Número do lote *"
            value={numeroLote}
            onChange={(e) => setNumeroLote(e.target.value)}
            placeholder="Ex.: LOTE-2026-001"
            autoComplete="off"
            className={`font-mono ${inputCompact}`}
            error={fieldErrors.numeroLote}
          />
          <Input
            label="Quantidade *"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            value={quantidadeInput}
            onChange={(e) => setQuantidadeInput(normalizeQuantidadeInput(e.target.value))}
            placeholder="Ex.: 300"
            className={`font-mono ${inputCompact}`}
            error={fieldErrors.quantidadeInput}
          />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <Input
              label="Fabricação"
              type="date"
              max={maxFabricacao}
              value={dataFabricacao}
              onChange={(e) => handleFabricacaoChange(e.target.value)}
              autoComplete="off"
              className={inputCompact}
              error={fieldErrors.dataFabricacao}
            />
            <p className="text-[10px] text-[#8b9cb3] mt-1">
              Até hoje{dataValidade ? ' e não depois da validade' : ''}.
            </p>
          </div>
          <div>
            <Input
              label="Validade *"
              type="date"
              min={hoje}
              value={dataValidade}
              onChange={(e) => handleValidadeChange(e.target.value)}
              autoComplete="off"
              className={inputCompact}
              error={fieldErrors.dataValidade}
            />
            <p className="text-[10px] text-[#8b9cb3] mt-1">Somente hoje ou datas futuras.</p>
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="sm:col-span-1">
            <Input
              label="Preço custo (R$)"
              type="text"
              inputMode="decimal"
              autoComplete="off"
              value={precoCusto}
              onChange={(e) => setPrecoCusto(e.target.value)}
              placeholder="Valor da NF-e"
              className={`font-mono ${inputCompact}`}
            />
            {medicamentoSelecionado && (
              <div className="mt-1.5 space-y-1">
                <p className="text-[10px] text-[#8b9cb3] leading-snug">
                  Não é o PMC de venda (
                  <span className="font-mono text-mint">
                    {formatCurrency(medicamentoSelecionado.precoMaximoConsumidor)}
                  </span>
                  ). É o que a farmácia pagou na compra — ajuste conforme a nota.
                </p>
                <button
                  type="button"
                  className="text-[10px] text-mint hover:underline"
                  onClick={() =>
                    setPrecoCusto(
                      String(precoInputValue(medicamentoSelecionado.precoMaximoConsumidor)),
                    )
                  }
                >
                  Usar PMC como custo
                </button>
                <span className="text-[#8b9cb3] mx-1">·</span>
                <button
                  type="button"
                  className="text-[10px] text-mint hover:underline"
                  onClick={() => aplicarPrecoCustoSugerido(medicamentoSelecionado)}
                >
                  Sugestão 60% do PMC (
                  {formatCurrency(
                    sugerirPrecoCustoFromPmc(medicamentoSelecionado.precoMaximoConsumidor),
                  )}
                  )
                </button>
              </div>
            )}
          </div>
          <Input
            label="Estoque mín."
            type="number"
            min={0}
            value={qtdMin}
            onChange={(e) => setQtdMin(e.target.value)}
            className={inputCompact}
          />
          <Input
            label="Estoque máx."
            type="number"
            min={1}
            value={qtdMax}
            onChange={(e) => setQtdMax(e.target.value)}
            className={inputCompact}
          />
        </div>
        <Input
          label="Observação"
          value={observacao}
          onChange={(e) => setObservacao(e.target.value)}
          placeholder="NF, fornecedor…"
          className={inputCompact}
        />
      </form>

      <Button
        className="mt-5"
        type="button"
        loading={entradaMutation.isPending}
        disabled={entradaMutation.isPending}
        onClick={tentarRegistrar}
      >
        Registrar entrada
      </Button>
    </Card>
  )
}
