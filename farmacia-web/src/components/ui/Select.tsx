import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { ChevronDown } from 'lucide-react'

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
}: SelectProps) {
  const [open, setOpen] = useState(false)
  const [menuStyle, setMenuStyle] = useState({ top: 0, left: 0, width: 0 })
  const rootRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLUListElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)

  const selected = options.find((o) => o.value === value)

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

    // click (não mousedown) — evita fechar antes do item ser selecionado
    document.addEventListener('click', handleClickOutside)
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('click', handleClickOutside)
      document.removeEventListener('keydown', handleKey)
    }
  }, [open])

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
        <ul
          ref={menuRef}
          role="listbox"
          style={{
            position: 'fixed',
            top: menuStyle.top,
            left: menuStyle.left,
            width: menuStyle.width,
          }}
          className="z-[200] max-h-56 overflow-y-auto rounded-xl border border-white/15 bg-bg-elevated shadow-2xl py-1 overscroll-contain"
        >
          {options.length === 0 ? (
            <li className="px-3 py-4 text-sm text-[#8b9cb3] text-center">Nenhuma opção disponível</li>
          ) : (
            options.map((opt) => (
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
        </ul>,
        document.body,
      )
    : null

  return (
    <div className="min-w-0 space-y-1.5" ref={rootRef}>
      {label && (
        <label className="block text-xs sm:text-sm font-medium text-[#8b9cb3]">{label}</label>
      )}
      <button
        ref={triggerRef}
        type="button"
        disabled={disabled || loading}
        onClick={toggleOpen}
        onBlur={onBlur}
        className={`w-full flex items-center justify-between gap-2 px-3 sm:px-4 rounded-xl glass text-left
          focus:outline-none focus:ring-2 focus:ring-mint/30 disabled:opacity-50
          ${open ? 'ring-2 ring-mint/30 border-mint/20' : ''}
          ${error ? 'border-coral/50 ring-coral/20' : ''} ${className}`}
      >
        <span className={`truncate text-sm ${selected ? 'text-white' : 'text-white/35'}`}>
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
