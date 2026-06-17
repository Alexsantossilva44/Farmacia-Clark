import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import {
  atualizarMedicamento,
  cadastrarMedicamento,
  excluirMedicamento,
  fetchCategorias,
  fetchFabricantes,
  fetchAllMedicamentos,
} from '@/lib/api'
import { canGerenciarMedicamentos, isAdmin } from '@/lib/auth'
import { traduzirErroApi } from '@/lib/erros'
import { ApiError } from '@/lib/api'
import { useErro } from '@/hooks/useErro'
import {
  FORMAS_FARMACEUTICAS,
  NIVEIS_CONTROLE,
  TIPOS_MEDICAMENTO,
  labelFormaFarmaceutica,
  labelTipoMedicamento,
  onlyDigits,
} from '@/lib/cadastro-options'
import { formatCurrency, nivelControleLabel, nivelControleVariant, roundMoney } from '@/lib/format'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { CATEGORIA_DEV_ID } from '@/types/catalogo'
import type { MedicamentoInput } from '@/types/cadastro'
import type { Medicamento } from '@/types/api'
import { useState, useRef } from 'react'
import { useErrosCampo, MSG_OBRIGATORIO } from '@/hooks/useErrosCampo'
import {
  focarPrimeiroErro,
  validarNumeroPositivo,
  validarObrigatorio,
  validarSelecao,
} from '@/lib/validacao-formulario'

/**
 * Cadastro e edição de medicamentos no catálogo.
 *
 * Alteração: botão "Cadastrar" deixou de usar disabled por campos vazios; validação explícita
 * no clique (nome, PMC, fabricante, categoria) com feedback visual por campo.
 *
 * Limites de texto no front (espelham @Size da API e VARCHAR do banco):
 * - Concentração: 50 caracteres (medicamentos.concentracao VARCHAR(50)).
 * - Apresentação: 100 caracteres (medicamentos.apresentacao VARCHAR(100)).
 */
const emptyForm = (): MedicamentoInput => ({
  codigoEan: '',
  nomeComercial: '',
  nomeGenerico: '',
  tipo: 'GENERICO',
  formaFarmaceutica: 'COMPRIMIDO',
  concentracao: '',
  apresentacao: '',
  requerReceita: false,
  nivelControle: 'LIVRE',
  precoMaximoConsumidor: 0,
  fabricante: { id: '' },
  categoria: { id: CATEGORIA_DEV_ID },
})

function medToForm(m: Medicamento): MedicamentoInput {
  return {
    codigoEan: m.codigoEan ?? '',
    codigoAnvisa: m.codigoAnvisa,
    nomeComercial: m.nomeComercial,
    nomeGenerico: m.nomeGenerico ?? '',
    tipo: m.tipo,
    formaFarmaceutica: m.formaFarmaceutica,
    concentracao: m.concentracao ?? '',
    apresentacao: m.apresentacao ?? '',
    classeTerapeutica: m.classeTerapeutica,
    requerReceita: m.requerReceita,
    nivelControle: m.nivelControle,
    precoMaximoConsumidor: m.precoMaximoConsumidor,
    fabricante: { id: m.fabricante?.id ?? '' },
    categoria: { id: m.categoria?.id ?? CATEGORIA_DEV_ID },
  }
}

export function MedicamentosCadastroTab() {
  const qc = useQueryClient()
  const podeGerenciar = canGerenciarMedicamentos()
  const podeExcluir = isAdmin()

  const [editId, setEditId] = useState<string | null>(null)
  const [form, setForm] = useState<MedicamentoInput>(emptyForm)
  const [pmcCents, setPmcCents] = useState(0)
  const { error, showError, clearError } = useErro()
  const [success, setSuccess] = useState('')
  const formRef = useRef<HTMLDivElement>(null)
  const { fieldErrors, setErroTemporario, limparErros } = useErrosCampo()
  const nomeRef = useRef<HTMLInputElement>(null)
  const eanRef = useRef<HTMLInputElement>(null)

  function formatPmcDisplay(cents: number): string {
    if (cents === 0) return ''
    const reais = Math.floor(cents / 100)
    const centavos = cents % 100
    return `R$ ${reais.toLocaleString('pt-BR')},${String(centavos).padStart(2, '0')}`
  }

  function handlePmcChange(e: React.ChangeEvent<HTMLInputElement>) {
    const digits = e.target.value.replace(/\D/g, '')
    const cents = Math.min(parseInt(digits || '0', 10), 99999999)
    setPmcCents(cents)
    const value = roundMoney(cents / 100)
    setForm((prev) => ({ ...prev, precoMaximoConsumidor: value }))
    if (fieldErrors.precoMaximoConsumidor && value > 0) {
      setErroTemporario('precoMaximoConsumidor', undefined)
    }
  }

  const medsQuery = useQuery({
    queryKey: ['medicamentos-cadastro'],
    queryFn: fetchAllMedicamentos,
    staleTime: 30_000,
  })

  const fabricantesQuery = useQuery({
    queryKey: ['fabricantes'],
    queryFn: fetchFabricantes,
    staleTime: 60_000,
  })

  const categoriasQuery = useQuery({
    queryKey: ['categorias'],
    queryFn: fetchCategorias,
    staleTime: 60_000,
  })

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload: MedicamentoInput = {
        ...form,
        codigoEan: form.codigoEan?.trim() || undefined,
        nomeGenerico: form.nomeGenerico?.trim() || undefined,
        precoMaximoConsumidor: roundMoney(Number(form.precoMaximoConsumidor) || 0),
      }
      if (editId) return atualizarMedicamento(editId, payload)
      return cadastrarMedicamento(payload)
    },
    onSuccess: () => {
      setSuccess(editId ? 'Medicamento atualizado.' : 'Medicamento cadastrado.')
      clearError()
      limparErros()
      setEditId(null)
      setForm(emptyForm())
      setPmcCents(0)
      qc.invalidateQueries({ queryKey: ['medicamentos'] })
      qc.invalidateQueries({ queryKey: ['medicamentos-cadastro'] })
    },
    onError: (err: unknown) => {
      setSuccess('')
      let afterDismiss: (() => void) | undefined
      if (err instanceof ApiError) {
        const fieldName = err.problem?.fields?.[0]?.name
        if (fieldName === 'codigoEan') {
          afterDismiss = () => {
            setForm((prev) => ({ ...prev, codigoEan: '' }))
            eanRef.current?.focus()
          }
        } else if (err.status === 409) {
          afterDismiss = () => {
            setForm((prev) => ({ ...prev, nomeComercial: '' }))
            nomeRef.current?.focus()
          }
        }
      }
      showError(traduzirErroApi(err), afterDismiss)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => excluirMedicamento(id),
    onSuccess: () => {
      setSuccess('Medicamento inativado.')
      setEditId(null)
      setForm(emptyForm())
      setPmcCents(0)
      qc.invalidateQueries({ queryKey: ['medicamentos'] })
      qc.invalidateQueries({ queryKey: ['medicamentos-cadastro'] })
    },
    onError: (err: unknown) => showError(traduzirErroApi(err)),
  })

  function startNew() {
    setEditId(null)
    setForm(emptyForm())
    setPmcCents(0)
    clearError()
    setSuccess('')
    setFieldErrors({})
  }

  function startEdit(m: Medicamento) {
    setEditId(m.id)
    setForm(medToForm(m))
    setPmcCents(Math.round(m.precoMaximoConsumidor * 100))
    clearError()
    setSuccess('')
    setFieldErrors({})
  }

  /** PMC > 0 e selects preenchidos — antes o botão ficava cinza sem explicar o motivo. */
  function validarFormulario(): boolean {
    const nomeErr = validarObrigatorio(form.nomeComercial, 'Nome comercial')
    const pmcErr = validarNumeroPositivo(form.precoMaximoConsumidor, 'PMC (R$)')
    const fabErr = validarSelecao(form.fabricante.id, 'Fabricante')
    const catErr = validarSelecao(form.categoria.id, 'Categoria')
    setErroTemporario('nomeComercial', nomeErr ? MSG_OBRIGATORIO : undefined)
    setErroTemporario('precoMaximoConsumidor', pmcErr ? MSG_OBRIGATORIO : undefined)
    setErroTemporario('fabricante', fabErr ? MSG_OBRIGATORIO : undefined)
    setErroTemporario('categoria', catErr ? MSG_OBRIGATORIO : undefined)
    const valido = !nomeErr && !pmcErr && !fabErr && !catErr
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  function formularioPronto(): boolean {
    return (
      form.nomeComercial.trim().length > 0 &&
      form.precoMaximoConsumidor > 0 &&
      form.fabricante.id.length > 0
    )
  }

  function calcularProgresso(): number {
    const campos = [
      form.nomeComercial.trim().length > 0,
      form.precoMaximoConsumidor > 0,
      form.fabricante.id.length > 0,
    ]
    return Math.round((campos.filter(Boolean).length / campos.length) * 100)
  }

  function tentarSalvar() {
    if (!validarFormulario()) return
    saveMutation.mutate()
  }

  if (!podeGerenciar) {
    return (
      <Card className="p-8 text-center">
        <p className="text-[#8b9cb3]">
          Cadastro de medicamentos exige perfil <strong className="text-white">Gerente</strong> ou{' '}
          <strong className="text-white">Administrador</strong>.
        </p>
      </Card>
    )
  }

  const fabricanteOpts =
    fabricantesQuery.data?.map((f) => ({
      value: f.id,
      label: f.nomeFantasia || f.razaoSocial,
      sublabel: f.cnpj ? `CNPJ ${f.cnpj}` : undefined,
    })) ?? []

  const categoriaOpts =
    categoriasQuery.data?.map((c) => ({
      value: c.id,
      label: c.nome,
    })) ?? []

  const tituloFormulario = editId ? 'Editar medicamento' : 'Novo medicamento'

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 lg:gap-6 h-full min-h-0">
      <div className="flex flex-col min-h-0 overflow-hidden order-2 lg:order-1">
        <div className="shrink-0 flex items-center justify-between gap-3 mb-3">
          <h2 className="font-semibold">Produtos cadastrados</h2>
          <Button variant="secondary" size="sm" onClick={startNew}>
            <Plus className="size-4" />
            Novo
          </Button>
        </div>
        <div className="flex-1 min-h-0 overflow-y-auto overscroll-contain glass rounded-2xl border border-white/10 divide-y divide-white/5">
          {medsQuery.isLoading && (
            <p className="p-6 text-sm text-[#8b9cb3]">Carregando…</p>
          )}
          {medsQuery.data?.filter((m) => m.ativo !== false).map((m) => (
            <button
              key={m.id}
              type="button"
              onClick={() => startEdit(m)}
              className={`w-full text-left px-4 py-3 hover:bg-mint/5 transition-colors
                ${editId === m.id ? 'bg-mint/10 border-l-2 border-mint' : ''}`}
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-medium text-sm">{m.nomeComercial}</p>
                  <p className="text-xs text-[#8b9cb3] mt-0.5">{m.nomeGenerico}</p>
                </div>
                <Badge variant={nivelControleVariant(m.nivelControle)} className="shrink-0">
                  {nivelControleLabel(m.nivelControle)}
                </Badge>
              </div>
              <p className="text-xs text-[#8b9cb3] mt-1">
                {m.fabricante?.nomeFantasia ?? m.fabricante?.razaoSocial ?? 'Sem fabricante'}
              </p>
              <p className="text-xs font-mono text-mint/70 mt-0.5">{formatCurrency(m.precoMaximoConsumidor)}</p>
            </button>
          ))}
        </div>
      </div>

      <Card className="flex flex-col min-h-0 overflow-hidden p-5 sm:p-6 order-1 lg:order-2">
        <div className="shrink-0 mb-4">
          <h2 className="font-semibold">{tituloFormulario}</h2>
          {!editId && (
            <p className="text-xs text-[#8b9cb3] mt-0.5">
              Campos marcados com <span className="text-coral">*</span> são obrigatórios para cadastrar.
            </p>
          )}
        </div>
        {error && <p className="text-sm text-coral mb-3 shrink-0">{error}</p>}
        {success && <p className="text-sm text-mint mb-3 shrink-0">{success}</p>}

        <div className="flex-1 min-h-0 overflow-y-auto overscroll-contain pr-1 -mr-1">
          <div ref={formRef} className="space-y-3">
          <Input
            ref={nomeRef}
            label="Nome comercial *"
            value={form.nomeComercial}
            onChange={(e) => {
              setForm({ ...form, nomeComercial: e.target.value })
              if (fieldErrors.nomeComercial && e.target.value.trim()) {
                setErroTemporario('nomeComercial', undefined)
              }
            }}
            onBlur={() => {
              if (!form.nomeComercial.trim()) setErroTemporario('nomeComercial', MSG_OBRIGATORIO)
            }}
            error={fieldErrors.nomeComercial}
          />
          <Input
            label="Nome genérico (DCB)"
            value={form.nomeGenerico ?? ''}
            onChange={(e) => setForm({ ...form, nomeGenerico: e.target.value })}
          />
          <div className="grid grid-cols-2 gap-3">
            <Input
              ref={eanRef}
              label="EAN-13"
              value={form.codigoEan ?? ''}
              onChange={(e) => setForm({ ...form, codigoEan: onlyDigits(e.target.value).slice(0, 13) })}
              className="font-mono"
              placeholder="7891234567890"
            />
            <Input
              label="PMC (R$) *"
              type="text"
              inputMode="decimal"
              placeholder="R$ 0,00"
              value={formatPmcDisplay(pmcCents)}
              onChange={handlePmcChange}
              onBlur={() => {
                if (validarNumeroPositivo(form.precoMaximoConsumidor, 'PMC (R$)')) {
                  setErroTemporario('precoMaximoConsumidor', MSG_OBRIGATORIO)
                }
              }}
              error={fieldErrors.precoMaximoConsumidor}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Tipo *"
              value={form.tipo}
              onChange={(v) => setForm({ ...form, tipo: v as MedicamentoInput['tipo'] })}
              options={TIPOS_MEDICAMENTO.map((t) => ({ value: t, label: labelTipoMedicamento(t) }))}
            />
            <Select
              label="Forma farmacêutica"
              value={form.formaFarmaceutica ?? 'COMPRIMIDO'}
              onChange={(v) =>
                setForm({ ...form, formaFarmaceutica: v as MedicamentoInput['formaFarmaceutica'] })
              }
              options={FORMAS_FARMACEUTICAS.map((f) => ({
                value: f,
                label: labelFormaFarmaceutica(f),
              }))}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            {/* Concentração: max 50 — MedicamentoInput @Size(max=50) / VARCHAR(50) */}
            <Input
              label="Concentração"
              value={form.concentracao ?? ''}
              onChange={(e) =>
                setForm({ ...form, concentracao: e.target.value.slice(0, 50) })
              }
              placeholder="500mg"
              maxLength={50}
            />
            {/* Apresentação: max 100 — MedicamentoInput @Size(max=100) / VARCHAR(100) */}
            <Input
              label="Apresentação"
              value={form.apresentacao ?? ''}
              onChange={(e) =>
                setForm({ ...form, apresentacao: e.target.value.slice(0, 100) })
              }
              placeholder={labelFormaFarmaceutica(form.formaFarmaceutica ?? 'COMPRIMIDO')}
              maxLength={100}
            />
          </div>
          <Select
            label="Nível de controle *"
            value={form.nivelControle}
            onChange={(v) => {
              const nivel = v as MedicamentoInput['nivelControle']
              setForm({
                ...form,
                nivelControle: nivel,
                requerReceita: nivel !== 'LIVRE',
              })
            }}
            options={NIVEIS_CONTROLE.map((n) => ({ value: n, label: nivelControleLabel(n) }))}
          />
          <Select
            label="Fabricante *"
            value={form.fabricante.id}
            onChange={(v) => {
              setForm({ ...form, fabricante: { id: v } })
              if (fieldErrors.fabricante) {
                setErroTemporario('fabricante', validarSelecao(v, 'Fabricante') ?? undefined)
              }
            }}
            onBlur={() => {
              if (!form.fabricante.id) setErroTemporario('fabricante', MSG_OBRIGATORIO)
            }}
            options={fabricanteOpts}
            loading={fabricantesQuery.isLoading}
            placeholder="Selecione o laboratório…"
            error={fieldErrors.fabricante}
          />
          <p className="text-xs text-[#8b9cb3] -mt-1">
            Cadastre laboratórios em <strong className="text-white/80">Cadastros → Fabricantes</strong>. O nome no
            produto (ex.: Mylan) não substitui este vínculo.
          </p>
          <Select
            label="Categoria *"
            value={form.categoria.id}
            onChange={(v) => {
              setForm({ ...form, categoria: { id: v } })
              if (fieldErrors.categoria) {
                setFieldErrors((prev) => ({
                  ...prev,
                  categoria: validarSelecao(v, 'Categoria') ?? undefined,
                }))
              }
            }}
            onBlur={() =>
              setFieldErrors((prev) => ({
                ...prev,
                categoria: validarSelecao(form.categoria.id, 'Categoria') ?? undefined,
              }))
            }
            options={categoriaOpts}
            loading={categoriasQuery.isLoading}
            error={fieldErrors.categoria}
          />
          <label className="flex items-center gap-2 text-sm text-[#8b9cb3] cursor-pointer">
            <input
              type="checkbox"
              checked={form.requerReceita}
              onChange={(e) => setForm({ ...form, requerReceita: e.target.checked })}
              className="rounded border-white/20 bg-white/5 text-mint focus:ring-mint/40"
            />
            Exige receita médica
          </label>
          </div>
        </div>

        <div className="shrink-0 flex flex-wrap gap-2 pt-4 mt-4 border-t border-white/10">
          {editId ? (
            <Button
              loading={saveMutation.isPending}
              disabled={saveMutation.isPending}
              onClick={tentarSalvar}
            >
              Salvar alterações
            </Button>
          ) : (
            <Button
              type="button"
              loading={saveMutation.isPending}
              disabled={saveMutation.isPending}
              onClick={tentarSalvar}
              className="relative overflow-hidden"
            >
              <div
                className="absolute inset-y-0 right-0 bg-black/20 transition-all duration-500"
                style={{ width: `${100 - calcularProgresso()}%` }}
              />
              <span className="relative">Cadastrar medicamento</span>
            </Button>
          )}
          {editId && podeExcluir && (
            <Button
              variant="danger"
              loading={deleteMutation.isPending}
              onClick={() => {
                if (window.confirm('Inativar este medicamento?')) deleteMutation.mutate(editId)
              }}
            >
              <Trash2 className="size-4" />
              Inativar
            </Button>
          )}
          {editId && (
            <Button variant="ghost" onClick={startNew}>
              Cancelar
            </Button>
          )}
        </div>
      </Card>
    </div>
  )
}
