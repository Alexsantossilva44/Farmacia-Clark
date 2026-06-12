import { type ButtonHTMLAttributes, forwardRef } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  loading?: boolean
}

const variants: Record<Variant, string> = {
  primary:
    'bg-mint text-bg-deep hover:bg-mint/90 font-semibold glow-mint disabled:opacity-50',
  secondary:
    'glass text-white hover:border-mint/40 hover:bg-white/[0.06]',
  ghost: 'text-text-muted hover:text-white hover:bg-white/5',
  danger: 'bg-coral/15 text-coral border border-coral/30 hover:bg-coral/25',
}

const sizes: Record<Size, string> = {
  sm: 'px-3 py-1.5 text-sm rounded-lg',
  md: 'px-4 py-2.5 text-sm rounded-xl',
  lg: 'px-6 py-3 text-base rounded-xl',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', loading, className = '', children, disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 transition-all duration-200 active:scale-[0.98] ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    >
      {loading && (
        <span className="size-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      )}
      {children}
    </button>
  ),
)

Button.displayName = 'Button'
