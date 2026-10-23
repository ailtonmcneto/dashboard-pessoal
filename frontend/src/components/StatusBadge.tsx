import { AlertTriangle, CheckCircle2, Circle, XCircle } from 'lucide-react'

export type Tone = 'neutral' | 'good' | 'warning' | 'critical'

const ICON = { neutral: Circle, good: CheckCircle2, warning: AlertTriangle, critical: XCircle }
const COLOR = { neutral: 'text-muted', good: 'text-good', warning: 'text-warning', critical: 'text-critical' }

/** Status sempre com ícone + rótulo; o texto fica na cor de texto, só o ícone leva a cor. */
export function StatusBadge({ tone, label }: { tone: Tone; label: string }) {
  const Icon = ICON[tone]
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-line bg-surface px-2 py-0.5 text-xs font-medium">
      <Icon size={13} className={COLOR[tone]} aria-hidden="true" />
      {label}
    </span>
  )
}
