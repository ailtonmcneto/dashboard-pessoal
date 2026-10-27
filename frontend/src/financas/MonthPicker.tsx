import { ChevronLeft, ChevronRight } from 'lucide-react'
import { addMonths, currentMonth, formatMonth } from '../lib/format'

export function MonthPicker({ value, onChange }: { value: string; onChange: (m: string) => void }) {
  return (
    <div className="inline-flex items-center gap-1 rounded-lg border border-line bg-surface p-1">
      <button
        type="button"
        onClick={() => onChange(addMonths(value, -1))}
        className="rounded-md p-1 text-muted hover:bg-canvas hover:text-ink"
        aria-label="Mês anterior"
      >
        <ChevronLeft size={16} />
      </button>
      <span className="min-w-36 text-center text-sm font-medium" aria-live="polite">
        {formatMonth(value)}
      </span>
      <button
        type="button"
        onClick={() => onChange(addMonths(value, 1))}
        className="rounded-md p-1 text-muted hover:bg-canvas hover:text-ink"
        aria-label="Próximo mês"
      >
        <ChevronRight size={16} />
      </button>
      {value !== currentMonth() && (
        <button
          type="button"
          onClick={() => onChange(currentMonth())}
          className="rounded-md px-2 py-1 text-xs text-accent hover:bg-canvas"
        >
          Hoje
        </button>
      )}
    </div>
  )
}
