import { useEffect, useState, useSyncExternalStore } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  Pill,
  ShoppingCart,
  Package,
  FileText,
  LogOut,
  Activity,
  ClipboardList,
  Truck,
  Menu,
  X,
} from 'lucide-react'
import { clearToken, getUserEmail, getUserRoles } from '@/lib/auth'
import { limparCacheUsuario } from '@/lib/queryClient'
import { FARMACIA_NOME_MARCA, FARMACIA_NOME_PRINCIPAL } from '@/lib/branding'
import { roleLabel } from '@/lib/format'

function subscribeLg(cb: () => void) {
  const mq = window.matchMedia('(min-width: 1024px)')
  mq.addEventListener('change', cb)
  return () => mq.removeEventListener('change', cb)
}

function getIsLg() {
  return window.matchMedia('(min-width: 1024px)').matches
}

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Painel', end: true },
  { to: '/medicamentos', icon: Pill, label: 'Catálogo' },
  { to: '/vendas', icon: ShoppingCart, label: 'PDV / Vendas' },
  { to: '/estoque', icon: Package, label: 'Estoque' },
  { to: '/compras', icon: Truck, label: 'Compras' },
  { to: '/receitas', icon: FileText, label: 'Receituário' },
  { to: '/cadastros', icon: ClipboardList, label: 'Cadastros' },
]

export function AppShell() {
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const isLg = useSyncExternalStore(subscribeLg, getIsLg, () => false)
  const email = getUserEmail()
  const role = getUserRoles()[0]

  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!mobileOpen) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [mobileOpen])

  function handleLogout() {
    limparCacheUsuario()
    clearToken()
    navigate('/login')
  }

  function closeMobile() {
    setMobileOpen(false)
  }

  return (
    <div className="flex min-h-screen min-h-[100dvh]">
      {/* Barra superior — mobile */}
      <header
        className="lg:hidden fixed top-0 inset-x-0 z-50 flex items-center gap-3 px-4 h-14
          border-b border-white/10 bg-bg-elevated/95 backdrop-blur-xl"
      >
        <button
          type="button"
          onClick={() => setMobileOpen(true)}
          className="flex size-10 items-center justify-center rounded-xl text-[#8b9cb3] hover:text-white hover:bg-white/5 transition-colors"
          aria-label="Abrir menu"
        >
          <Menu className="size-5" />
        </button>
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-mint/15 border border-mint/30">
            <Activity className="size-4 text-mint" strokeWidth={2.5} />
          </div>
          <div className="min-w-0">
            <p className="font-bold text-sm tracking-tight leading-none truncate">{FARMACIA_NOME_PRINCIPAL}</p>
            <p className="text-[10px] text-mint font-medium tracking-widest uppercase truncate">
              {FARMACIA_NOME_MARCA}
            </p>
          </div>
        </div>
      </header>

      {/* Overlay */}
      {mobileOpen && (
        <button
          type="button"
          className="lg:hidden fixed inset-0 z-40 bg-black/60 backdrop-blur-sm"
          onClick={closeMobile}
          aria-label="Fechar menu"
        />
      )}

      <aside
        aria-label="Menu principal"
        aria-hidden={!isLg && !mobileOpen}
        className={`fixed inset-y-0 left-0 z-50 flex w-[min(280px,88vw)] max-w-[260px] flex-col
          border-r border-white/10 bg-bg-elevated/95 backdrop-blur-xl
          transition-transform duration-300 ease-out
          lg:translate-x-0 lg:z-40
          ${mobileOpen
            ? 'translate-x-0'
            : '-translate-x-full max-lg:invisible max-lg:pointer-events-none lg:translate-x-0'
          }`}
      >
        <div className="flex items-center gap-3 px-5 py-5 border-b border-white/10 lg:px-6 lg:py-6">
          <div className="flex size-10 items-center justify-center rounded-xl bg-mint/15 border border-mint/30">
            <Activity className="size-5 text-mint" strokeWidth={2.5} />
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-bold text-lg tracking-tight leading-none">{FARMACIA_NOME_PRINCIPAL}</p>
            <p className="text-xs text-mint font-medium tracking-widest uppercase mt-0.5">
              {FARMACIA_NOME_MARCA}
            </p>
          </div>
          <button
            type="button"
            onClick={closeMobile}
            className="lg:hidden flex size-9 items-center justify-center rounded-xl text-[#8b9cb3] hover:text-white hover:bg-white/5"
            aria-label="Fechar menu"
          >
            <X className="size-5" />
          </button>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto overscroll-contain">
          {navItems.map(({ to, icon: Icon, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              onClick={closeMobile}
              className={({ isActive }) =>
                `group flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200
                ${isActive
                  ? 'bg-mint/15 text-mint border border-mint/20'
                  : 'text-[#8b9cb3] hover:text-white hover:bg-white/5'
                }`
              }
            >
              <Icon className="size-[18px] shrink-0" strokeWidth={2} />
              <span className="flex-1">{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10">
          <div className="glass rounded-xl p-3 mb-3">
            <p className="text-xs text-[#8b9cb3] truncate">{email}</p>
            {role && (
              <p className="text-xs text-mint font-medium mt-0.5">{roleLabel(role)}</p>
            )}
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-2 px-3 py-2 text-sm text-[#8b9cb3] hover:text-coral rounded-xl hover:bg-coral/5 transition-colors"
          >
            <LogOut className="size-4" />
            Sair
          </button>
        </div>
      </aside>

      <main className="flex-1 min-w-0 min-h-screen min-h-[100dvh] overflow-x-hidden pt-14 lg:pt-0 lg:ml-[260px]">
        <Outlet />
      </main>
    </div>
  )
}
