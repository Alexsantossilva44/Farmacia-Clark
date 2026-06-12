import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { cadastrarFornecedor, fetchFornecedores } from '@/lib/api'
import { canGerenciarCompras } from '@/lib/auth'
import { traduzirErroApi } from '@/lib/erros'
import { maskCnpjInput, onlyDigits, formatCnpjDisplay } from '@/lib/cadastro-options'
import {
  focarPrimeiroErro,
  validarCnpj,
  validarObrigatorio,
} from '@/lib/validacao-formulario'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Card } from '@/components/ui/Card'

/**
 * Cadastro de fornecedores de compra (distribuidores / NF-e).
 *
 * Alteração: validação no clique em "Cadastrar fornecedor" — antes o botão ficava disabled
 * sem feedback visual; agora fieldErrors + focarPrimeiroErro guiam o usuário até Razão social e CNPJ.
 */
function comparePtBr(a: string, b: string): number {
  return a.localeCompare(b, 'pt-BR', { sensitivity: 'base' })
}

export function FornecedoresCadastroTab() {
  const qc = useQueryClient()
  // Escopo do formulário para scroll/foco após validação (ver validacao-formulario.ts).
  const formRef = useRef<HTMLDivElement>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [razaoSocial, setRazaoSocial] = useState('')
  const [nomeFantasia, setNomeFantasia] = useState('')
  const [cnpj, setCnpj] = useState('')
  // Mensagens por campo — alimentam a prop error de Input (borda e texto coral).
  const [fieldErrors, setFieldErrors] = useState<{ razaoSocial?: string; cnpj?: string }>({})

  const fornecedoresQuery = useQuery({
    queryKey: ['fornecedores'],
    queryFn: fetchFornecedores,
    staleTime: 30_000,
  })

  const saveMutation = useMutation({
    mutationFn: () =>
      cadastrarFornecedor({
        razaoSocial: razaoSocial.trim(),
        nomeFantasia: nomeFantasia.trim() || undefined,
        cnpj: onlyDigits(cnpj),
      }),
    onSuccess: () => {
      setSuccess('Fornecedor cadastrado.')
      setError('')
      setFieldErrors({})
      setRazaoSocial('')
      setNomeFantasia('')
      setCnpj('')
      qc.invalidateQueries({ queryKey: ['fornecedores'] })
    },
    onError: (err: unknown) => {
      setSuccess('')
      setError(traduzirErroApi(err))
    },
  })

  /** Valida todos os obrigatórios no submit; não bloqueia o botão antecipadamente. */
  function validarFormulario(): boolean {
    const razaoErr = validarObrigatorio(razaoSocial, 'Razão social')
    const cnpjErr = validarCnpj(cnpj, true)
    setFieldErrors({
      razaoSocial: razaoErr ?? undefined,
      cnpj: cnpjErr ?? undefined,
    })
    const valido = !razaoErr && !cnpjErr
    if (!valido) focarPrimeiroErro(formRef.current)
    return valido
  }

  /** Só envia à API se validarFormulario passou — evita 400 genérico da API. */
  function tentarCadastrar() {
    if (!validarFormulario()) return
    saveMutation.mutate()
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
            <div key={f.id} className="px-4 py-3">
              <p className="font-medium text-sm">{f.nomeFantasia ?? f.razaoSocial}</p>
              <p className="text-xs text-[#8b9cb3]">
                {f.razaoSocial}
                {f.cnpj ? ` · CNPJ ${formatCnpjDisplay(f.cnpj)}` : ''}
              </p>
            </div>
          ))}
        </div>
      </div>

      <Card className="p-5 sm:p-6 shrink-0 self-start xl:self-stretch">
        <div ref={formRef}>
        <h2 className="font-semibold mb-4">Novo fornecedor</h2>
        {error && <p className="text-sm text-coral mb-3">{error}</p>}
        {success && <p className="text-sm text-mint mb-3">{success}</p>}
        <div className="space-y-3">
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
            maxLength={18}
          />
        </div>
        {/* disabled apenas durante loading — validação fica a cargo de tentarCadastrar */}
        <Button
          className="mt-6"
          loading={saveMutation.isPending}
          disabled={saveMutation.isPending}
          onClick={tentarCadastrar}
        >
          Cadastrar fornecedor
        </Button>
        </div>
      </Card>
    </div>
  )
}
