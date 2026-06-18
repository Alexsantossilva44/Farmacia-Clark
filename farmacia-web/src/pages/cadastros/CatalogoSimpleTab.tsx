import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { Pencil, Trash2 } from 'lucide-react'
import { useErrosCampo, MSG_OBRIGATORIO, obrigatorio, calcularProgressoCampos, DELAY_ERRO_MS } from '@/hooks/useErrosCampo'
import {
  ApiError,
  atualizarPrescritor,
  cadastrarCategoria,
  cadastrarFabricante,
  cadastrarPrescritor,
  excluirPrescritor,
  fetchCategorias,
  fetchFabricantes,
  fetchPrescritores,
} from '@/lib/api'
import { canGerenciarMedicamentos } from '@/lib/auth'
import { traduzirErroApi } from '@/lib/erros'
import { useErro } from '@/hooks/useErro'
import { UFS_BR, formatCnpjDisplay, maskCnpjInput, onlyDigits } from '@/lib/cadastro-options'
import {
  focarPrimeiroErro,
  validarCnpj,
  validarObrigatorio,
  validarSelecao,
} from '@/lib/validacao-formulario'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Card } from '@/components/ui/Card'

import type { Categoria, Fabricante, Prescritor } from '@/types/catalogo'

/**
 * Abas Fabricantes, Categorias e Prescritores em Cadastros.
 *
 * Alterações:
 * - Padrão fieldErrors + validarFormulario + focarPrimeiroErro (igual Clientes/Fornecedores).
 * - Categorias: descrição obrigatória (antes opcional no front e na API).
 * - Prescritores: especialidade obrigatória (mesma regra).
 * - Fabricantes: CNPJ obrigatório (coluna NOT NULL em fabricantes; igual Fornecedores).
 * - Títulos do painel direito: Novo Fabricante / Nova Categoria / Novo Prescritor.
 */
type CatalogoKind = 'fabricantes' | 'categorias' | 'prescritores'

interface Props {
  kind: CatalogoKind
}

function comparePtBr(a: string, b: string): number {
  return a.localeCompare(b, 'pt-BR', { sensitivity: 'base' })
}

function sortFabricantes(items: Fabricante[]): Fabricante[] {
  return [...items].sort((a, b) =>
    comparePtBr(
      (a.nomeFantasia || a.razaoSocial).trim(),
      (b.nomeFantasia || b.razaoSocial).trim(),
    ),
  )
}

function sortCategorias(items: Categoria[]): Categoria[] {
  return [...items].sort((a, b) => comparePtBr(a.nome.trim(), b.nome.trim()))
}

function sortPrescritores(items: Prescritor[]): Prescritor[] {
  return [...items].sort((a, b) => comparePtBr(a.nome.trim(), b.nome.trim()))
}

export function CatalogoSimpleTab({ kind }: Props) {
  const qc = useQueryClient()
  const formRef = useRef<HTMLDivElement>(null)
  const { error, showError, clearError } = useErro()
  const [success, setSuccess] = useState('')
  const { fieldErrors, setErroTemporario, limparErros } = useErrosCampo()

  const [razaoSocial, setRazaoSocial] = useState('')
  const [nomeFantasia, setNomeFantasia] = useState('')
  const [cnpj, setCnpj] = useState('')

  const [nomeCategoria, setNomeCategoria] = useState('')
  const [descricao, setDescricao] = useState('')

  const [nomePrescritor, setNomePrescritor] = useState('')
  const [crm, setCrm] = useState('')
  const [ufCrm, setUfCrm] = useState('SP')
  const [especialidade, setEspecialidade] = useState('')
  const [editId, setEditId] = useState<string | null>(null)

  function startEdit(p: Prescritor) {
    setEditId(p.id)
    setNomePrescritor(p.nome)
    setCrm(p.crm)
    setUfCrm(p.ufCrm)
    setEspecialidade(p.especialidade ?? '')
    clearError()
    setSuccess('')
    limparErros()
    setTimeout(() => formRef.current?.querySelector<HTMLElement>('input')?.focus(), 0)
  }

  function startNew() {
    setEditId(null)
    setNomePrescritor('')
    setCrm('')
    setUfCrm('SP')
    setEspecialidade('')
    clearError()
    setSuccess('')
    limparErros()
  }

  const fabricantesQuery = useQuery({
    queryKey: ['fabricantes'],
    queryFn: fetchFabricantes,
    enabled: kind === 'fabricantes',
    staleTime: 60_000,
  })

  const categoriasQuery = useQuery({
    queryKey: ['categorias'],
    queryFn: fetchCategorias,
    enabled: kind === 'categorias',
    staleTime: 60_000,
  })

  const prescritoresQuery = useQuery({
    queryKey: ['prescritores'],
    queryFn: fetchPrescritores,
    enabled: kind === 'prescritores',
    staleTime: 60_000,
  })

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (kind === 'fabricantes') {
        return cadastrarFabricante({
          razaoSocial: razaoSocial.trim(),
          nomeFantasia: nomeFantasia.trim() || undefined,
          cnpj: onlyDigits(cnpj), // CNPJ obrigatório — 14 dígitos; coluna fabricantes.cnpj NOT NULL
        })
      }
      if (kind === 'categorias') {
        // descricao sempre enviada — validação front já impediu submit vazio.
        return cadastrarCategoria({
          nome: nomeCategoria.trim(),
          descricao: descricao.trim(),
        })
      }
      // especialidade sempre enviada — alinhado ao @NotBlank de PrescritorInput na API.
      const prescritorPayload = {
        nome: nomePrescritor.trim(),
        crm: crm.trim(),
        ufCrm,
        especialidade: especialidade.trim(),
      }
      if (editId) return atualizarPrescritor(editId, prescritorPayload)
      return cadastrarPrescritor(prescritorPayload)
    },
    onSuccess: () => {
      setSuccess(editId ? 'Prescritor atualizado.' : 'Cadastro realizado com sucesso.')
      clearError()
      limparErros()
      if (kind === 'fabricantes') {
        setRazaoSocial('')
        setNomeFantasia('')
        setCnpj('')
        qc.invalidateQueries({ queryKey: ['fabricantes'] })
      } else if (kind === 'categorias') {
        setNomeCategoria('')
        setDescricao('')
        qc.invalidateQueries({ queryKey: ['categorias'] })
      } else {
        setEditId(null)
        setNomePrescritor('')
        setCrm('')
        setEspecialidade('')
        qc.invalidateQueries({ queryKey: ['prescritores'] })
      }
      setTimeout(() => {
        formRef.current?.querySelector<HTMLElement>('input, select, textarea')?.focus()
      }, 0)
    },
    onError: (err: unknown) => {
      setSuccess('')
      if (kind === 'fabricantes' && err instanceof ApiError && err.status === 409) {
        if (err.problem?.title === 'Fabricante duplicado') {
          setErroTemporario('razaoSocial', 'Razão social já cadastrada.', DELAY_ERRO_MS)
          setTimeout(() => {
            setRazaoSocial('')
            formRef.current?.querySelector<HTMLInputElement>('input')?.focus()
          }, DELAY_ERRO_MS)
          return
        }
        if (err.problem?.title === 'CNPJ já cadastrado') {
          setErroTemporario('cnpj', 'CNPJ já cadastrado.', DELAY_ERRO_MS)
          setTimeout(() => {
            setCnpj('')
            const inputs = formRef.current?.querySelectorAll<HTMLInputElement>('input')
            inputs?.[2]?.focus()
          }, DELAY_ERRO_MS)
          return
        }
      }
      showError(traduzirErroApi(err))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => excluirPrescritor(id),
    onSuccess: () => {
      setSuccess('Prescritor inativado.')
      setEditId(null)
      setNomePrescritor('')
      setCrm('')
      setUfCrm('SP')
      setEspecialidade('')
      limparErros()
      qc.invalidateQueries({ queryKey: ['prescritores'] })
    },
    onError: (err: unknown) => showError(traduzirErroApi(err)),
  })

  const gerenteOnly = kind === 'fabricantes' || kind === 'categorias'
  if (gerenteOnly && !canGerenciarMedicamentos()) {
    return (
      <Card className="p-8 text-center">
        <p className="text-[#8b9cb3]">
          Cadastro de {kind === 'fabricantes' ? 'fabricantes' : 'categorias'} exige perfil{' '}
          <strong className="text-white">Gerente</strong> ou{' '}
          <strong className="text-white">Administrador</strong>.
        </p>
      </Card>
    )
  }

  const listLoading =
    (kind === 'fabricantes' && fabricantesQuery.isLoading) ||
    (kind === 'categorias' && categoriasQuery.isLoading) ||
    (kind === 'prescritores' && prescritoresQuery.isLoading)

  const fabricantesOrdenados = sortFabricantes(fabricantesQuery.data ?? [])
  const categoriasOrdenadas = sortCategorias(categoriasQuery.data ?? [])
  const prescritoresOrdenados = sortPrescritores(prescritoresQuery.data ?? [])

  /** Regras por aba (kind); inclui descricao e especialidade como obrigatórios. */
  function validarFormulario(): boolean {
    let erros: Record<string, string | undefined> = {}

    if (kind === 'fabricantes') {
      const razaoErr = obrigatorio(razaoSocial)
      erros = {
        razaoSocial: razaoErr,
        cnpj: validarCnpj(cnpj, true) ?? undefined,
      }
    } else if (kind === 'categorias') {
      const catErr = obrigatorio(nomeCategoria)
      const descErr = obrigatorio(descricao)
      erros = {
        nomeCategoria: catErr,
        descricao: descErr,
      }
    } else {
      const nomeErr = obrigatorio(nomePrescritor)
      const crmErr = obrigatorio(crm)
      const espErr = obrigatorio(especialidade)
      erros = {
        nomePrescritor: nomeErr,
        crm: crmErr,
        ufCrm: validarSelecao(ufCrm, 'UF') ? MSG_OBRIGATORIO : undefined,
        especialidade: espErr,
      }
    }

    Object.entries(erros).forEach(([campo, msg]) => setErroTemporario(campo, msg))
    const valido = !Object.values(erros).some(Boolean)
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  function checarRazaoSocialDuplicada(): boolean {
    const lista = qc.getQueryData<Fabricante[]>(['fabricantes']) ?? []
    return lista.some(
      f => f.razaoSocial.trim().toLowerCase() === razaoSocial.trim().toLowerCase()
    )
  }

  function checarNomeCategoriaDuplicado(): boolean {
    const lista = qc.getQueryData<Categoria[]>(['categorias']) ?? []
    return lista.some(
      c => c.nome.trim().toLowerCase() === nomeCategoria.trim().toLowerCase()
    )
  }

  function checarNomePrescitorDuplicado(): boolean {
    const lista = qc.getQueryData<Prescritor[]>(['prescritores']) ?? []
    return lista.some(
      p => p.id !== editId && p.nome.trim().toLowerCase() === nomePrescritor.trim().toLowerCase()
    )
  }

  function checarCrmDuplicado(): boolean {
    const lista = qc.getQueryData<Prescritor[]>(['prescritores']) ?? []
    return lista.some(
      p =>
        p.id !== editId &&
        p.crm.trim() === crm.trim() &&
        p.ufCrm.trim().toUpperCase() === ufCrm.trim().toUpperCase()
    )
  }

  function tentarCadastrar() {
    if (kind === 'fabricantes' && checarRazaoSocialDuplicada()) {
      setErroTemporario('razaoSocial', 'Razão social já cadastrada.', DELAY_ERRO_MS)
      setTimeout(() => {
        setRazaoSocial('')
        formRef.current?.querySelector<HTMLInputElement>('input')?.focus()
      }, DELAY_ERRO_MS)
      return
    }
    if (kind === 'categorias' && checarNomeCategoriaDuplicado()) {
      setErroTemporario('nomeCategoria', 'Nome de categoria já cadastrado.', DELAY_ERRO_MS)
      setTimeout(() => {
        setNomeCategoria('')
        formRef.current?.querySelector<HTMLInputElement>('input')?.focus()
      }, DELAY_ERRO_MS)
      return
    }
    if (kind === 'prescritores' && checarNomePrescitorDuplicado()) {
      setErroTemporario('nomePrescritor', 'Nome já cadastrado.', DELAY_ERRO_MS)
      setTimeout(() => {
        setNomePrescritor('')
        formRef.current?.querySelector<HTMLInputElement>('input')?.focus()
      }, DELAY_ERRO_MS)
      return
    }
    if (kind === 'prescritores' && checarCrmDuplicado()) {
      setErroTemporario('crm', 'CRM já cadastrado para esta UF.', DELAY_ERRO_MS)
      setTimeout(() => {
        setCrm('')
        formRef.current?.querySelectorAll<HTMLInputElement>('input')?.[1]?.focus()
      }, DELAY_ERRO_MS)
      return
    }
    if (!validarFormulario()) return
    saveMutation.mutate()
  }

  function calcularProgresso(): number {
    if (kind === 'fabricantes') {
      return calcularProgressoCampos([
        razaoSocial.trim().length > 0,
        onlyDigits(cnpj).length === 14,
      ])
    }
    if (kind === 'categorias') {
      return calcularProgressoCampos([
        nomeCategoria.trim().length > 0,
        descricao.trim().length > 0,
      ])
    }
    return calcularProgressoCampos([
      nomePrescritor.trim().length > 0,
      crm.trim().length > 0,
      especialidade.trim().length > 0,
    ])
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-full min-h-0">
      <div className="flex flex-col min-h-0 overflow-hidden">
        <h2 className="font-semibold mb-1 shrink-0">
          {kind === 'fabricantes' && 'Fabricantes cadastrados'}
          {kind === 'categorias' && 'Categorias'}
          {kind === 'prescritores' && 'Prescritores (médicos)'}
        </h2>
        {kind === 'fabricantes' && (
          <p className="text-xs text-[#8b9cb3] mb-3 shrink-0">
            Laboratório do medicamento. Para aparecer no catálogo, vincule em{' '}
            <strong className="text-white/80">Cadastros → Medicamentos</strong> ao criar ou editar cada produto.
          </p>
        )}
        <div className="flex-1 min-h-0 overflow-y-auto glass rounded-2xl border border-white/10 divide-y divide-white/5 overscroll-contain">
          {listLoading && <p className="p-4 text-sm text-[#8b9cb3]">Carregando…</p>}
          {kind === 'fabricantes' &&
            fabricantesOrdenados.map((f) => (
              <div key={f.id} className="px-4 py-3">
                <p className="font-medium text-sm">{f.nomeFantasia || f.razaoSocial}</p>
                <p className="text-xs text-[#8b9cb3]">
                  {f.razaoSocial}
                  {f.cnpj ? ` · CNPJ ${formatCnpjDisplay(f.cnpj)}` : ''}
                </p>
              </div>
            ))}
          {kind === 'categorias' &&
            categoriasOrdenadas.map((c) => (
              <div key={c.id} className="px-4 py-3">
                <p className="font-medium text-sm">{c.nome}</p>
                {c.descricao && <p className="text-xs text-[#8b9cb3]">{c.descricao}</p>}
              </div>
            ))}
          {kind === 'prescritores' &&
            prescritoresOrdenados.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => startEdit(p)}
                className={`w-full text-left px-4 py-3 hover:bg-mint/5 transition-colors flex items-center justify-between gap-2
                  ${editId === p.id ? 'bg-mint/10 border-l-2 border-mint' : ''}`}
              >
                <div>
                  <p className="font-medium text-sm">{p.nome}</p>
                  <p className="text-xs text-[#8b9cb3] font-mono">
                    CRM {p.crm}/{p.ufCrm}
                    {p.especialidade ? ` · ${p.especialidade}` : ''}
                  </p>
                </div>
                <Pencil className="size-3.5 text-[#8b9cb3] shrink-0" />
              </button>
            ))}
        </div>
      </div>

      <Card className="p-5 sm:p-6 shrink-0 self-start xl:self-stretch xl:overflow-visible">
        <div ref={formRef}>
        <h2 className="font-semibold mb-4">
          {kind === 'fabricantes' && 'Novo Fabricante'}
          {kind === 'categorias' && 'Nova Categoria'}
          {kind === 'prescritores' && (editId ? 'Editar Prescritor' : 'Novo Prescritor')}
        </h2>
        {error && <p className="text-sm text-coral mb-3">{error}</p>}
        {success && <p className="text-sm text-mint mb-3">{success}</p>}

        <div className="space-y-3">
          {kind === 'fabricantes' && (
            <>
              <Input
                label="Razão social *"
                value={razaoSocial}
                onChange={(e) => {
                  const v = e.target.value
                  if (v && !/^[a-zA-ZÀ-ÿ0-9]/.test(v)) return
                  setRazaoSocial(v)
                  if (fieldErrors.razaoSocial && v.trim()) setErroTemporario('razaoSocial', undefined)
                }}
                onBlur={() => {
                  const vazio = obrigatorio(razaoSocial)
                  if (vazio) { setErroTemporario('razaoSocial', vazio); return }
                  if (checarRazaoSocialDuplicada()) {
                    setErroTemporario('razaoSocial', 'Razão social já cadastrada.', DELAY_ERRO_MS)
                    setTimeout(() => {
                      setRazaoSocial('')
                      formRef.current?.querySelector<HTMLInputElement>('input')?.focus()
                    }, DELAY_ERRO_MS)
                  }
                }}
                error={fieldErrors.razaoSocial}
              />
              <Input
                label="Nome fantasia"
                value={nomeFantasia}
                onChange={(e) => {
                  const v = e.target.value
                  if (v && !/^[a-zA-ZÀ-ÿ0-9]/.test(v)) return
                  setNomeFantasia(v)
                }}
              />
              <Input
                label="CNPJ *"
                value={cnpj}
                onChange={(e) => {
                  setCnpj(maskCnpjInput(e.target.value))
                  if (fieldErrors.cnpj) {
                    setErroTemporario('cnpj', validarCnpj(e.target.value, true) ?? undefined)
                  }
                }}
                onBlur={() =>
                  setErroTemporario('cnpj', validarCnpj(cnpj, true) ?? undefined)
                }
                error={fieldErrors.cnpj}
                className="font-mono"
                placeholder="00.000.000/0000-00"
                inputMode="numeric"
                maxLength={18} // máscara 00.000.000/0000-00 (14 dígitos + formatação)
              />
            </>
          )}
          {kind === 'categorias' && (
            <>
              <Input
                label="Nome da categoria *"
                value={nomeCategoria}
                onChange={(e) => {
                  const v = e.target.value
                  if (v && !/^[a-zA-ZÀ-ÿ0-9]/.test(v)) return
                  setNomeCategoria(v)
                  if (fieldErrors.nomeCategoria && v.trim()) setErroTemporario('nomeCategoria', undefined)
                }}
                onBlur={() => {
                  const vazio = obrigatorio(nomeCategoria)
                  if (vazio) { setErroTemporario('nomeCategoria', vazio); return }
                  if (checarNomeCategoriaDuplicado()) {
                    setErroTemporario('nomeCategoria', 'Nome de categoria já cadastrado.', DELAY_ERRO_MS)
                    setTimeout(() => {
                      setNomeCategoria('')
                      formRef.current?.querySelector<HTMLInputElement>('input')?.focus()
                    }, DELAY_ERRO_MS)
                  }
                }}
                error={fieldErrors.nomeCategoria}
              />
              <Input
                label="Descrição *"
                value={descricao}
                onChange={(e) => {
                  const v = e.target.value
                  if (v && !/^[a-zA-ZÀ-ÿ0-9]/.test(v)) return
                  setDescricao(v)
                  if (fieldErrors.descricao && v.trim()) setErroTemporario('descricao', undefined)
                }}
                onBlur={() => {
                  if (!descricao.trim()) setErroTemporario('descricao', obrigatorio(descricao) ?? undefined)
                }}
                error={fieldErrors.descricao}
              />
            </>
          )}
          {kind === 'prescritores' && (
            <>
              <Input
                label="Nome completo *"
                value={nomePrescritor}
                onChange={(e) => {
                  const v = e.target.value
                  if (v && !/^[a-zA-ZÀ-ÿ0-9]/.test(v)) return
                  setNomePrescritor(v)
                  if (fieldErrors.nomePrescritor && v.trim()) setErroTemporario('nomePrescritor', undefined)
                }}
                onBlur={() => {
                  const vazio = obrigatorio(nomePrescritor)
                  if (vazio) { setErroTemporario('nomePrescritor', vazio); return }
                  if (checarNomePrescitorDuplicado()) {
                    setErroTemporario('nomePrescritor', 'Nome já cadastrado.', DELAY_ERRO_MS)
                    setTimeout(() => {
                      setNomePrescritor('')
                      formRef.current?.querySelector<HTMLInputElement>('input')?.focus()
                    }, DELAY_ERRO_MS)
                  }
                }}
                error={fieldErrors.nomePrescritor}
              />
              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="CRM *"
                  value={crm}
                  onChange={(e) => {
                    const v = e.target.value
                    if (v && !/^[0-9]/.test(v)) return
                    setCrm(v)
                    if (fieldErrors.crm && v.trim()) setErroTemporario('crm', undefined)
                  }}
                  onBlur={() => {
                    const vazio = obrigatorio(crm)
                    if (vazio) { setErroTemporario('crm', vazio); return }
                    if (checarCrmDuplicado()) {
                      setErroTemporario('crm', 'CRM já cadastrado para esta UF.', DELAY_ERRO_MS)
                      setTimeout(() => {
                        setCrm('')
                        formRef.current?.querySelectorAll<HTMLInputElement>('input')?.[1]?.focus()
                      }, DELAY_ERRO_MS)
                    }
                  }}
                  error={fieldErrors.crm}
                />
                <Select
                  label="UF *"
                  value={ufCrm}
                  onChange={(v) => {
                    setUfCrm(v)
                    if (fieldErrors.ufCrm) {
                      setErroTemporario('ufCrm', validarSelecao(v, 'UF') ?? undefined)
                    }
                  }}
                  onBlur={() =>
                    setErroTemporario('ufCrm', validarSelecao(ufCrm, 'UF') ? MSG_OBRIGATORIO : undefined)
                  }
                  options={UFS_BR.map((uf) => ({ value: uf, label: uf }))}
                  error={fieldErrors.ufCrm}
                />
              </div>
              <Input
                label="Especialidade *"
                value={especialidade}
                onChange={(e) => {
                  const v = e.target.value
                  if (v && !/^[a-zA-ZÀ-ÿ0-9]/.test(v)) return
                  setEspecialidade(v)
                  if (fieldErrors.especialidade && v.trim()) setErroTemporario('especialidade', undefined)
                }}
                onBlur={() => {
                  if (!especialidade.trim()) setErroTemporario('especialidade', obrigatorio(especialidade) ?? undefined)
                }}
                error={fieldErrors.especialidade}
              />
            </>
          )}
        </div>

        {kind === 'prescritores' && editId ? (
          <div className="mt-6 flex flex-wrap gap-2">
            <Button
              loading={saveMutation.isPending}
              disabled={saveMutation.isPending || deleteMutation.isPending}
              onClick={tentarCadastrar}
            >
              Salvar alterações
            </Button>
            <Button
              variant="danger"
              loading={deleteMutation.isPending}
              disabled={saveMutation.isPending || deleteMutation.isPending}
              onClick={() => {
                if (window.confirm('Inativar este prescritor?')) deleteMutation.mutate(editId)
              }}
            >
              <Trash2 className="size-4" />
              Inativar
            </Button>
            <Button variant="ghost" disabled={saveMutation.isPending || deleteMutation.isPending} onClick={startNew}>
              Cancelar
            </Button>
          </div>
        ) : (
          <Button
            className="mt-6 relative overflow-hidden"
            loading={saveMutation.isPending}
            disabled={saveMutation.isPending}
            onClick={tentarCadastrar}
          >
            <div
              className="absolute inset-y-0 right-0 bg-black/20 transition-all duration-500"
              style={{ width: `${100 - calcularProgresso()}%` }}
            />
            <span className="relative">
              {kind === 'fabricantes' && 'Cadastrar fabricante'}
              {kind === 'categorias' && 'Cadastrar categoria'}
              {kind === 'prescritores' && 'Cadastrar prescritor'}
            </span>
          </Button>
        )}
        </div>
      </Card>
    </div>
  )
}
