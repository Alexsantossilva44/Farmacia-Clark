import { type InputHTMLAttributes, forwardRef, useId, useRef } from 'react'
import { CalendarDays } from 'lucide-react'
import {
  dataBrParaIsoSafe,
  dataIsoMaxNascimentoCadastro,
  dataIsoMinNascimentoCadastro,
  dataIsoParaBr,
  maskDataNascimentoBr,
} from '@/lib/validacao-cliente'

interface DataNascimentoInputProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'onChange'> {
  label?: string
  value: string
  onChange: (value: string) => void
  error?: string
}

export const DataNascimentoInput = forwardRef<HTMLInputElement, DataNascimentoInputProps>(function DataNascimentoInput({
  label = 'Data de nascimento',
  value,
  onChange,
  error,
  className = '',
  id,
  disabled,
  readOnly,
  onBlur,
  ...props
}, ref) {
  const geradoId = useId()
  const inputId = id ?? geradoId.replace(/:/g, '')
  const pickerRef = useRef<HTMLInputElement>(null)
  const isoValue = dataBrParaIsoSafe(value)
  const bloqueado = Boolean(disabled || readOnly)

  function handlePickerChange(iso: string) {
    if (!iso) {
      onChange('')
      return
    }
    onChange(dataIsoParaBr(iso))
  }

  const inputClassName = `w-full pl-3 sm:pl-4 pr-11 py-2.5 rounded-xl glass bg-white/[0.03] text-white
    placeholder:text-white/30 font-mono text-sm
    focus:outline-none focus:ring-2 focus:ring-mint/40 focus:border-mint/30
    transition-all duration-200 disabled:opacity-50
    ${error ? 'border-coral/50 ring-coral/20' : ''} ${className}`

  return (
    <div className="min-w-0 space-y-1.5">
      {label && (
        <label htmlFor={inputId} className="block text-xs sm:text-sm font-medium text-[#8b9cb3]">
          {label}
        </label>
      )}
      <div className="relative">
        <input
          {...props}
          ref={ref}
          id={inputId}
          type="text"
          inputMode="numeric"
          autoComplete="bday"
          disabled={disabled}
          readOnly={readOnly}
          value={value}
          placeholder="DD/MM/AAAA"
          className={inputClassName}
          onChange={(e) => onChange(maskDataNascimentoBr(e.target.value))}
          onBlur={onBlur}
        />
        <div className="absolute right-1.5 top-1/2 -translate-y-1/2 size-8">
          <input
            ref={pickerRef}
            type="date"
            tabIndex={-1}
            aria-label="Calendário de data de nascimento"
            disabled={bloqueado}
            min={dataIsoMinNascimentoCadastro()}
            max={dataIsoMaxNascimentoCadastro()}
            value={isoValue}
            onChange={(e) => handlePickerChange(e.target.value)}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer disabled:cursor-not-allowed"
          />
          <span
            className="pointer-events-none absolute inset-0 flex items-center justify-center text-[#8b9cb3]"
            aria-hidden="true"
          >
            <CalendarDays className="size-4" />
          </span>
        </div>
      </div>
      {error && <p className="text-xs text-coral leading-snug">{error}</p>}
    </div>
  )
})
