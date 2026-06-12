import {
  Pill,
  TrendingUp,
  AlertTriangle,
  Clock,
  ArrowUpRight,
  Zap,
} from 'lucide-react'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { FARMACIA_NOME_COMPLETO } from '@/lib/branding'
import { getUserEmail } from '@/lib/auth'

const stats = [
  {
    label: 'Medicamentos ativos',
    value: '—',
    sub: 'Sincronizado com API',
    icon: Pill,
    accent: 'mint' as const,
  },
  {
    label: 'Alertas de vencimento',
    value: '—',
    sub: 'Módulo em integração',
    icon: AlertTriangle,
    accent: 'amber' as const,
  },
  {
    label: 'Vendas hoje',
    value: '—',
    sub: 'PDV em breve',
    icon: TrendingUp,
    accent: 'coral' as const,
  },
  {
    label: 'Caixas abertos',
    value: '—',
    sub: 'Abertura de caixa pendente',
    icon: Clock,
    accent: 'sky' as const,
  },
]

const modules = [
  {
    title: 'Catálogo ANVISA',
    desc: 'Medicamentos com nível de controle, PMC e fabricante.',
    href: '/medicamentos',
    ready: true,
    tag: 'Ativo',
  },
  {
    title: 'PDV & Vendas',
    desc: 'Dispensação com FEFO, pagamento e cupom fiscal.',
    href: '/vendas',
    ready: true,
    tag: 'Ativo',
  },
  {
    title: 'Estoque FEFO',
    desc: 'Lotes, validades e alertas de ruptura.',
    href: '/estoque',
    ready: true,
    tag: 'Ativo',
  },
  {
    title: 'Receituário',
    desc: 'Validação de receitas branca especial, azul e amarela.',
    href: '/receitas',
    ready: true,
    tag: 'Ativo',
  },
  {
    title: 'Compras & NF-e',
    desc: 'Notas fiscais de entrada e recebimento de mercadorias.',
    href: '/compras',
    ready: true,
    tag: 'Ativo',
  },
  {
    title: 'Cadastros',
    desc: 'Medicamentos, clientes, fabricantes, categorias e prescritores.',
    href: '/cadastros',
    ready: true,
    tag: 'Ativo',
  },
]

const accentBorder = {
  mint: 'border-l-mint',
  amber: 'border-l-amber',
  coral: 'border-l-coral',
  sky: 'border-l-sky',
}

const accentIcon = {
  mint: 'text-mint',
  amber: 'text-amber',
  coral: 'text-coral',
  sky: 'text-sky',
}

export function DashboardPage() {
  const email = getUserEmail()
  const hour = new Date().getHours()
  const greeting =
    hour < 12 ? 'Bom dia' : hour < 18 ? 'Boa tarde' : 'Boa noite'

  return (
    <div className="page-shell">
      <header className="page-header-band relative">
        <div className="absolute -top-8 left-4 lg:left-6 size-32 bg-mint/5 rounded-full blur-2xl pointer-events-none" />
        <p className="relative text-sm text-mint font-medium uppercase tracking-widest mb-2">
          {greeting}
        </p>
        <h1 className="relative text-2xl sm:text-4xl lg:text-5xl font-bold tracking-tight max-w-2xl leading-tight">
          Painel operacional
          <span className="text-[#8b9cb3] font-normal text-2xl lg:text-3xl block mt-1 truncate">
            {email ?? FARMACIA_NOME_COMPLETO}
          </span>
        </h1>
      </header>

      {/* Cards com scroll */}
      <div className="page-scroll-body">
      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-10">
        {stats.map((stat) => (
          <Card
            key={stat.label}
            hover
            className={`border-l-4 ${accentBorder[stat.accent]}`}
          >
            <div className="flex items-start justify-between">
              <stat.icon className={`size-5 ${accentIcon[stat.accent]}`} />
              <Badge variant={stat.accent} dot>
                Em tempo real
              </Badge>
            </div>
            <p className="text-3xl font-bold mt-4 font-mono">{stat.value}</p>
            <p className="text-sm font-medium mt-1">{stat.label}</p>
            <p className="text-xs text-[#8b9cb3] mt-0.5">{stat.sub}</p>
          </Card>
        ))}
      </div>

      {/* Módulos */}
      <section>
        <div className="flex items-end justify-between mb-5">
          <div>
            <h2 className="text-xl font-bold">Módulos do sistema</h2>
            <p className="text-sm text-[#8b9cb3] mt-1">
              Complexidade do backend mapeada para telas incrementais
            </p>
          </div>
          <div className="hidden sm:flex items-center gap-1.5 text-xs text-mint">
            <Zap className="size-3.5" />
            API Spring Boot · porta 8080
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pb-4">
          {modules.map((mod) => (
            <a
              key={mod.title}
              href={mod.ready ? mod.href : undefined}
              className={`block ${!mod.ready ? 'cursor-default' : ''}`}
            >
              <Card hover={mod.ready} glow={mod.ready ? 'mint' : 'none'} className="h-full group">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <Badge variant={mod.ready ? 'mint' : 'sky'}>{mod.tag}</Badge>
                    <h3 className="text-lg font-semibold mt-3 group-hover:text-mint transition-colors">
                      {mod.title}
                    </h3>
                    <p className="text-sm text-[#8b9cb3] mt-1 leading-relaxed">{mod.desc}</p>
                  </div>
                  {mod.ready && (
                    <ArrowUpRight className="size-5 text-[#8b9cb3] group-hover:text-mint shrink-0 transition-colors" />
                  )}
                </div>
              </Card>
            </a>
          ))}
        </div>
      </section>
      </div>
    </div>
  )
}
