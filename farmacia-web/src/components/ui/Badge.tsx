import type { BadgeVariant } from '@/lib/format'

const styles: Record<BadgeVariant, string> = {
  mint: 'bg-mint/15 text-mint border-mint/25',
  sky: 'bg-sky/15 text-sky border-sky/25',
  amber: 'bg-amber/15 text-amber border-amber/25',
  coral: 'bg-coral/15 text-coral border-coral/25',
  violet: 'bg-violet/15 text-violet border-violet/25',
}

interface BadgeProps {
  children: React.ReactNode
  variant?: BadgeVariant
  dot?: boolean
  className?: string
}

export function Badge({ children, variant = 'mint', dot, className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 text-xs font-medium rounded-full border ${styles[variant]} ${className}`}
    >
      {dot && <span className="size-1.5 rounded-full bg-current animate-pulse" />}
      {children}
    </span>
  )
}
