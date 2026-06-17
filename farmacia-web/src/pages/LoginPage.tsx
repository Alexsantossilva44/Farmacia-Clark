import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Activity, ArrowRight, ShieldCheck } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { login } from '@/lib/api'
import { saveToken, isAuthenticated } from '@/lib/auth'
import { limparCacheUsuario } from '@/lib/queryClient'
import { FARMACIA_NOME_COMPLETO } from '@/lib/branding'
import { traduzirErroApi, mensagemValidacaoCampo } from '@/lib/erros'
import { useErro } from '@/hooks/useErro'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Card } from '@/components/ui/Card'

/**
 * Contas de demonstração — espelham DevAmbienteSeed (perfil dev).
 * Inclui os 5 papéis de RoleSistema: Admin, Gerente, Farmacêutico, Estoquista, Balconista.
 */
const DEMO_ACCOUNTS = [
  { email: 'admin@farmacia.com', senha: 'admin123', role: 'Administrador' },
  { email: 'gerente@farmacia.com', senha: 'ger123', role: 'Gerente' },
  { email: 'farmaceutico@farmacia.com', senha: 'farm123', role: 'Farmacêutico' },
  { email: 'estoquista@farmacia.com', senha: 'est123', role: 'Estoquista' },
  { email: 'balconista@farmacia.com', senha: 'bal123', role: 'Balconista' },
]

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? '/'

  const [email, setEmail] = useState(import.meta.env.DEV ? 'admin@farmacia.com' : '')
  const [senha, setSenha] = useState(import.meta.env.DEV ? 'admin123' : '')
  const { error, showError, clearError } = useErro()

  useEffect(() => {
    if (isAuthenticated()) {
      navigate(from, { replace: true })
    }
  }, [from, navigate])

  const mutation = useMutation({
    mutationFn: () => login(email.trim(), senha),
    onSuccess: (data) => {
      limparCacheUsuario()
      saveToken(data.token, data.expiraEmSegundos)
      navigate(from, { replace: true })
    },
    onError: (err: unknown) => showError(traduzirErroApi(err)),
  })

  function fillDemo(account: (typeof DEMO_ACCOUNTS)[0]) {
    setEmail(account.email)
    setSenha(account.senha)
    clearError()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    clearError()

    if (!email.trim()) {
      showError(mensagemValidacaoCampo('e-mail'))
      return
    }
    if (!senha) {
      showError(mensagemValidacaoCampo('senha'))
      return
    }

    mutation.mutate()
  }

  return (
    <div className="min-h-screen flex">
      {/* Painel editorial esquerdo */}
      <div className="hidden lg:flex lg:w-[52%] relative flex-col justify-between p-12 overflow-hidden">
        <div className="absolute -top-24 -left-24 size-96 rounded-full bg-mint/10 blur-3xl" />
        <div className="absolute bottom-20 right-10 size-64 rounded-full bg-coral/10 blur-3xl" />

        <div className="relative z-10 flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-2xl bg-mint/15 border border-mint/30 glow-mint">
            <Activity className="size-6 text-mint" />
          </div>
          <div>
            <p className="font-bold text-2xl">{FARMACIA_NOME_COMPLETO}</p>
            <p className="text-sm text-[#8b9cb3]">Operação farmacêutica integrada</p>
          </div>
        </div>

        <div className="relative z-10 max-w-lg -ml-3">
          <h1 className="text-5xl font-bold leading-[1.1] tracking-tight">
            Controle ANVISA,
            <span className="text-mint"> estoque FEFO</span>
            <br />
            e PDV na mesma tela.
          </h1>
          <p className="mt-6 text-lg text-[#8b9cb3] leading-relaxed">
            Catálogo com níveis de controle, validação de receitas e rastreabilidade
            SNGPC — construído sobre a API Spring Boot do sistema.
          </p>

          <div className="mt-10 grid grid-cols-1 sm:grid-cols-2 gap-4">
            {[
              { value: '7', label: 'Níveis de controle' },
              { value: 'FEFO', label: 'Dispensação por validade' },
              { value: 'JWT', label: 'Autenticação por perfil' },
              { value: 'RFC 7807', label: 'Erros padronizados' },
            ].map((stat) => (
              <div key={stat.label} className="glass rounded-xl p-4 border-l-2 border-l-mint/50">
                <p className="text-2xl font-bold font-mono text-mint">{stat.value}</p>
                <p className="text-xs text-[#8b9cb3] mt-1 uppercase tracking-wider">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>

        <p className="relative z-10 text-xs text-white/30">
          Sistema Farmacêutico · Java 21 · Spring Boot 3 · PostgreSQL
        </p>
      </div>

      {/* Formulário */}
      <div className="flex flex-1 items-center justify-center p-6 lg:p-12">
        <div className="w-full max-w-md">
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <Activity className="size-8 text-mint" />
            <span className="font-bold text-xl">{FARMACIA_NOME_COMPLETO}</span>
          </div>

          <Card glow="mint" className="p-8">
            <div className="flex items-center gap-2 text-mint mb-1">
              <ShieldCheck className="size-4" />
              <span className="text-xs font-semibold uppercase tracking-widest">Acesso seguro</span>
            </div>
            <h2 className="text-2xl font-bold mt-2">Entrar no sistema</h2>
            <p className="text-sm text-[#8b9cb3] mt-1 mb-6">
              Use suas credenciais corporativas de funcionário.
            </p>

            <form onSubmit={handleSubmit} noValidate className="space-y-4">
              <Input
                label="E-mail corporativo"
                type="email"
                data-testid="login-email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu@farmacia.com"
                autoComplete="username"
              />
              <Input
                label="Senha"
                type="password"
                data-testid="login-senha"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                autoComplete="current-password"
              />

              {error && (
                <div className="px-4 py-3 rounded-xl bg-coral/10 border border-coral/25 text-sm text-coral">
                  {error}
                </div>
              )}

              <Button
                type="submit"
                size="lg"
                className="w-full"
                loading={mutation.isPending}
                data-testid="login-submit"
              >
                Acessar painel
                <ArrowRight className="size-4" />
              </Button>
            </form>

            {import.meta.env.DEV && (
              <div className="mt-6 pt-6 border-t border-white/10">
                <p className="text-xs text-[#8b9cb3] mb-3 uppercase tracking-wider">
                  Contas de desenvolvimento
                </p>
                <div className="flex flex-wrap gap-2">
                  {DEMO_ACCOUNTS.map((acc) => (
                    <button
                      key={acc.email}
                      type="button"
                      onClick={() => fillDemo(acc)}
                      className="px-3 py-1.5 text-xs rounded-lg glass hover:border-mint/30 hover:text-mint transition-colors"
                    >
                      {acc.role}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}
