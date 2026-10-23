import { ArrowLeft, Plus, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import type { Assessment, AssessmentKind, Course, CourseStatus } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import { Button, Card, Empty, ErrorText, Field, IconButton, Input, Loaded, SectionTitle, Select } from '../components/ui'
import { formData } from '../lib/forms'
import { formatClock, formatDate, formatGrade, todayIso, WEEKDAYS } from '../lib/format'
import {
  useAddAbsence,
  useAddAssessment,
  useAddSlot,
  useCourse,
  useCourseAbsences,
  useCourseAssessments,
  useCourseSlots,
  useDeleteAbsence,
  useDeleteAssessment,
  useDeleteCourse,
  useDeleteSlot,
  useUpdateAssessment,
  useUpdateCourse,
} from './api'
import { ASSESSMENT_KIND, COURSE_STATUS, GRADE_STATUS } from './labels'
import { neededText } from './grades'
import { AbsenceMeter, ColorDot } from './parts'
import { TopicsSection } from './TopicsSection'

function fieldErrors(error: unknown) {
  return error instanceof ApiError ? error.errors : {}
}

// ---------- Avaliações ----------

function ScoreInput({ assessment }: { assessment: Assessment }) {
  const update = useUpdateAssessment()
  const [value, setValue] = useState(assessment.score?.toString() ?? '')

  function save() {
    const score = value.trim() === '' ? null : Number(value.replace(',', '.'))
    if (score === (assessment.score ?? null)) return
    update.mutate({
      id: assessment.id,
      title: assessment.title,
      kind: assessment.kind,
      dueOn: assessment.dueOn,
      weight: assessment.weight,
      maxScore: assessment.maxScore,
      score,
      topicIds: assessment.topicIds,
    })
  }

  return (
    <div className="flex items-center gap-1">
      <input
        aria-label={`Nota de ${assessment.title}`}
        inputMode="decimal"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onBlur={save}
        onKeyDown={(e) => e.key === 'Enter' && e.currentTarget.blur()}
        placeholder="—"
        className={`w-14 rounded-md border px-2 py-1 text-right text-sm tabular-nums outline-none focus:border-accent ${
          update.isError ? 'border-danger' : 'border-line'
        }`}
      />
      <span className="text-xs text-muted">/ {assessment.maxScore}</span>
      {update.isError && <span className="sr-only">{(update.error as Error).message}</span>}
    </div>
  )
}

function AssessmentsSection({ course }: { course: Course }) {
  const list = useCourseAssessments(course.id)
  const add = useAddAssessment(course.id)
  const remove = useDeleteAssessment()
  const errors = fieldErrors(add.error)
  const [open, setOpen] = useState(false)

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    add.mutate(
      {
        title: f.required('title'),
        kind: f.text('kind') as AssessmentKind,
        dueOn: f.text('dueOn'),
        weight: f.number('weight'),
        maxScore: f.number('maxScore'),
      },
      { onSuccess: () => form.reset() },
    )
  }

  return (
    <Card>
      <SectionTitle
        action={
          <Button variant="ghost" onClick={() => setOpen((o) => !o)}>
            <Plus size={16} /> Avaliação
          </Button>
        }
      >
        Avaliações
      </SectionTitle>
      {open && (
        <form onSubmit={submit} className="mb-4 grid gap-3 rounded-lg bg-canvas p-3 sm:grid-cols-6 sm:items-end">
          <div className="sm:col-span-2">
            <Field label="Título" error={errors.title}>
              <Input name="title" required maxLength={120} placeholder="P1" />
            </Field>
          </div>
          <Field label="Tipo">
            <Select name="kind" defaultValue="PROVA">
              {Object.entries(ASSESSMENT_KIND).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Data" error={errors.dueOn}>
            <Input name="dueOn" type="date" />
          </Field>
          <Field label="Peso" error={errors.weight}>
            <Input name="weight" type="number" step="0.1" min="0.1" defaultValue="1" />
          </Field>
          <Field label="Nota máx." error={errors.maxScore}>
            <Input name="maxScore" type="number" step="0.1" min="0.1" defaultValue="10" />
          </Field>
          <div className="sm:col-span-6">
            <ErrorText error={Object.keys(errors).length ? null : add.error} />
            <Button type="submit" disabled={add.isPending}>
              Adicionar
            </Button>
          </div>
        </form>
      )}
      <Loaded query={list}>
        {(data) =>
          data.length === 0 ? (
            <Empty>Nenhuma avaliação cadastrada.</Empty>
          ) : (
            <ul className="divide-y divide-line">
              {data.map((a) => (
                <li key={a.id} className="flex items-center gap-3 py-2">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{a.title}</p>
                    <p className="text-xs text-muted">
                      {ASSESSMENT_KIND[a.kind]} · peso {a.weight}
                      {a.dueOn && ` · ${formatDate(a.dueOn)}`}
                    </p>
                  </div>
                  <ScoreInput key={`${a.id}-${a.score}`} assessment={a} />
                  <IconButton label={`Excluir ${a.title}`} onClick={() => remove.mutate(a.id)}>
                    <Trash2 size={15} />
                  </IconButton>
                </li>
              ))}
            </ul>
          )
        }
      </Loaded>
    </Card>
  )
}

// ---------- Horário ----------

function SlotsSection({ course }: { course: Course }) {
  const list = useCourseSlots(course.id)
  const add = useAddSlot(course.id)
  const remove = useDeleteSlot()

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    add.mutate(
      {
        weekday: f.number('weekday')!,
        startTime: f.required('startTime'),
        endTime: f.required('endTime'),
        room: f.text('room'),
      },
      { onSuccess: () => form.reset() },
    )
  }

  return (
    <Card>
      <SectionTitle>Horário</SectionTitle>
      <Loaded query={list}>
        {(data) =>
          data.length === 0 ? (
            <Empty>Sem aulas no horário.</Empty>
          ) : (
            <ul className="mb-3 space-y-1">
              {data.map((s) => (
                <li key={s.id} className="flex items-center gap-2 text-sm">
                  <span className="w-20">{WEEKDAYS[s.weekday - 1]}</span>
                  <span className="tabular-nums text-muted">
                    {formatClock(s.startTime)}–{formatClock(s.endTime)}
                  </span>
                  {s.room && <span className="text-muted">· {s.room}</span>}
                  <IconButton label="Remover horário" className="ml-auto" onClick={() => remove.mutate(s.id)}>
                    <Trash2 size={14} />
                  </IconButton>
                </li>
              ))}
            </ul>
          )
        }
      </Loaded>
      <form onSubmit={submit} className="mt-3 grid grid-cols-2 gap-2">
        <Select name="weekday" aria-label="Dia" defaultValue="1">
          {WEEKDAYS.map((d, i) => (
            <option key={d} value={i + 1}>
              {d}
            </option>
          ))}
        </Select>
        <Input name="room" placeholder="Sala" aria-label="Sala" maxLength={60} />
        <Input name="startTime" type="time" aria-label="Início" required />
        <Input name="endTime" type="time" aria-label="Fim" required />
        <div className="col-span-2">
          <ErrorText error={add.error} />
          <Button type="submit" variant="ghost" disabled={add.isPending}>
            <Plus size={16} /> Aula
          </Button>
        </div>
      </form>
    </Card>
  )
}

// ---------- Faltas ----------

function AbsencesSection({ course }: { course: Course }) {
  const list = useCourseAbsences(course.id)
  const add = useAddAbsence(course.id)
  const remove = useDeleteAbsence()

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    add.mutate({ date: f.required('date'), classesMissed: f.number('classesMissed') }, { onSuccess: () => form.reset() })
  }

  return (
    <Card>
      <SectionTitle>Faltas</SectionTitle>
      <AbsenceMeter course={course} />
      <form onSubmit={submit} className="mt-3 flex items-end gap-2">
        <Input name="date" type="date" aria-label="Data da falta" defaultValue={todayIso()} max={todayIso()} required />
        <div className="w-20">
          <Input name="classesMissed" type="number" min="1" max="20" defaultValue="1" aria-label="Aulas perdidas" />
        </div>
        <Button type="submit" variant="ghost" disabled={add.isPending}>
          <Plus size={16} /> Falta
        </Button>
      </form>
      <ErrorText error={add.error} />
      <Loaded query={list}>
        {(data) =>
          data.length > 0 && (
            <ul className="mt-3 space-y-1">
              {data.map((a) => (
                <li key={a.id} className="flex items-center gap-2 text-sm">
                  <span>{formatDate(a.date)}</span>
                  <span className="text-muted">
                    {a.classesMissed} {a.classesMissed === 1 ? 'aula' : 'aulas'}
                  </span>
                  <IconButton label="Remover falta" className="ml-auto" onClick={() => remove.mutate(a.id)}>
                    <Trash2 size={14} />
                  </IconButton>
                </li>
              ))}
            </ul>
          )
        }
      </Loaded>
    </Card>
  )
}

// ---------- Página ----------

function Summary({ course }: { course: Course }) {
  const g = course.grades
  const status = GRADE_STATUS[g.status]
  const needed = neededText(course)
  return (
    <Card>
      <p className="text-sm text-muted">Média atual</p>
      <p className="text-4xl font-bold tabular-nums">{g.average != null ? formatGrade(g.average) : '—'}</p>
      <div className="mt-2 flex flex-wrap items-center gap-2">
        <StatusBadge tone={status.tone} label={status.label} />
        <span className="text-xs text-muted">aprovação com {formatGrade(g.passingGrade)}</span>
      </div>
      {needed && <p className="mt-2 text-sm">{needed}</p>}
    </Card>
  )
}

export function DisciplinaPage() {
  const { id = '' } = useParams()
  const course = useCourse(id)
  const update = useUpdateCourse()
  const remove = useDeleteCourse()
  const navigate = useNavigate()

  return (
    <Loaded query={course}>
      {(c) => (
        <div className="space-y-6">
          <div className="flex flex-wrap items-center gap-3">
            <Link to="/estudos" className="text-muted hover:text-ink" aria-label="Voltar ao semestre">
              <ArrowLeft size={18} />
            </Link>
            <ColorDot color={c.color} size={14} />
            <div className="min-w-0">
              <h2 className="truncate text-xl font-semibold">{c.name}</h2>
              <p className="text-xs text-muted">{[c.code, c.professor].filter(Boolean).join(' · ')}</p>
            </div>
            <div className="ml-auto flex items-center gap-1">
              <div className="w-36">
                <Select
                  aria-label="Situação da disciplina"
                  value={c.status}
                  onChange={(e) =>
                    update.mutate({
                      id: c.id,
                      termId: c.termId,
                      name: c.name,
                      code: c.code,
                      professor: c.professor,
                      workloadHours: c.workloadHours,
                      maxAbsences: c.maxAbsences,
                      color: c.color,
                      status: e.target.value as CourseStatus,
                    })
                  }
                >
                  {Object.entries(COURSE_STATUS).map(([v, l]) => (
                    <option key={v} value={v}>
                      {l}
                    </option>
                  ))}
                </Select>
              </div>
              <IconButton
                label="Apagar disciplina"
                onClick={() => {
                  if (confirm(`Apagar ${c.name} com notas, faltas e horário?`)) {
                    remove.mutate(c.id, { onSuccess: () => navigate('/estudos') })
                  }
                }}
              >
                <Trash2 size={16} />
              </IconButton>
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
            <div className="space-y-6">
              <AssessmentsSection course={c} />
              <TopicsSection courseId={c.id} />
            </div>
            <div className="space-y-6">
              <Summary course={c} />
              <AbsencesSection course={c} />
              <SlotsSection course={c} />
            </div>
          </div>
        </div>
      )}
    </Loaded>
  )
}
