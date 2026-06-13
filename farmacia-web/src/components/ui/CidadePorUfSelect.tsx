import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { ChevronDown, Search } from 'lucide-react'
import {
  filtrarOpcoesCidades,
  formatarNomeCidadeExibicao,
  listarOpcoesCidadesPorUf,
  normalizarNomeCidade,
} from '@/lib/cidades-uf-br'

interface CidadePorUfSelectProps {
  label?: string
  uf: string
  value: string
  onChange: (cidade: string) => void
  placeholder?: string
  className?: string
  disabled?: boolean
  error?: string
}

export function CidadePorUfSelect({
  label = 'Cidade',
  uf,
  value,
  onChange,
  placeholder = 'Selecione a cidade…',
  className = '',
  disabled = false,
  error,
}: CidadePorUfSelectProps) {
  const [open, setOpen] = useState(false)
  const [filtro, setFiltro] = useState('')
  const [menuStyle, setMenuStyle] = useState({ top: 0, left: 0, width: 0 })
  const rootRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)

  const opcoes = useMemo(() => listarOpcoesCidadesPorUf(uf), [uf])
  const filtradas = useMemo(() => filtrarOpcoesCidades(opcoes, filtro), [opcoes, filtro])

  const valorNormalizado = normalizarNomeCidade(value)
  const selected = opcoes.find((o) => o.value === valorNormalizado)
  const bloqueado = disabled || !uf

  useEffect(() => {
    if (!open) return

    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node
      if (rootRef.current?.contains(target) || menuRef.current?.contains(target)) return
      setOpen(false)
    }

    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }

    document.addEventListener('click', handleClickOutside)
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('click', handleClickOutside)
      document.removeEventListener('keydown', handleKey)
    }
  }, [open])

  useEffect(() => {
    setOpen(false)
    setFiltro('')
  }, [uf])

  function updateMenuPosition() {
    const rect = triggerRef.current?.getBoundingClientRect()
    if (!rect) return
    setMenuStyle({
      top: rect.bottom + 6,
      left: rect.left,
      width: Math.max(rect.width, 280),
    })
  }

  function toggleOpen() {
    if (bloqueado) return
    if (!open) {
      updateMenuPosition()
      setFiltro('')
    }
    setOpen((prev) => !prev)
  }

  function handleSelect(chave: string) {
    onChange(formatarNomeCidadeExibicao(chave))
    setOpen(false)
    setFiltro('')
  }

  useEffect(() => {
    if (open) {
      requestAnimationFrame(() => searchRef.current?.focus())
    }
  }, [open])

  const menu = open
    ? createPortal(
        <div
          ref={menuRef}
          style={{
            position: 'fixed',
            top: menuStyle.top,
            left: menuStyle.left,
            width: menuStyle.width,
          }}
          className="z-[200] rounded-xl border border-white/15 bg-bg-elevated shadow-2xl overflow-hidden"
        >
          <div className="p-2 border-b border-white/10">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-3.5 text-[#8b9cb3]" />
              <input
                ref={searchRef}
                type="search"
                aria-label="Filtrar cidades"
                value={filtro}
                onChange={(e) => setFiltro(e.target.value)}
                placeholder="Filtrar municípios…"
                className="w-full pl-8 pr-3 py-2 rounded-lg bg-white/[0.04] text-sm text-white placeholder:text-white/30
                  focus:outline-none focus:ring-2 focus:ring-mint/30"
              />
            </div>
          </div>
          <ul role="listbox" className="max-h-56 overflow-y-auto py-1 overscroll-contain">
            {filtradas.length === 0 ? (
              <li className="px-3 py-4 text-sm text-[#8b9cb3] text-center">
                Nenhum município encontrado
              </li>
            ) : (
              filtradas.map((opt) => (
                <li key={opt.value} role="option" aria-selected={opt.value === valorNormalizado}>
                  <button
                    type="button"
                    onMouseDown={(e) => {
                      e.preventDefault()
                      handleSelect(opt.value)
                    }}
                    className={`w-full text-left px-3 py-2.5 text-sm transition-colors
                      ${opt.value === valorNormalizado
                        ? 'bg-mint/15 text-mint'
                        : 'text-[#f0f4f8] hover:bg-white/8'
                      }`}
                  >
                    {opt.label}
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>,
        document.body,
      )
    : null

  const placeholderAtual = !uf ? 'Selecione a UF primeiro' : placeholder

  return (
    <div className="min-w-0 space-y-1.5" ref={rootRef}>
      {label && (
        <label className="block text-xs sm:text-sm font-medium text-[#8b9cb3]">{label}</label>
      )}
      <button
        ref={triggerRef}
        type="button"
        aria-label={label}
        disabled={bloqueado}
        onClick={toggleOpen}
        className={`w-full flex items-center justify-between gap-2 px-3 sm:px-4 rounded-xl glass text-left
          focus:outline-none focus:ring-2 focus:ring-mint/30 disabled:opacity-50
          ${open ? 'ring-2 ring-mint/30 border-mint/20' : ''}
          ${error ? 'border-coral/50 ring-coral/20' : ''} ${className}`}
      >
        <span className={`truncate text-sm ${selected || value ? 'text-white' : 'text-white/35'}`}>
          {selected?.label ?? (value || placeholderAtual)}
        </span>
        <ChevronDown
          className={`size-4 text-[#8b9cb3] shrink-0 transition-transform ${open ? 'rotate-180' : ''}`}
        />
      </button>
      {error && <p className="text-xs text-coral leading-snug">{error}</p>}
      {menu}
    </div>
  )
}
