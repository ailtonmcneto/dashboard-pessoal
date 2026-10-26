import { Archive, ArchiveRestore, CalendarDays, Flame, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import type { Habit, HabitSchedule } from '../api/types'
import { Button, Card, Empty, ErrorText, Field, IconButton, Input, Loaded, SectionTitle } from '../components/ui'
import { COURSE_COLORS } from '../estudos/labels'
import { formData } from '../lib/forms'
import { addDaysIso, formatDate, parseLocalDate, todayIso } from '../lib/format'
import { useCreateHabit, useDeleteHabit, useHabitHistory, useHabits, useUpdateHabit } from './api'
import { DayToggle } from './HabitToggle'
import { scheduleLabel, WEEKDAYS_SHORT } from './labels'

const dayShort = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' })

/** Calendário das últimas 12 semanas: colunas = semanas, linhas = seg…dom. */
function Heatmap({ habit }: { habit: Habit }) {
  const today = todayIso()
  const todayDate = parseLocalDate(today)
  const mondayOffset = (todayDate.getDay() + 6) % 7
  const firstMonday = addDaysIso(today, -mondayOffset - 7 * 11)
  const history = useHabitHistory(habit.id, firstMonday, today)
  const done = new Set(history.data ?? [])

  return (
    <div className="mt-3 overflow-x-auto">
      <div className="inline-grid grid-flow-col grid-rows-7 gap-0.5" role="img" aria-label={`${habit.name}: ${done.size} dias cumpridos nas últimas 12 semanas`}>
        {Array.from({ length: 12 * 7 }, (_, i) => {
          const date = addDaysIso(firstMonday, i)
          const future = date > today
          const before = date < habit.startsOn
          return (
            <span
              key={date}
              title={`${formatDate(date)}${done.has(date) ? ' — feito' : ''}`}
              className={`size-3 rounded-sm ${
                future ? 'bg-transparent' : before ? 'bg-canvas opacity-40' : done.has(date) ? 'bg-chart' : 'bg-canvas'
              }`}
            />
          )
        })}
      </div>
      <p className="mt-1 text-xs text-muted">Últimas 12 semanas · {done.size} dias cumpridos</p>
    </div>
  )
}

function HabitRow({ habit }: { habit: Habit }) {
  const [open, setOpen] = useState(false)
  const update = useUpdateHabit()
  const remove = useDeleteHabit()
  const unit = habit.streakUnit === 'SEMANAS' ? (habit.currentStreak === 1 ? 'semana' : 'semanas') : habit.currentStreak === 1 ? 'dia' : 'dias'

  return (
    <li className={`py-3 ${habit.archived ? 'opacity-60' : ''}`}>
      <div className="flex flex-wrap items-center gap-3">
        <span className="h-8 w-1 shrink-0 rounded-full" style={{ background: habit.color }} aria-hidden="true" />
        <div className="min-w-40 flex-1">
          <p className="font-medium">{habit.name}</p>
          <p className="text-xs text-muted">
            {scheduleLabel(habit)} · {habit.doneThisWeek}/{habit.targetThisWeek} nesta semana · {habit.rate30}% em 30 dias
          </p>
        </div>
        <span
          className="inline-flex items-center gap-1 text-sm tabular-nums"
          title={`Recorde: ${habit.bestStreak}`}
        >
          <Flame size={15} className={habit.currentStreak > 0 ? 'text-warning' : 'text-muted'} aria-hidden="true" />
          {habit.currentStreak} {unit}
        </span>
        <div className="flex gap-1">
          {habit.lastDays.map((d) => (
            <div key={d.date} className="flex flex-col items-center gap-0.5">
              <span className="text-[10px] text-muted">{dayShort.format(parseLocalDate(d.date)).replace('.', '')}</span>
              <DayToggle habit={habit} date={d.date} due={d.due} done={d.done} />
            </div>
          ))}
        </div>
        <div className="flex">
          <IconButton label={`Calendário de ${habit.name}`} onClick={() => setOpen((o) => !o)} aria-expanded={open}>
            <CalendarDays size={15} />
          </IconButton>
          <IconButton
            label={habit.archived ? `Desarquivar ${habit.name}` : `Arquivar ${habit.name}`}
            onClick={() =>
              update.mutate({
                id: habit.id,
                name: habit.name,
                schedule: habit.schedule,
                weekdays: habit.weekdays,
                timesPerWeek: habit.timesPerWeek,
                startsOn: habit.startsOn,
                color: habit.color,
                archived: !habit.archived,
              })
            }
          >
            {habit.archived ? <ArchiveRestore size={15} /> : <Archive size={15} />}
          </IconButton>
          <IconButton
            label={`Apagar ${habit.name}`}
            onClick={() => confirm(`Apagar “${habit.name}” e todo o histórico?`) && remove.mutate(habit.id)}
          >
            <Trash2 size={15} />
          </IconButton>
        </div>
      </div>
      {open && <Heatmap habit={habit} />}
    </li>
  )
}

function HabitForm() {
  const create = useCreateHabit()
  const [schedule, setSchedule] = useState<HabitSchedule>('DIARIO')
  const [weekdays, setWeekdays] = useState<number[]>([1, 3, 5])
  const [color, setColor] = useState(COURSE_COLORS[0])
  const errors = create.error instanceof ApiError ? create.error.errors : {}

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    create.mutate(
      {
        name: f.required('name'),
        schedule,
        weekdays: schedule === 'DIAS_SEMANA' ? weekdays : null,
        timesPerWeek: schedule === 'X_POR_SEMANA' ? f.number('timesPerWeek') : null,
        startsOn: f.text('startsOn'),
        color,
      },
      { onSuccess: () => form.reset() },
    )
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Hábito" error={errors.name}>
        <Input name="name" required maxLength={60} placeholder="Ex.: ler 20 minutos" />
      </Field>
      <fieldset>
        <legend className="mb-1 text-sm font-medium">Frequência</legend>
        <div className="grid grid-cols-3 gap-1 rounded-lg bg-canvas p-1" role="radiogroup">
          {(
            [
              ['DIARIO', 'Todo dia'],
              ['DIAS_SEMANA', 'Dias fixos'],
              ['X_POR_SEMANA', 'X por semana'],
            ] as const
          ).map(([v, l]) => (
            <button
              key={v}
              type="button"
              role="radio"
              aria-checked={schedule === v}
              onClick={() => setSchedule(v)}
              className={`rounded-md px-1 py-1.5 text-xs font-medium ${schedule === v ? 'bg-surface shadow-sm' : 'text-muted'}`}
            >
              {l}
            </button>
          ))}
        </div>
      </fieldset>
      {schedule === 'DIAS_SEMANA' && (
        <fieldset>
          <legend className="mb-1 text-sm font-medium">Quais dias</legend>
          <div className="flex flex-wrap gap-1">
            {WEEKDAYS_SHORT.map((d, i) => {
              const on = weekdays.includes(i + 1)
              return (
                <button
                  key={d}
                  type="button"
                  aria-pressed={on}
                  onClick={() => setWeekdays(on ? weekdays.filter((x) => x !== i + 1) : [...weekdays, i + 1])}
                  className={`rounded-md border px-2 py-1 text-xs ${on ? 'border-accent bg-accent-soft' : 'border-line'}`}
                >
                  {d}
                </button>
              )
            })}
          </div>
        </fieldset>
      )}
      {schedule === 'X_POR_SEMANA' && (
        <Field label="Vezes por semana" error={errors.timesPerWeek}>
          <Input name="timesPerWeek" type="number" min="1" max="7" defaultValue="3" required />
        </Field>
      )}
      <Field label="Começa em" error={errors.startsOn}>
        <Input name="startsOn" type="date" defaultValue={todayIso()} max={todayIso()} />
      </Field>
      <fieldset>
        <legend className="mb-1 text-sm font-medium">Cor</legend>
        <div className="flex flex-wrap gap-2">
          {COURSE_COLORS.map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => setColor(c)}
              aria-label={`Cor ${c}`}
              aria-pressed={color === c}
              className={`size-6 rounded-full ring-offset-2 ${color === c ? 'ring-2 ring-ink' : ''}`}
              style={{ background: c }}
            />
          ))}
        </div>
      </fieldset>
      <ErrorText error={Object.keys(errors).length ? null : create.error} />
      <Button type="submit" disabled={create.isPending}>
        Criar hábito
      </Button>
    </form>
  )
}

export function HabitosPage() {
  const habits = useHabits()
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
      <Card>
        <SectionTitle>Hábitos</SectionTitle>
        <p className="mb-2 text-xs text-muted">
          Clique num dia para marcar ou desmarcar. Dias tracejados não fazem parte da frequência do hábito. Hoje
          ainda não feito não quebra a sequência.
        </p>
        <Loaded query={habits}>
          {(list) =>
            list.length === 0 ? (
              <Empty>Nenhum hábito ainda.</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {list.map((h) => (
                  <HabitRow key={h.id} habit={h} />
                ))}
              </ul>
            )
          }
        </Loaded>
      </Card>
      <Card className="self-start">
        <SectionTitle>Novo hábito</SectionTitle>
        <HabitForm />
      </Card>
    </div>
  )
}
