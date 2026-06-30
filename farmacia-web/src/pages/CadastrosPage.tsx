import { useSearchParams } from 'react-router-dom'
import { ClipboardList, Pill, Users, Factory, Tags, Stethoscope, Truck } from 'lucide-react'
import type { CadastroTab } from '@/lib/cadastro-options'
import { TAB_LABELS } from '@/lib/cadastro-options'
import { MedicamentosCadastroTab } from './cadastros/MedicamentosCadastroTab'
import { ClientesCadastroTab } from './cadastros/ClientesCadastroTab'
import { CatalogoSimpleTab } from './cadastros/CatalogoSimpleTab'
import { FornecedoresCadastroTab } from './cadastros/FornecedoresCadastroTab'

const TABS: { id: CadastroTab; icon: typeof Pill }[] = [
  { id: 'clientes', icon: Users },
  { id: 'fabricantes', icon: Factory },
  { id: 'fornecedores', icon: Truck },
  { id: 'categorias', icon: Tags },
  { id: 'prescritores', icon: Stethoscope },
  { id: 'medicamentos', icon: Pill },
]

export function CadastrosPage() {
  const [params, setParams] = useSearchParams()
  const aba = (params.get('aba') as CadastroTab) || 'clientes'
  const tabAtiva = TAB_LABELS[aba] ? aba : 'clientes'

  function setTab(id: CadastroTab) {
    setParams({ aba: id })
  }

  return (
    <div className="page-shell">
      <header className="page-header-band">
        <div className="flex items-center gap-2 text-amber mb-1.5">
          <ClipboardList className="size-4" />
          <span className="text-xs font-semibold uppercase tracking-widest">Administração</span>
        </div>
        <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight">Cadastros</h1>
        <p className="text-[#8b9cb3] mt-1 text-sm">
          Medicamentos, clientes e dados auxiliares do catálogo
        </p>

        <nav className="page-tabs">
          {TABS.map(({ id, icon: Icon }) => (
            <button
              key={id}
              type="button"
              onClick={() => setTab(id)}
              style={{
                borderColor: tabAtiva === id
                  ? 'rgba(45, 212, 168, 0.30)'
                  : 'rgba(255, 255, 255, 0.12)',
              }}
              className={`inline-flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-all border
                ${tabAtiva === id
                  ? 'bg-mint/15 text-mint'
                  : 'text-[#8b9cb3] hover:text-white hover:bg-white/5'
                }`}
            >
              <Icon className="size-4" />
              {TAB_LABELS[id]}
            </button>
          ))}
        </nav>
      </header>

      <div
        className={`flex-1 min-h-0 w-full px-4 sm:px-6 lg:px-10 py-4 sm:py-6 ${
          tabAtiva === 'medicamentos' || tabAtiva === 'fabricantes' || tabAtiva === 'fornecedores' || tabAtiva === 'categorias' || tabAtiva === 'prescritores' || tabAtiva === 'clientes'
            ? `overflow-hidden flex flex-col ${
                tabAtiva === 'clientes'
                  ? 'max-w-7xl xl:max-w-[1800px]'
                  : 'max-w-7xl xl:max-w-[1600px]'
              }`
            : 'overflow-y-auto max-w-7xl'
        }`}
      >
        {tabAtiva === 'medicamentos' && (
          <div className="flex-1 min-h-0">
            <MedicamentosCadastroTab />
          </div>
        )}
        {tabAtiva === 'clientes' && (
          <div className="flex-1 min-h-0">
            <ClientesCadastroTab />
          </div>
        )}
        {tabAtiva === 'fornecedores' && (
          <div className="flex-1 min-h-0">
            <FornecedoresCadastroTab />
          </div>
        )}
        {(tabAtiva === 'fabricantes' || tabAtiva === 'categorias' || tabAtiva === 'prescritores') && (
          <div className="flex-1 min-h-0">
            <CatalogoSimpleTab kind={tabAtiva} />
          </div>
        )}
      </div>
    </div>
  )
}
