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
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const formRef = useRef<HTMLDivElement>(null)
  // Erros inline nos campos marcados com * no formulário.
  const [fieldErrors, setFieldErrors] = useState<{
    nomeComercial?: string
    precoMaximoConsumidor?: string
    fabricante?: string
    categoria?: string
  }>({})

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
      setError('')
      setFieldErrors({})
      setEditId(null)
      setForm(emptyForm())
      qc.invalidateQueries({ queryKey: ['medicamentos'] })
      qc.invalidateQueries({ queryKey: ['medicamentos-cadastro'] })
    },
    onError: (err: unknown) => {
      setSuccess('')
      setError(traduzirErroApi(err))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => excluirMedicamento(id),
    onSuccess: () => {
      setSuccess('Medicamento inativado.')
      setEditId(null)
      setForm(emptyForm())
      qc.invalidateQueries({ queryKey: ['medicamentos'] })
      qc.invalidateQueries({ queryKey: ['medicamentos-cadastro'] })
    },
    onError: (err: unknown) => setError(traduzirErroApi(err)),
  })

  function startNew() {
    setEditId(null)
    setForm(emptyForm())
    setError('')
    setSuccess('')
    setFieldErrors({})
  }

  function startEdit(m: Medicamento) {
    setEditId(m.id)
    setForm(medToForm(m))
    setError('')
    setSuccess('')
    setFieldErrors({})
  }

  /** PMC > 0 e selects preenchidos — antes o botão ficava cinza sem explicar o motivo. */
  function validarFormulario(): boolean {
    const nomeErr = validarObrigatorio(form.nomeComercial, 'Nome comercial')
    const pmcErr = validarNumeroPositivo(form.precoMaximoConsumidor, 'PMC (R$)')
    const fabErr = validarSelecao(form.fabricante.id, 'Fabricante')
    const catErr = validarSelecao(form.categoria.id, 'Categoria')
    setFieldErrors({
      nomeComercial: nomeErr ?? undefined,
      precoMaximoConsumidor: pmcErr ?? undefined,
      fabricante: fabErr ?? undefined,
      categoria: catErr ?? undefined,
    })
    const valido = !nomeErr && !pmcErr && !fabErr && !catErr
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
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
    <div className="h-full min-h-0 flex flex-col">
      <div className="shrink-0 grid grid-cols-1 lg:grid-cols-2 gap-4 lg:gap-6 pb-4
        bg-bg-deep/95 backdrop-blur-sm border-b border-white/10">
        <div className="flex items-center justify-between gap-3">
          <h2 className="font-semibold">Produtos cadastrados</h2>
          <Button variant="secondary" size="sm" onClick={startNew}>
            <Plus className="size-4" />
            Novo
          </Button>
        </div>
        <h2 className="font-semibold xl:pt-0 pt-1">{tituloFormulario}</h2>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto overscroll-contain">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 pt-4 pb-6">
          <div className="glass rounded-2xl border border-white/10 divide-y divide-white/5 min-h-[12rem]">
            {medsQuery.isLoading && (
              <p className="p-6 text-sm text-[#8b9cb3]">Carregando…</p>
            )}
            {medsQuery.data?.map((m) => (
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

          <Card className="p-5 sm:p-6">
            {error && <p className="text-sm text-coral mb-3">{error}</p>}
            {success && <p className="text-sm text-mint mb-3">{success}</p>}

            <div ref={formRef} className="space-y-3">
          <Input
            label="Nome comercial *"
            value={form.nomeComercial}
            onChange={(e) => {
              setForm({ ...form, nomeComercial: e.target.value })
              if (fieldErrors.nomeComercial) {
                setFieldErrors((prev) => ({
                  ...prev,
                  nomeComercial: validarObrigatorio(e.target.value, 'Nome comercial') ?? undefined,
                }))
              }
            }}
            onBlur={() =>
              setFieldErrors((prev) => ({
                ...prev,
                nomeComercial: validarObrigatorio(form.nomeComercial, 'Nome comercial') ?? undefined,
              }))
            }
            error={fieldErrors.nomeComercial}
          />
          <Input
            label="Nome genérico (DCB)"
            value={form.nomeGenerico ?? ''}
            onChange={(e) => setForm({ ...form, nomeGenerico: e.target.value })}
          />
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="EAN-13"
              value={form.codigoEan ?? ''}
              onChange={(e) => setForm({ ...form, codigoEan: onlyDigits(e.target.value).slice(0, 13) })}
              className="font-mono"
              placeholder="7891234567890"
            />
            <Input
              label="PMC (R$) *"
              type="number"
              min={0.01}
              step={0.01}
              value={form.precoMaximoConsumidor || ''}
              onChange={(e) => {
                setForm({ ...form, precoMaximoConsumidor: parseFloat(e.target.value) || 0 })
                if (fieldErrors.precoMaximoConsumidor) {
                  setFieldErrors((prev) => ({
                    ...prev,
                    precoMaximoConsumidor:
                      validarNumeroPositivo(parseFloat(e.target.value) || 0, 'PMC (R$)') ?? undefined,
                  }))
                }
              }}
              onBlur={() =>
                setFieldErrors((prev) => ({
                  ...prev,
                  precoMaximoConsumidor:
                    validarNumeroPositivo(form.precoMaximoConsumidor, 'PMC (R$)') ?? undefined,
                }))
              }
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
                setFieldErrors((prev) => ({
                  ...prev,
                  fabricante: validarSelecao(v, 'Fabricante') ?? undefined,
                }))
              }
            }}
            onBlur={() =>
              setFieldErrors((prev) => ({
                ...prev,
                fabricante: validarSelecao(form.fabricante.id, 'Fabricante') ?? undefined,
              }))
            }
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

        <div className="flex flex-wrap gap-2 mt-6">
          <Button
            loading={saveMutation.isPending}
            disabled={saveMutation.isPending}
            onClick={tentarSalvar}
          >
            {editId ? 'Salvar alterações' : 'Cadastrar medicamento'}
          </Button>
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
      </div>
    </div>
  )
}
