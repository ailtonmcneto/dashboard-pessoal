import { ExternalLink, ListChecks, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import type { Project, ProjectKind, ProjectStatus } from '../api/types'
import { Meter } from '../components/charts'
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
  Textarea,
} from '../components/ui'
import { useTermCourses, useTerms } from '../estudos/api'
import { formData } from '../lib/forms'
import { formatDate } from '../lib/format'
import { useCreateProject, useDeleteProject, useGoals, useProjects, useUpdateProject } from './api'
import { PROJECT_KIND, PROJECT_STATUS } from './labels'
import { TaskList } from './TaskList'

function ProjectRow({ p }: { p: Project }) {
  const [open, setOpen] = useState(false)
  const update = useUpdateProject()
  const remove = useDeleteProject()
  const total = p.openTasks + p.doneTasks
  return (
    <li className={`py-3 ${p.status === 'CONCLUIDO' ? 'opacity-70' : ''}`}>
      <div className="flex flex-wrap items-center gap-3">
        <div className="min-w-48 flex-1">
          <p className="flex items-center gap-1 font-medium">
            {p.title}
            {p.url && (
              <a
                href={p.url}
                target="_blank"
                rel="noreferrer"
                className="text-muted hover:text-accent"
                aria-label="Abrir link"
              >
                <ExternalLink size={14} />
              </a>
            )}
          </p>
          <p className="flex flex-wrap gap-x-2 text-xs text-muted">
            <span>{PROJECT_KIND[p.kind]}</span>
            {p.dueOn && (
              <span className={p.overdue ? 'font-semibold text-danger' : ''}>
                {p.overdue ? 'atrasado · ' : 'prazo '}
                {formatDate(p.dueOn)}
              </span>
            )}
            {p.goalTitle && (
              <Link to={`/metas/objetivos/${p.goalId}`} className="hover:text-accent hover:underline">
                meta: {p.goalTitle}
              </Link>
            )}
            {p.courseName && (
              <Link to={`/estudos/disciplinas/${p.courseId}`} className="hover:text-accent hover:underline">
                {p.courseName}
              </Link>
            )}
          </p>
        </div>
        <div className="w-32">
          <Select
            aria-label={`Situação de ${p.title}`}
            value={p.status}
            onChange={(e) =>
              update.mutate({
                id: p.id,
                title: p.title,
                description: p.description,
                kind: p.kind,
                status: e.target.value as ProjectStatus,
                startsOn: p.startsOn,
                dueOn: p.dueOn,
                url: p.url,
                goalId: p.goalId,
                courseId: p.courseId,
              })
            }
          >
            {Object.entries(PROJECT_STATUS).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </div>
        <EditButton label={`Editar ${p.title}`} title="Editar projeto">
          {(close) => <ProjectForm initial={p} onDone={close} />}
        </EditButton>
        <IconButton label={`Tarefas de ${p.title}`} onClick={() => setOpen((o) => !o)} aria-expanded={open}>
          <ListChecks size={16} />
        </IconButton>
        <IconButton
          label={`Apagar ${p.title}`}
          onClick={() => confirm(`Apagar “${p.title}”? As tarefas continuam, sem o projeto.`) && remove.mutate(p.id)}
        >
          <Trash2 size={15} />
        </IconButton>
      </div>
      {total > 0 && (
        <div className="mt-2 flex items-center gap-2">
          <div className="flex-1">
            <Meter value={p.doneTasks} max={total} label={`${p.title}: ${p.percent}% das tarefas`} />
          </div>
          <span className="text-xs tabular-nums text-muted">
            {p.doneTasks}/{total} tarefas
          </span>
        </div>
      )}
      {open && (
        <div className="mt-3 rounded-lg bg-canvas p-3">
          <TaskList filter={{ projectId: p.id }} showLinks={false} empty="Nenhuma tarefa no projeto." />
        </div>
      )}
    </li>
  )
}

/** Cria um projeto ou, com `initial`, edita esse projeto (a situação muda na própria lista). */
function ProjectForm({ initial, onDone }: { initial?: Project; onDone?: () => void }) {
  const create = useCreateProject()
  const update = useUpdateProject()
  const save = initial ? update : create
  const goals = (useGoals().data ?? []).filter((g) => g.status === 'ATIVA' || g.id === initial?.goalId)
  const current = useTerms().data?.find((t) => t.current)
  const termCourses = useTermCourses(current?.id ?? null).data ?? []
  // a disciplina ligada pode ser de outro semestre: continua na lista para não se perder ao salvar
  const courses =
    initial?.courseId && !termCourses.some((c) => c.id === initial.courseId)
      ? [...termCourses, { id: initial.courseId, name: initial.courseName ?? 'Disciplina' }]
      : termCourses
  const errors = save.error instanceof ApiError ? save.error.errors : {}
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const req = {
      title: f.required('title'),
      description: f.text('description'),
      kind: f.text('kind') as ProjectKind,
      dueOn: f.text('dueOn'),
      url: f.text('url'),
      goalId: f.text('goalId'),
      courseId: f.text('courseId'),
    }
    if (initial) {
      update.mutate(
        { id: initial.id, ...req, status: initial.status, startsOn: initial.startsOn },
        { onSuccess: onDone },
      )
    } else create.mutate(req, { onSuccess: () => form.reset() })
  }
  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Projeto" error={errors.title}>
        <Input
          name="title"
          required
          maxLength={120}
          placeholder="Ex.: portfólio pessoal"
          defaultValue={initial?.title}
        />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Tipo">
          <Select name="kind" defaultValue={initial?.kind ?? 'PESSOAL'}>
            {Object.entries(PROJECT_KIND).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Prazo" error={errors.dueOn}>
          <Input name="dueOn" type="date" defaultValue={initial?.dueOn ?? ''} />
        </Field>
      </div>
      <Field label="Serve à meta">
        <Select name="goalId" defaultValue={initial?.goalId ?? ''}>
          <option value="">—</option>
          {goals.map((g) => (
            <option key={g.id} value={g.id}>
              {g.title}
            </option>
          ))}
        </Select>
      </Field>
      {courses.length > 0 && (
        <Field label="Disciplina">
          <Select name="courseId" defaultValue={initial?.courseId ?? ''}>
            <option value="">—</option>
            {courses.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </Field>
      )}
      <Field label="Link" error={errors.url}>
        <Input
          name="url"
          type="url"
          placeholder="https://github.com/…"
          maxLength={500}
          defaultValue={initial?.url ?? ''}
        />
      </Field>
      <Field label="Descrição">
        <Textarea name="description" rows={2} maxLength={2000} defaultValue={initial?.description ?? ''} />
      </Field>
      <ErrorText error={Object.keys(errors).length ? null : save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? 'Salvar' : 'Criar projeto'}
      </Button>
    </form>
  )
}

export function ProjetosPage() {
  const projects = useProjects()
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <Card>
        <SectionTitle>Projetos</SectionTitle>
        <Loaded query={projects}>
          {(list) =>
            list.length === 0 ? (
              <Empty>Nenhum projeto ainda.</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {list.map((p) => (
                  <ProjectRow key={p.id} p={p} />
                ))}
              </ul>
            )
          }
        </Loaded>
      </Card>
      <Card className="self-start">
        <SectionTitle>Novo projeto</SectionTitle>
        <ProjectForm />
      </Card>
    </div>
  )
}
