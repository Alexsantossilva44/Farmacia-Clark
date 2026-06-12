import { Construction } from 'lucide-react'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'

interface ComingSoonPageProps {
  title: string
  description: string
}

export function ComingSoonPage({ title, description }: ComingSoonPageProps) {
  return (
    <div className="p-8 lg:p-10 max-w-2xl">
      <Card glow="coral" className="text-center py-16">
        <Construction className="size-12 text-amber mx-auto mb-4" />
        <Badge variant="amber" className="mb-4">Em breve</Badge>
        <h1 className="text-2xl font-bold">{title}</h1>
        <p className="text-[#8b9cb3] mt-2 leading-relaxed">{description}</p>
        <p className="text-xs text-white/30 mt-6">
          Use case já existe no backend — REST será exposto na próxima iteração.
        </p>
      </Card>
    </div>
  )
}
