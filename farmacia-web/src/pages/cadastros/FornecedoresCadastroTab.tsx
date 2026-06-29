import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { Pencil } from 'lucide-react'
import { atualizarFornecedor, cadastrarFornecedor, fetchFornecedores } from '@/lib/api'
import { canGerenciarCompras } from '@/lib/auth'
import type { Fornecedor } from '@/types/compra'
import { traduzirErroApi } from '@/lib/erros'
import { useErro } from '@/hooks/useErro'
import { useErrosCampo, obrigatorio, calcularProgressoCampos, DELAY_ERRO_MS } from '@/hooks/useErrosCampo'
import { maskCnpjInput, onlyDigits, formatCnpjDisplay } from '@/lib/cadastro-options'
import {
  focarPrimeiroErro,
  validarCnpj,
} from '@/lib/validacao-formulario'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Card } from '@/components/ui/Card'

/**
 * Cadastro e edição de fornecedores de compra (distribuidores / NF-e).
 */
function comparePtBr(a: string, b: string): number {
  return a.localeCompare(b, 'pt-BR', { sensitivity: 'base' })
}

export function FornecedoresCadastroTab() {
  const qc = useQueryClient()
  const formRef = useRef<HTMLDivElement>(null)
  const { error, showError, clearError } = useErro()
  const { fieldErrors, setErroTemporario, limparErros } = useErrosCampo()
  const [success, setSuccess] = useState('')
  const [editId, setEditId] = useState<string | null>(null)
  const [razaoSocial, setRazaoSocial] = useState('')
  const [nomeFantasia, setNomeFantasia] = useState('')
  const [cnpj, setCnpj] = useState('')

  const fornecedoresQuery = useQuery({
    queryKey: ['fornecedores'],
    queryFn: fetchFornecedores,
    staleTime: 30_000,
  })

  function startEdit(f: Fornecedor) {
    setEditId(f.id)
    setRazaoSocial(f.razaoSocial)
    setNomeFantasia(f.nomeFantasia ?? '')
    setCnpj(formatCnpjDisplay(f.cnpj))
    clearError()
    setSuccess('')
    limparErros()
    setTimeout(() => formRef.current?.querySelector<HTMLElement>('input')?.focus(), 0)
  }

  function startNew() {
    setEditId(null)
    setRazaoSocial('')
    setNomeFantasia('')
    setCnpj('')
    clearError()
    setSuccess('')
    limparErros()
  }

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        razaoSocial: razaoSocial.trim(),
        nomeFantasia: nomeFantasia.trim() || undefined,
        cnpj: onlyDigits(cnpj),
      }
      if (editId) return atualizarFornecedor(editId, payload)
      return cadastrarFornecedor(payload)
    },
    onSuccess: () => {
      setSuccess(editId ? 'Fornecedor atualizado.' : 'Fornecedor cadastrado.')
      clearError()
      limparErros()
      setEditId(null)
      setRazaoSocial('')
      setNomeFantasia('')
      setCnpj('')
      qc.invalidateQueries({ queryKey: ['fornecedores'] })
      setTimeout(() => {
        formRef.current?.querySelector<HTMLElement>('input')?.focus()
      }, 0)
    },
    onError: (err: unknown) => {
      setSuccess('')
      showError(traduzirErroApi(err))
    },
  })

  function checarRazaoSocialDuplicada(): boolean {
    const lista = qc.getQueryData<Fornecedor[]>(['fornecedores']) ?? []
    return lista.some(
      f => f.id !== editId && f.razaoSocial.trim().toLowerCase() === razaoSocial.trim().toLowerCase()
    )
  }

  function checarCnpjDuplicado(): boolean {
    const lista = qc.getQueryData<Fornecedor[]>(['fornecedores']) ?? []
    return lista.some(f => f.id !== editId && f.cnpj === onlyDigits(cnpj))
  }

  function erroComLimpeza(campo: string, msg: string, limpar: () => void) {
    setErroTemporario(campo, msg, DELAY_ERRO_MS)
    setTimeout(() => {
      limpar()
      formRef.current?.querySelector<HTMLElement>('input')?.focus()
    }, DELAY_ERRO_MS)
  }

  function validarFormulario(): boolean {
    const razaoErr = obrigatorio(razaoSocial)
    const cnpjErr = validarCnpj(cnpj, true)
    setErroTemporario('razaoSocial', razaoErr)
    setErroTemporario('cnpj', cnpjErr ?? undefined)
    const valido = !razaoErr && !cnpjErr
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  function tentarSalvar() {
    if (checarRazaoSocialDuplicada()) {
      erroComLimpeza('razaoSocial', 'Razão social já cadastrada.', () => setRazaoSocial(''))
      return
    }
    if (checarCnpjDuplicado()) {
      setErroTemporario('cnpj', 'CNPJ já cadastrado.', DELAY_ERRO_MS)
      setTimeout(() => {
        setCnpj('')
        formRef.current?.querySelectorAll<HTMLElement>('input')?.[2]?.focus()
      }, DELAY_ERRO_MS)
      return
    }
    if (!validarFormulario()) return
    saveMutation.mutate()
  }

  function calcularProgresso(): number {
    return calcularProgressoCampos([
      razaoSocial.trim().length > 0,
      onlyDigits(cnpj).length === 14,
    ])
  }

  if (!canGerenciarCompras()) {
    return (
      <Card className="p-8 text-center">
        <p className="text-[#8b9cb3]">
          Cadastro de fornecedores exige perfil <strong className="text-white">Estoquista</strong>,{' '}
          <strong className="text-white">Gerente</strong> ou <strong className="text-white">Administrador</strong>.
        </p>
      </Card>
    )
  }

  const fornecedores = [...(fornecedoresQuery.data ?? [])].sort((a, b) =>
    comparePtBr((a.nomeFantasia ?? a.razaoSocial).trim(), (b.nomeFantasia ?? b.razaoSocial).trim()),
  )

  const tituloFormulario = editId ? 'Editar fornecedor' : 'Novo fornecedor'

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-full min-h-0">
      <div className="flex flex-col min-h-0 overflow-hidden">
        <h2 className="font-semibold mb-1 shrink-0">Fornecedores de compra</h2>
        <p className="text-xs text-[#8b9cb3] mb-3 shrink-0">
          Distribuidores para pedidos e NF-e. Diferente de <strong className="text-white/80">Fabricantes</strong>{' '}
          (laboratório do medicamento no catálogo).
        </p>
        <div className="flex-1 min-h-0 overflow-y-auto glass rounded-2xl border border-white/10 divide-y divide-white/5 overscroll-contain">
          {fornecedoresQuery.isLoading && <p className="p-4 text-sm text-[#8b9cb3]">Carregando…</p>}
          {fornecedoresQuery.isError && (
            <p className="p-4 text-sm text-coral">{traduzirErroApi(fornecedoresQuery.error)}</p>
          )}
          {!fornecedoresQuery.isLoading && fornecedores.length === 0 && (
            <p className="p-4 text-sm text-[#8b9cb3]">Nenhum fornecedor cadastrado.</p>
          )}
          {fornecedores.map((f) => (
            <button
              key={f.id}
              type="button"
              onClick={() => startEdit(f)}
              className={`w-full text-left px-4 py-3 hover:bg-mint/5 transition-colors flex items-center justify-between gap-2
                ${editId === f.id ? 'bg-mint/10 border-l-2 border-mint' : ''}`}
            >
              <div>
                <p className="font-medium text-sm">{f.nomeFantasia ?? f.razaoSocial}</p>
                <p className="text-xs text-[#8b9cb3]">
                  {f.razaoSocial}
                  {f.cnpj ? ` · CNPJ ${formatCnpjDisplay(f.cnpj)}` : ''}
                </p>
              </div>
              <Pencil className="size-3.5 text-red-400 shrink-0" />
            </button>
          ))}
        </div>
      </div>

      <Card className="p-5 sm:p-6 shrink-0 self-start xl:self-stretch">
        <div ref={formRef}>
          <h2 className="font-semibold mb-4">{tituloFormulario}</h2>
          {error && <p className="text-sm text-coral mb-3">{error}</p>}
          {success && <p className="text-sm text-mint mb-3">{success}</p>}
          <div className="glass-section rounded-xl p-4 space-y-3">
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
                  erroComLimpeza('razaoSocial', 'Razão social já cadastrada.', () => setRazaoSocial(''))
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
                if (fieldErrors.cnpj) setErroTemporario('cnpj', validarCnpj(e.target.value, true) ?? undefined)
              }}
              onBlur={() => {
                const cnpjErr = validarCnpj(cnpj, true)
                if (cnpjErr) { setErroTemporario('cnpj', cnpjErr); return }
                if (checarCnpjDuplicado()) {
                  setErroTemporario('cnpj', 'CNPJ já cadastrado.', DELAY_ERRO_MS)
                  setTimeout(() => {
                    setCnpj('')
                    formRef.current?.querySelectorAll<HTMLElement>('input')?.[2]?.focus()
                  }, DELAY_ERRO_MS)
                }
              }}
              error={fieldErrors.cnpj}
              className="font-mono"
              placeholder="00.000.000/0000-00"
              inputMode="numeric"
              maxLength={18}
            />
          </div>
          <div className="flex flex-wrap gap-2 mt-6">
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
                className="relative overflow-hidden"
                loading={saveMutation.isPending}
                disabled={saveMutation.isPending}
                onClick={tentarSalvar}
              >
                <div
                  className="absolute inset-y-0 right-0 bg-black/20 transition-all duration-500"
                  style={{ width: `${100 - calcularProgresso()}%` }}
                />
                <span className="relative">Cadastrar fornecedor</span>
              </Button>
            )}
            {editId && (
              <Button variant="ghost" onClick={startNew}>
                Cancelar
              </Button>
            )}
          </div>
        </div>
      </Card>
    </div>
  )
}