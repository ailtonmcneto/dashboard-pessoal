import { useState } from 'react'
import { Link } from 'react-router'
import { Card, Empty, Loaded } from '../components/ui'
import { formatClock, WEEKDAYS } from '../lib/format'
import { useSchedule } from './api'

/** 1 = segunda … 7 = domingo (ISO), igual à API. */
const isoWeekday = (d: Date) => ((d.getDay() + 6) % 7) + 1

export function HorarioPage() {
  const schedule = useSchedule()
  const [today] = useState(() => isoWeekday(new Date()))

  return (
    <Loaded query={schedule}>
      {(slots) => {
        if (slots.length === 0) {
          return (
            <Card>
              <Empty>
                Nenhuma aula no semestre atual. Cadastre os horários dentro de cada{' '}
                <Link to="/estudos" className="text-accent hover:underline">
                  disciplina
                </Link>
                .
              </Empty>
            </Card>
          )
        }
        // segunda a sexta sempre; sábado e domingo só se tiver aula
        const days = [1, 2, 3, 4, 5, 6, 7].filter((d) => d <= 5 || slots.some((s) => s.weekday === d))
        return (
          <div className="grid gap-3 sm:grid-cols-3 lg:grid-cols-[repeat(auto-fit,minmax(140px,1fr))]">
            {days.map((d) => (
              <section
                key={d}
                aria-label={WEEKDAYS[d - 1]}
                className={`rounded-xl border bg-surface p-3 ${d === today ? 'border-accent' : 'border-line'}`}
              >
                <h3 className="mb-2 text-sm font-semibold">
                  {WEEKDAYS[d - 1]}
                  {d === today && <span className="ml-1 text-xs font-normal text-accent">hoje</span>}
                </h3>
                <ul className="space-y-2">
                  {slots
                    .filter((s) => s.weekday === d)
                    .map((s) => (
                      <li key={s.id}>
                        <Link
                          to={`/estudos/disciplinas/${s.courseId}`}
                          className="block rounded-lg bg-canvas p-2 text-sm hover:bg-accent-soft"
                          style={{ boxShadow: `inset 3px 0 0 ${s.color}` }}
                        >
                          <p className="pl-1 text-xs tabular-nums text-muted">
                            {formatClock(s.startTime)}–{formatClock(s.endTime)}
                          </p>
                          <p className="pl-1 font-medium leading-tight">{s.courseName}</p>
                          {s.room && <p className="pl-1 text-xs text-muted">{s.room}</p>}
                        </Link>
                      </li>
                    ))}
                  {!slots.some((s) => s.weekday === d) && <li className="text-xs text-muted">Livre</li>}
                </ul>
              </section>
            ))}
          </div>
        )
      }}
    </Loaded>
  )
}
