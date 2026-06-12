import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import {
  cadastrarCategoria,
  cadastrarFabricante,
  cadastrarPrescritor,
  fetchCategorias,
  fetchFabricantes,
  fetchPrescritores,
} from '@/lib/api'
import { canGerenciarMedicamentos } from '@/lib/auth'
import { traduzirErroApi } from '@/lib/erros'
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
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string | undefined>>({})

  const [razaoSocial, setRazaoSocial] = useState('')
  const [nomeFantasia, setNomeFantasia] = useState('')
  const [cnpj, setCnpj] = useState('')

  const [nomeCategoria, setNomeCategoria] = useState('')
  const [descricao, setDescricao] = useState('')

  const [nomePrescritor, setNomePrescritor] = useState('')
  const [crm, setCrm] = useState('')
  const [ufCrm, setUfCrm] = useState('SP')
  const [especialidade, setEspecialidade] = useState('')

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
      return cadastrarPrescritor({
        nome: nomePrescritor.trim(),
        crm: crm.trim(),
        ufCrm,
        especialidade: especialidade.trim(),
      })
    },
    onSuccess: () => {
      setSuccess('Cadastro realizado com sucesso.')
      setError('')
      setFieldErrors({})
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
        setNomePrescritor('')
        setCrm('')
        setEspecialidade('')
        qc.invalidateQueries({ queryKey: ['prescritores'] })
      }
    },
    onError: (err: unknown) => {
      setSuccess('')
      setError(traduzirErroApi(err))
    },
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
    const erros: Record<string, string | undefined> = {}

    if (kind === 'fabricantes') {
      erros.razaoSocial = validarObrigatorio(razaoSocial, 'Razão social') ?? undefined
      // obrigatorio=true — fabricantes.cnpj é NOT NULL UNIQUE no banco (V1).
      erros.cnpj = validarCnpj(cnpj, true) ?? undefined
    } else if (kind === 'categorias') {
      erros.nomeCategoria = validarObrigatorio(nomeCategoria, 'Nome da categoria') ?? undefined
      // Descrição deixou de ser opcional — categoriza o uso no catálogo de medicamentos.
      erros.descricao = validarObrigatorio(descricao, 'Descrição') ?? undefined
    } else {
      erros.nomePrescritor = validarObrigatorio(nomePrescritor, 'Nome completo') ?? undefined
      erros.crm = validarObrigatorio(crm, 'CRM') ?? undefined
      erros.ufCrm = validarSelecao(ufCrm, 'UF') ?? undefined
      // Especialidade exigida para identificar o prescritor na receituário/validação.
      erros.especialidade = validarObrigatorio(especialidade, 'Especialidade') ?? undefined
    }

    setFieldErrors(erros)
    const valido = !Object.values(erros).some(Boolean)
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  function tentarCadastrar() {
    if (!validarFormulario()) return
    saveMutation.mutate()
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
              <div key={p.id} className="px-4 py-3">
                <p className="font-medium text-sm">{p.nome}</p>
                <p className="text-xs text-[#8b9cb3] font-mono">
                  CRM {p.crm}/{p.ufCrm}
                  {p.especialidade ? ` · ${p.especialidade}` : ''}
                </p>
              </div>
            ))}
        </div>
      </div>

      <Card className="p-5 sm:p-6 shrink-0 self-start xl:self-stretch xl:overflow-visible">
        <div ref={formRef}>
        <h2 className="font-semibold mb-4">
          {/* Título específico por aba — substitui o genérico "Novo cadastro". */}
          {kind === 'fabricantes' && 'Novo Fabricante'}
          {kind === 'categorias' && 'Nova Categoria'}
          {kind === 'prescritores' && 'Novo Prescritor'}
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
                  setRazaoSocial(e.target.value)
                  if (fieldErrors.razaoSocial) {
                    setFieldErrors((prev) => ({
                      ...prev,
                      razaoSocial: validarObrigatorio(e.target.value, 'Razão social') ?? undefined,
                    }))
                  }
                }}
                onBlur={() =>
                  setFieldErrors((prev) => ({
                    ...prev,
                    razaoSocial: validarObrigatorio(razaoSocial, 'Razão social') ?? undefined,
                  }))
                }
                error={fieldErrors.razaoSocial}
              />
              <Input
                label="Nome fantasia"
                value={nomeFantasia}
                onChange={(e) => setNomeFantasia(e.target.value)}
              />
              <Input
                label="CNPJ *"
                value={cnpj}
                onChange={(e) => {
                  setCnpj(maskCnpjInput(e.target.value))
                  if (fieldErrors.cnpj) {
                    setFieldErrors((prev) => ({
                      ...prev,
                      // true = obrigatório; alinhado a FabricanteInput @NotBlank e NOT NULL no banco.
                      cnpj: validarCnpj(e.target.value, true) ?? undefined,
                    }))
                  }
                }}
                onBlur={() =>
                  setFieldErrors((prev) => ({
                    ...prev,
                    cnpj: validarCnpj(cnpj, true) ?? undefined,
                  }))
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
                  setNomeCategoria(e.target.value)
                  if (fieldErrors.nomeCategoria) {
                    setFieldErrors((prev) => ({
                      ...prev,
                      nomeCategoria: validarObrigatorio(e.target.value, 'Nome da categoria') ?? undefined,
                    }))
                  }
                }}
                onBlur={() =>
                  setFieldErrors((prev) => ({
                    ...prev,
                    nomeCategoria: validarObrigatorio(nomeCategoria, 'Nome da categoria') ?? undefined,
                  }))
                }
                error={fieldErrors.nomeCategoria}
              />
              <Input
                label="Descrição *"
                value={descricao}
                onChange={(e) => {
                  setDescricao(e.target.value)
                  if (fieldErrors.descricao) {
                    setFieldErrors((prev) => ({
                      ...prev,
                      descricao: validarObrigatorio(e.target.value, 'Descrição') ?? undefined,
                    }))
                  }
                }}
                onBlur={() =>
                  setFieldErrors((prev) => ({
                    ...prev,
                    descricao: validarObrigatorio(descricao, 'Descrição') ?? undefined,
                  }))
                }
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
                  setNomePrescritor(e.target.value)
                  if (fieldErrors.nomePrescritor) {
                    setFieldErrors((prev) => ({
                      ...prev,
                      nomePrescritor: validarObrigatorio(e.target.value, 'Nome completo') ?? undefined,
                    }))
                  }
                }}
                onBlur={() =>
                  setFieldErrors((prev) => ({
                    ...prev,
                    nomePrescritor: validarObrigatorio(nomePrescritor, 'Nome completo') ?? undefined,
                  }))
                }
                error={fieldErrors.nomePrescritor}
              />
              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="CRM *"
                  value={crm}
                  onChange={(e) => {
                    setCrm(e.target.value)
                    if (fieldErrors.crm) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        crm: validarObrigatorio(e.target.value, 'CRM') ?? undefined,
                      }))
                    }
                  }}
                  onBlur={() =>
                    setFieldErrors((prev) => ({
                      ...prev,
                      crm: validarObrigatorio(crm, 'CRM') ?? undefined,
                    }))
                  }
                  error={fieldErrors.crm}
                />
                <Select
                  label="UF *"
                  value={ufCrm}
                  onChange={(v) => {
                    setUfCrm(v)
                    if (fieldErrors.ufCrm) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        ufCrm: validarSelecao(v, 'UF') ?? undefined,
                      }))
                    }
                  }}
                  onBlur={() =>
                    setFieldErrors((prev) => ({
                      ...prev,
                      ufCrm: validarSelecao(ufCrm, 'UF') ?? undefined,
                    }))
                  }
                  options={UFS_BR.map((uf) => ({ value: uf, label: uf }))}
                  error={fieldErrors.ufCrm}
                />
              </div>
              <Input
                label="Especialidade *"
                value={especialidade}
                onChange={(e) => {
                  setEspecialidade(e.target.value)
                  if (fieldErrors.especialidade) {
                    setFieldErrors((prev) => ({
                      ...prev,
                      especialidade: validarObrigatorio(e.target.value, 'Especialidade') ?? undefined,
                    }))
                  }
                }}
                onBlur={() =>
                  setFieldErrors((prev) => ({
                    ...prev,
                    especialidade: validarObrigatorio(especialidade, 'Especialidade') ?? undefined,
                  }))
                }
                error={fieldErrors.especialidade}
              />
            </>
          )}
        </div>

        <Button
          className="mt-6"
          loading={saveMutation.isPending}
          disabled={saveMutation.isPending}
          onClick={tentarCadastrar}
        >
          Cadastrar
        </Button>
        </div>
      </Card>
    </div>
  )
}
