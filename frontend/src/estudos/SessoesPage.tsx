import { Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import { BarList, ColumnChart } from '../components/charts'
import {
  Button,
  Card,
  Empty,
  ErrorText,
  Field,
  IconButton,
  Input,
  Loaded,
  SectionTitle,
  Select,
  Textarea,
} from '../components/ui'
import { formData } from '../lib/forms'
import { dayKey, formatMinutes, formatTime, formatWeekdayDate, parseLocalDate, todayIso } from '../lib/format'
import {
  useCreateSession,
  useDeleteSession,
  useResources,
  useStudySessions,
  useStudySummary,
  useTermCourses,
  useTerms,
  useTopics,
} from './api'
import { ColorDot } from './parts'

const dayLabel = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' })

/** Últimos 7 dias inteiros (hoje incluído), no fuso do navegador. */
function lastWeek() {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() - 6)
  const end = new Date(start)
  end.setDate(end.getDate() + 7)
  return { from: start.toISOString(), to: end.toISOString() }
}

function NewSessionForm() {
  const create = useCreateSession()
  const terms = useTerms()
  const current = terms.data?.find((t) => t.current)
  const courses = useTermCourses(current?.id ?? null)
  const resources = useResources()
  const topics = useTopics()
  const [subject, setSubject] = useState('')
  const errors = create.error instanceof ApiError ? create.error.errors : {}

  // assuntos do que foi escolhido (ou todos, sem vínculo); o assunto já liga a sessão à disciplina/curso dele
  const [subjectKind, subjectId] = subject.split(':')
  const subjectTopics = (topics.data ?? []).filter((t) =>
    subjectKind === 'c' ? t.courseId === subjectId : subjectKind === 'r' ? t.resourceId === subjectId : true,
  )

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const minutes = f.number('minutes') ?? 0
    // "terminei agora" por padrão: começo = fim − duração
    const end = new Date(`${f.required('date')}T${f.text('endTime') ?? '00:00'}`)
    const startedAt = new Date(end.getTime() - minutes * 60_000)
    const [kind, id] = (f.text('subject') ?? ':').split(':')
    create.mutate(
      {
        startedAt: startedAt.toISOString(),
        minutes,
        courseId: kind === 'c' ? id : null,
        resourceId: kind === 'r' ? id : null,
        topicId: f.text('topicId'),
        notes: f.text('notes'),
      },
      {
        onSuccess: () => {
          form.reset()
          setSubject('')
        },
      },
    )
  }

  const [nowTime] = useState(() => {
    const now = new Date()
    return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  })

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="O que estudou">
        <Select name="subject" value={subject} onChange={(e) => setSubject(e.target.value)}>
          <option value="">Sem vínculo</option>
          {(courses.data ?? []).length > 0 && (
            <optgroup label="Disciplinas">
              {courses.data!.map((c) => (
                <option key={c.id} value={`c:${c.id}`}>
                  {c.name}
                </option>
              ))}
            </optgroup>
          )}
          {(resources.data ?? []).length > 0 && (
            <optgroup label="Cursos e livros">
              {resources.data!.map((r) => (
                <option key={r.id} value={`r:${r.id}`}>
                  {r.title}
                </option>
              ))}
            </optgroup>
          )}
        </Select>
      </Field>
      {subjectTopics.length > 0 && (
        <Field label="Assunto">
          <Select name="topicId" key={subject} defaultValue="">
            <option value="">Nenhum em especial</option>
            {subjectTopics.map((t) => (
              <option key={t.id} value={t.id}>
                {t.title}
              </option>
            ))}
          </Select>
        </Field>
      )}
      <div className="grid grid-cols-2 gap-3">
        <Field label="Minutos" error={errors.minutes}>
          <Input name="minutes" type="number" min="1" max="1440" defaultValue="50" required />
        </Field>
        <Field label="Terminou às">
          <Input name="endTime" type="time" defaultValue={nowTime} required />
        </Field>
      </div>
      <Field label="Dia">
        <Input name="date" type="date" defaultValue={todayIso()} max={todayIso()} required />
      </Field>
      <Field label="Anotações">
        <Textarea name="notes" maxLength={1000} rows={2} />
      </Field>
      <ErrorText error={Object.keys(errors).length ? null : create.error} />
      <Button type="submit" disabled={create.isPending}>
        Registrar
      </Button>
    </form>
  )
}

export function SessoesPage() {
  const [range] = useState(lastWeek)
  const summary = useStudySummary(range.from, range.to)
  const sessions = useStudySessions(range.from, range.to)
  const remove = useDeleteSession()

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <div className="space-y-6">
        <Loaded query={summary}>
          {(s) => (
            <>
              <div className="grid grid-cols-2 gap-4">
                <Card>
                  <p className="text-sm text-muted">Últimos 7 dias</p>
                  <p className="text-3xl font-bold tabular-nums">{formatMinutes(s.totalMinutes)}</p>
                </Card>
                <Card>
                  <p className="text-sm text-muted">Média por dia</p>
                  <p className="text-3xl font-bold tabular-nums">{formatMinutes(Math.round(s.totalMinutes / 7))}</p>
                </Card>
              </div>
              <Card>
                <SectionTitle>Minutos por dia</SectionTitle>
                <ColumnChart
                  caption="Minutos estudados por dia nos últimos 7 dias"
                  format={formatMinutes}
                  data={s.byDay.map((d) => ({
                    key: d.date,
                    label: dayLabel.format(parseLocalDate(d.date)).replace('.', ''),
                    value: d.minutes,
                  }))}
                />
              </Card>
              <Card>
                <SectionTitle>Por disciplina ou curso</SectionTitle>
                {s.bySubject.length === 0 ? (
                  <Empty>Nenhum estudo registrado na semana.</Empty>
                ) : (
                  <BarList
                    caption="Minutos estudados por disciplina ou curso nos últimos 7 dias"
                    format={formatMinutes}
                    data={s.bySubject.map((b) => ({
                      key: b.courseId ?? b.resourceId ?? 'none',
                      label: b.label,
                      value: b.minutes,
                    }))}
                    marker={(d) => {
                      const color = s.bySubject.find((b) => (b.courseId ?? b.resourceId ?? 'none') === d.key)?.color
                      return color ? <ColorDot color={color} /> : null
                    }}
                  />
                )}
              </Card>
            </>
          )}
        </Loaded>
        <Card>
          <SectionTitle>Registros</SectionTitle>
          <Loaded query={sessions}>
            {(data) =>
              data.length === 0 ? (
                <Empty>Nenhuma sessão na semana.</Empty>
              ) : (
                <ul className="divide-y divide-line">
                  {data.map((x) => (
                    <li key={x.id} className="flex items-center gap-3 py-2 text-sm">
                      <span className="w-28 shrink-0 text-muted">
                        {formatWeekdayDate(dayKey(x.startedAt))} {formatTime(x.startedAt)}
                      </span>
                      <span className="min-w-0 flex-1 truncate">{x.label}</span>
                      <span className="tabular-nums">{formatMinutes(x.minutes)}</span>
                      <IconButton label="Excluir sessão" onClick={() => remove.mutate(x.id)}>
                        <Trash2 size={14} />
                      </IconButton>
                    </li>
                  ))}
                </ul>
              )
            }
          </Loaded>
        </Card>
      </div>
      <Card className="self-start">
        <SectionTitle>Registrar estudo</SectionTitle>
        <NewSessionForm />
      </Card>
    </div>
  )
}
