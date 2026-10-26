import { Check } from 'lucide-react'
import type { Habit } from '../api/types'
import { parseLocalDate } from '../lib/format'
import { useCheck, useUncheck } from './api'

const dayFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: 'numeric' })

/** Botão de um dia: marca/desmarca. Dias que o hábito não pede ficam esmaecidos (mas podem ser marcados). */
export function DayToggle({ habit, date, due, done, size = 'md' }: {
  habit: Habit
  date: string
  due: boolean
  done: boolean
  size?: 'md' | 'lg'
}) {
  const check = useCheck()
  const uncheck = useUncheck()
  const busy = check.isPending || uncheck.isPending
  const label = `${habit.name} em ${dayFmt.format(parseLocalDate(date))}: ${done ? 'feito' : 'não feito'}`
  return (
    <button
      type="button"
      aria-pressed={done}
      aria-label={label}
      title={label}
      disabled={busy}
      onClick={() => (done ? uncheck : check).mutate({ id: habit.id, date })}
      className={`flex items-center justify-center rounded-md border transition ${
        size === 'lg' ? 'size-9' : 'size-7'
      } ${
        done
          ? 'border-transparent bg-chart text-white'
          : due
            ? 'border-line bg-surface hover:border-accent'
            : 'border-dashed border-line bg-canvas opacity-60 hover:opacity-100'
      }`}
    >
      {done && <Check size={size === 'lg' ? 18 : 14} strokeWidth={3} />}
    </button>
  )
}
