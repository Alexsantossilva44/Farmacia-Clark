import { type InputHTMLAttributes, forwardRef, useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  /** Exibe ícone de olho para alternar visibilidade (campos type="password") */
  showPasswordToggle?: boolean
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className = '', id, type, showPasswordToggle, ...props }, ref) => {
    const [senhaVisivel, setSenhaVisivel] = useState(false)
    const inputId = id ?? label?.toLowerCase().replace(/\s/g, '-')
    const isPassword = type === 'password'
    const exibirToggle = isPassword && showPasswordToggle !== false
    const inputType = exibirToggle && senhaVisivel ? 'text' : type

    const inputClassName = `w-full px-4 py-3 rounded-xl glass bg-white/[0.03] text-white placeholder:text-white/30
      focus:outline-none focus:ring-2 focus:ring-mint/40 focus:border-mint/30
      transition-all duration-200 ${exibirToggle ? 'pr-11' : ''}
      ${error ? 'border-coral/50 ring-coral/20' : ''} ${className}`

    return (
      <div className="space-y-1.5">
        {label && (
          <label htmlFor={inputId} className="block text-sm font-medium text-[#8b9cb3]">
            {label}
          </label>
        )}
        <div className="relative">
          <input
            ref={ref}
            id={inputId}
            type={inputType}
            className={inputClassName}
            {...props}
          />
          {exibirToggle && (
            <button
              type="button"
              onClick={() => setSenhaVisivel((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-lg text-[#8b9cb3]
                hover:text-white hover:bg-white/5 transition-colors"
              aria-label={senhaVisivel ? 'Ocultar senha' : 'Mostrar senha'}
              tabIndex={-1}
            >
              {senhaVisivel ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          )}
        </div>
        {error && <p className="text-sm text-coral">{error}</p>}
      </div>
    )
  },
)

Input.displayName = 'Input'
