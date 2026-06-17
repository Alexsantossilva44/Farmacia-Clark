import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { ChevronDown, Search } from 'lucide-react'

export interface SelectOption {
  value: string
  label: string
  sublabel?: string
}

interface SelectProps {
  label?: string
  value: string
  onChange: (value: string) => void
  options: SelectOption[]
  placeholder?: string
  className?: string
  loading?: boolean
  disabled?: boolean
  error?: string
  onBlur?: () => void
  searchable?: boolean
}

export function Select({
  label,
  value,
  onChange,
  options,
  placeholder = 'Selecione…',
  className = '',
  loading = false,
  disabled = false,
  error,
  onBlur,
  searchable = false,
}: SelectProps) {
  const [open, setOpen] = useState(false)
  const [menuStyle, setMenuStyle] = useState({ top: 0, left: 0, width: 0 })
  const [search, setSearch] = useState('')
  const rootRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const focusFromMouseRef = useRef(false)

  const selected = options.find((o) => o.value === value)

  const filteredOptions = searchable && search.trim()
    ? options.filter((o) => o.label.toLowerCase().startsWith(search.trim().toLowerCase()))
    : options

  useEffect(() => {
    if (!open) {
      setSearch('')
      return
    }
    if (searchable) {
      setTimeout(() => searchRef.current?.focus(), 0)
    }

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
  }, [open, searchable])

  function updateMenuPosition() {
    const rect = triggerRef.current?.getBoundingClientRect()
    if (!rect) return
    setMenuStyle({
      top: rect.bottom + 6,
      left: rect.left,
      width: rect.width,
    })
  }

  function toggleOpen() {
    if (disabled || loading) return
    if (!open) updateMenuPosition()
    setOpen((prev) => !prev)
  }

  function handleSelect(optionValue: string) {
    onChange(optionValue)
    setOpen(false)
  }

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
          className="z-[200] rounded-xl border border-white/15 bg-bg-deep/90 backdrop-blur-xl shadow-2xl overflow-hidden"
        >
          {searchable && (
            <div className="px-2 pt-2 pb-1 border-b border-white/10">
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 size-3.5 text-[#8b9cb3]" />
                <input
                  ref={searchRef}
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Escape') { e.stopPropagation(); setOpen(false) }
                    if (e.key === 'Enter' && filteredOptions.length === 1) {
                      handleSelect(filteredOptions[0].value)
                    }
                  }}
                  placeholder="Buscar…"
                  className="w-full pl-7 pr-2 py-1.5 rounded-lg bg-white/[0.06] text-white text-sm placeholder:text-white/30 focus:outline-none focus:ring-1 focus:ring-mint/40"
                />
              </div>
            </div>
          )}
          <ul
            role="listbox"
            className="max-h-48 overflow-y-auto py-1 overscroll-contain"
          >
            {filteredOptions.length === 0 ? (
              <li className="px-3 py-4 text-sm text-[#8b9cb3] text-center">Nenhuma opção encontrada</li>
            ) : (
              filteredOptions.map((opt) => (
                <li key={opt.value} role="option" aria-selected={opt.value === value}>
                  <button
                    type="button"
                    onMouseDown={(e) => {
                      e.preventDefault()
                      handleSelect(opt.value)
                    }}
                    className={`w-full text-left px-3 py-2.5 transition-colors
                      ${opt.value === value
                        ? 'bg-mint/15 text-mint'
                        : 'text-[#f0f4f8] hover:bg-white/8'
                      }`}
                  >
                    <span className="block text-sm font-medium leading-snug">{opt.label}</span>
                    {opt.sublabel && (
                      <span className="block text-xs text-[#8b9cb3] mt-0.5 leading-snug">{opt.sublabel}</span>
                    )}
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>,
        document.body,
      )
    : null

  return (
    <div className="min-w-0 space-y-1.5" ref={rootRef}>
      {label && (
        <label className="block text-xs sm:text-sm font-medium text-[#a8bbd0]">{label}</label>
      )}
      <button
        ref={triggerRef}
        type="button"
        disabled={disabled || loading}
        onClick={toggleOpen}
        onMouseDown={() => { focusFromMouseRef.current = true }}
        onFocus={() => {
          if (focusFromMouseRef.current) { focusFromMouseRef.current = false; return }
          if (!disabled && !loading && !open) { updateMenuPosition(); setOpen(true) }
        }}
        onBlur={onBlur}
        className={`w-full flex items-center justify-between gap-2 px-3 sm:px-4 rounded-xl glass text-left
          focus:outline-none focus:ring-2 focus:ring-mint/30 disabled:opacity-50
          ${open ? 'ring-2 ring-mint/30 border-mint/20' : ''}
          ${error ? 'border-coral/50 ring-coral/20' : ''} ${className}`}
      >
        <span className={`truncate text-sm ${selected ? 'text-white' : 'text-white/55'}`}>
          {loading ? 'Carregando…' : selected?.label ?? placeholder}
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
