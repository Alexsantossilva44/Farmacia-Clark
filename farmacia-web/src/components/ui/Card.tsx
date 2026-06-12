interface CardProps {
  children: React.ReactNode
  className?: string
  glow?: 'mint' | 'coral' | 'none'
  hover?: boolean
  onClick?: () => void
}

export function Card({ children, className = '', glow = 'none', hover, onClick }: CardProps) {
  const glowClass =
    glow === 'mint' ? 'glow-mint' : glow === 'coral' ? 'glow-coral' : ''
  const hoverClass = hover
    ? 'transition-all duration-200 hover:-translate-y-0.5 hover:border-white/20 hover:bg-white/[0.05]'
    : ''

  return (
    <div
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
      className={`glass rounded-2xl p-5 ${glowClass} ${hoverClass} ${className}`}
    >
      {children}
    </div>
  )
}
