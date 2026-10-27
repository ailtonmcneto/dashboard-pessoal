import { Plus, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import type { Course, Term } from '../api/types'
import { EditButton } from '../components/Modal'
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
} from '../components/ui'
import { formData } from '../lib/forms'
import { formatDate } from '../lib/format'
import {
  useCreateCourse,
  useCreateTerm,
  useDeleteTerm,
  useTermCourses,
  useTerms,
  useUpdateCourse,
  useUpdateTerm,
} from './api'
import { COURSE_COLORS } from './labels'
import { CourseCard } from './parts'

/** Cria um semestre ou, com `initial`, edita esse semestre. */
function TermForm({ initial, onSaved }: { initial?: Term; onSaved: (t: Term) => void }) {
  const create = useCreateTerm()
  const update = useUpdateTerm()
  const save = initial ? update : create
  const errors = save.error instanceof ApiError ? save.error.errors : {}

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = formData(e.currentTarget)
    const req = {
      name: f.required('name'),
      startsOn: f.required('startsOn'),
      endsOn: f.required('endsOn'),
      passingGrade: f.number('passingGrade'),
    }
    if (initial) update.mutate({ id: initial.id, ...req }, { onSuccess: onSaved })
    else create.mutate(req, { onSuccess: onSaved })
  }

  return (
    <form onSubmit={submit} className={`grid gap-3 sm:items-end ${initial ? 'sm:grid-cols-2' : 'sm:grid-cols-4'}`}>
      <Field label="Nome" error={errors.name}>
        <Input name="name" placeholder="2026.2" required maxLength={60} defaultValue={initial?.name} />
      </Field>
      <Field label="Início" error={errors.startsOn}>
        <Input name="startsOn" type="date" required defaultValue={initial?.startsOn} />
      </Field>
      <Field label="Fim" error={errors.endsOn}>
        <Input name="endsOn" type="date" required defaultValue={initial?.endsOn} />
      </Field>
      <Field label="Média para aprovar" error={errors.passingGrade}>
        <Input
          name="passingGrade"
          type="number"
          step="0.1"
          min="0"
          max="10"
          defaultValue={initial?.passingGrade ?? 6}
        />
      </Field>
      <div className="sm:col-span-full">
        <ErrorText error={Object.keys(errors).length ? null : save.error} />
        <Button type="submit" disabled={save.isPending}>
          {initial ? 'Salvar' : 'Criar semestre'}
        </Button>
      </div>
    </form>
  )
}

/** Cria uma disciplina no semestre ou, com `initial`, edita essa disciplina (a situação não muda aqui). */
export function CourseForm({ termId, initial, onDone }: { termId: string; initial?: Course; onDone?: () => void }) {
  const create = useCreateCourse()
  const update = useUpdateCourse()
  const save = initial ? update : create
  const errors = save.error instanceof ApiError ? save.error.errors : {}
  const [color, setColor] = useState(initial?.color ?? COURSE_COLORS[0])

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const req = {
      termId,
      name: f.required('name'),
      code: f.text('code'),
      professor: f.text('professor'),
      workloadHours: f.number('workloadHours'),
      maxAbsences: f.number('maxAbsences'),
      color,
    }
    if (initial) update.mutate({ id: initial.id, ...req, status: initial.status }, { onSuccess: onDone })
    else create.mutate(req, { onSuccess: () => form.reset() })
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Nome" error={errors.name}>
        <Input name="name" required maxLength={120} placeholder="Cálculo I" defaultValue={initial?.name} />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Código" error={errors.code}>
          <Input name="code" maxLength={30} defaultValue={initial?.code ?? ''} />
        </Field>
        <Field label="Professor(a)" error={errors.professor}>
          <Input name="professor" maxLength={120} defaultValue={initial?.professor ?? ''} />
        </Field>
        <Field label="Carga horária (h)" error={errors.workloadHours}>
          <Input name="workloadHours" type="number" min="1" defaultValue={initial?.workloadHours ?? ''} />
        </Field>
        <Field label="Limite de faltas" error={errors.maxAbsences}>
          <Input
            name="maxAbsences"
            type="number"
            min="0"
            placeholder="em aulas"
            defaultValue={initial?.maxAbsences ?? ''}
          />
        </Field>
      </div>
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
              className={`size-7 rounded-full ring-offset-2 ${color === c ? 'ring-2 ring-ink' : ''}`}
              style={{ background: c }}
            />
          ))}
        </div>
      </fieldset>
      <ErrorText error={Object.keys(errors).length ? null : save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? (
          'Salvar'
        ) : (
          <>
            <Plus size={16} /> Adicionar disciplina
          </>
        )}
      </Button>
    </form>
  )
}

export function SemestrePage() {
  const terms = useTerms()
  const [selected, setSelected] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const deleteTerm = useDeleteTerm()

  const list = terms.data ?? []
  const term = list.find((t) => t.id === selected) ?? list.find((t) => t.current) ?? list[0]
  const courses = useTermCourses(term?.id ?? null)

  if (terms.isPending) return <Empty>Carregando…</Empty>
  if (terms.isError) return <ErrorText error={terms.error} />

  if (list.length === 0 || creating) {
    return (
      <Card>
        <SectionTitle
          action={
            list.length > 0 && (
              <Button variant="ghost" onClick={() => setCreating(false)}>
                Cancelar
              </Button>
            )
          }
        >
          {list.length === 0 ? 'Comece pelo seu semestre' : 'Novo semestre'}
        </SectionTitle>
        <TermForm
          onSaved={(t) => {
            setSelected(t.id)
            setCreating(false)
          }}
        />
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <div className="w-48">
          <Select value={term.id} onChange={(e) => setSelected(e.target.value)} aria-label="Semestre">
            {list.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
                {t.current ? ' (atual)' : ''}
              </option>
            ))}
          </Select>
        </div>
        <p className="text-sm text-muted">
          {formatDate(term.startsOn)} – {formatDate(term.endsOn)} · média {term.passingGrade}
        </p>
        <div className="ml-auto flex items-center gap-1">
          <Button variant="ghost" onClick={() => setCreating(true)}>
            <Plus size={16} /> Semestre
          </Button>
          <EditButton label={`Editar semestre ${term.name}`} title="Editar semestre">
            {(close) => <TermForm initial={term} onSaved={close} />}
          </EditButton>
          <IconButton
            label={`Apagar semestre ${term.name}`}
            onClick={() => {
              if (confirm(`Apagar ${term.name} com todas as disciplinas, notas e faltas?`)) {
                deleteTerm.mutate(term.id, { onSuccess: () => setSelected(null) })
              }
            }}
          >
            <Trash2 size={16} />
          </IconButton>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <Loaded query={courses}>
          {(data) =>
            data.length === 0 ? (
              <Card>
                <Empty>Nenhuma disciplina neste semestre ainda.</Empty>
              </Card>
            ) : (
              <div className="grid content-start gap-4 sm:grid-cols-2">
                {data.map((c) => (
                  <CourseCard key={c.id} course={c} />
                ))}
              </div>
            )
          }
        </Loaded>
        <Card className="self-start">
          <SectionTitle>Nova disciplina</SectionTitle>
          <CourseForm termId={term.id} />
        </Card>
      </div>
    </div>
  )
}
