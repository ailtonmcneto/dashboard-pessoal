import { Link } from 'react-router'
import type { Goal } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import { formatDate } from '../lib/format'
import { AREA, PACE, progressText } from './labels'

/** Barra de progresso com a marca do "esperado até hoje". */
export function GoalBar({ goal }: { goal: Goal }) {
  return (
    <div className="relative">
      <div
        role="meter"
        aria-label={`${goal.title}: ${goal.percent}%`}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={goal.percent}
        className="h-2.5 overflow-hidden rounded-full bg-canvas"
      >
        <div className="h-full rounded-full bg-chart" style={{ width: `${goal.percent}%` }} />
      </div>
      {goal.expectedPercent != null && goal.status === 'ATIVA' && (
        <span
          className="absolute -top-1 h-4.5 w-0.5 rounded bg-ink/60"
          style={{ left: `calc(${goal.expectedPercent}% - 1px)` }}
          title={`Esperado até hoje: ${goal.expectedPercent}%`}
          aria-hidden="true"
        />
      )}
    </div>
  )
}

export function GoalCard({ goal }: { goal: Goal }) {
  const pace = PACE[goal.pace]
  return (
    <Link
      to={`/metas/objetivos/${goal.id}`}
      className={`block rounded-xl border border-line bg-surface p-4 transition hover:border-accent/50 hover:shadow-sm ${
        goal.status !== 'ATIVA' ? 'opacity-70' : ''
      }`}
    >
      <div className="mb-1 flex items-start justify-between gap-2">
        <p className="font-semibold">{goal.title}</p>
        <span className="text-xl font-bold tabular-nums">{goal.percent}%</span>
      </div>
      <p className="mb-3 text-xs text-muted">
        {AREA[goal.area]}
        {goal.dueOn && ` · prazo ${formatDate(goal.dueOn)}`}
      </p>
      <GoalBar goal={goal} />
      <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-muted">
        <StatusBadge tone={pace.tone} label={pace.label} />
        <span>
          {progressText(goal)}
          {goal.expectedPercent != null && goal.status === 'ATIVA' && ` · esperado ${goal.expectedPercent}%`}
        </span>
      </div>
    </Link>
  )
}
