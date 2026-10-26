import { MapPin, Trash2 } from 'lucide-react'
import type { ReactNode } from 'react'
import type { EventItem } from '../api/types'
import { dayKey, formatDay, formatTime } from '../lib/format'
import { KIND_LABEL } from './events'

export function EventList({
  events,
  onDelete,
  edit,
}: {
  events: EventItem[]
  onDelete?: (id: string) => void
  edit?: (event: EventItem) => ReactNode
}) {
  if (events.length === 0) return <p className="text-sm text-muted">Nada marcado.</p>

  const byDay = new Map<string, EventItem[]>()
  for (const e of events) {
    const key = dayKey(e.startsAt)
    byDay.set(key, [...(byDay.get(key) ?? []), e])
  }

  return (
    <div className="space-y-5">
      {[...byDay.values()].map((dayEvents) => (
        <div key={dayKey(dayEvents[0].startsAt)}>
          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">
            {formatDay(dayEvents[0].startsAt)}
          </h3>
          <ul className="divide-y divide-line">
            {dayEvents.map((e) => (
              <li key={e.id} className="flex items-start gap-3 py-2">
                <span className="w-24 shrink-0 text-sm tabular-nums text-muted">
                  {formatTime(e.startsAt)}
                  {e.endsAt && `–${formatTime(e.endsAt)}`}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium">{e.title}</p>
                  <p className="flex items-center gap-2 text-xs text-muted">
                    {KIND_LABEL[e.kind]}
                    {e.location && (
                      <span className="flex items-center gap-0.5">
                        <MapPin size={12} /> {e.location}
                      </span>
                    )}
                  </p>
                </div>
                {edit?.(e)}
                {onDelete && (
                  <button
                    onClick={() => onDelete(e.id)}
                    className="text-muted hover:text-danger"
                    aria-label={`Excluir ${e.title}`}
                  >
                    <Trash2 size={16} />
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  )
}
