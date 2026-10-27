import { Flag, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link } from 'react-router'
import type { Task, TaskView } from '../api/types'
import { EditButton } from '../components/Modal'
import { Button, Empty, ErrorText, Field, IconButton, Input, Loaded, Select, Textarea } from '../components/ui'
import { useTermCourses, useTerms } from '../estudos/api'
import { formData } from '../lib/forms'
import { formatDate, todayIso } from '../lib/format'
import {
  type TaskFilter,
  useCreateTask,
  useDeleteTask,
  useGoals,
  useProjects,
  useTasks,
  useToggleTask,
  useUpdateTask,
} from './api'
import { PRIORITY } from './labels'

function DueLabel({ task }: { task: Task }) {
  if (!task.dueOn) return null
  const today = todayIso()
  const text = task.dueOn === today ? 'hoje' : formatDate(task.dueOn)
  return (
    <span
      className={`whitespace-nowrap text-xs tabular-nums ${task.overdue ? 'font-semibold text-danger' : 'text-muted'}`}
    >
      {task.overdue ? `atrasada · ${text}` : text}
    </span>
  )
}

/** Edição completa da tarefa: texto, prioridade, prazo e a que ela está ligada. */
function TaskForm({ task, onDone }: { task: Task; onDone: () => void }) {
  const update = useUpdateTask()
  const goals = (useGoals().data ?? []).filter((g) => g.status === 'ATIVA' || g.id === task.goalId)
  const projects = (useProjects().data ?? []).filter((p) => p.status !== 'CONCLUIDO' || p.id === task.projectId)
  const current = useTerms().data?.find((t) => t.current)
  const termCourses = useTermCourses(current?.id ?? null).data ?? []
  const courses =
    task.courseId && !termCourses.some((c) => c.id === task.courseId)
      ? [...termCourses, { id: task.courseId, name: task.courseName ?? 'Disciplina' }]
      : termCourses
  const [goalId, setGoalId] = useState(task.goalId ?? '')
  const milestones = goals.find((g) => g.id === goalId)?.milestones ?? []

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = formData(e.currentTarget)
    update.mutate(
      {
        id: task.id,
        title: f.required('title'),
        notes: f.text('notes'),
        priority: f.number('priority'),
        dueOn: f.text('dueOn'),
        goalId: goalId || null,
        milestoneId: goalId ? f.text('milestoneId') : null,
        projectId: f.text('projectId'),
        courseId: f.text('courseId'),
      },
      { onSuccess: onDone },
    )
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Tarefa">
        <Input name="title" required maxLength={200} defaultValue={task.title} />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Prazo">
          <Input name="dueOn" type="date" defaultValue={task.dueOn ?? ''} />
        </Field>
        <Field label="Prioridade">
          <Select name="priority" defaultValue={String(task.priority)}>
            {Object.entries(PRIORITY).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Meta">
          <Select value={goalId} onChange={(e) => setGoalId(e.target.value)}>
            <option value="">—</option>
            {goals.map((g) => (
              <option key={g.id} value={g.id}>
                {g.title}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Marco">
          <Select
            name="milestoneId"
            key={goalId}
            defaultValue={task.milestoneId ?? ''}
            disabled={milestones.length === 0}
          >
            <option value="">—</option>
            {milestones.map((m) => (
              <option key={m.id} value={m.id}>
                {m.title}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Projeto">
          <Select name="projectId" defaultValue={task.projectId ?? ''}>
            <option value="">—</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.title}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Disciplina">
          <Select name="courseId" defaultValue={task.courseId ?? ''}>
            <option value="">—</option>
            {courses.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </Field>
      </div>
      <Field label="Notas">
        <Textarea name="notes" maxLength={2000} rows={2} defaultValue={task.notes ?? ''} />
      </Field>
      <ErrorText error={update.error} />
      <Button type="submit" disabled={update.isPending}>
        Salvar
      </Button>
    </form>
  )
}

export function TaskItem({ task, showLinks = true }: { task: Task; showLinks?: boolean }) {
  const toggle = useToggleTask()
  const remove = useDeleteTask()
  // marca na hora (otimista); se a API falhar, volta ao estado do servidor
  const [done, setDone] = useState(task.done)
  const flip = () => {
    const next = !done
    setDone(next)
    toggle.mutate({ id: task.id, done: next }, { onError: () => setDone(task.done) })
  }
  const links = showLinks
    ? [
        task.goalTitle && { to: `/metas/objetivos/${task.goalId}`, label: task.goalTitle },
        task.projectTitle && { to: '/metas/projetos', label: task.projectTitle },
        task.courseName && { to: `/estudos/disciplinas/${task.courseId}`, label: task.courseName },
      ].filter((l): l is { to: string; label: string } => Boolean(l))
    : []
  return (
    <li className="flex items-start gap-3 py-2">
      <input
        type="checkbox"
        checked={done}
        onChange={flip}
        aria-label={`Concluir ${task.title}`}
        className="mt-0.5 size-4 shrink-0 accent-[var(--color-accent)]"
      />
      <div className="min-w-0 flex-1">
        <p className={`text-sm ${done ? 'text-muted line-through' : 'font-medium'}`}>
          {task.priority === 1 && !done && (
            <Flag size={12} className="mr-1 inline text-danger" aria-label="Prioridade alta" />
          )}
          {task.title}
        </p>
        {(links.length > 0 || task.notes) && (
          <p className="mt-0.5 flex flex-wrap gap-x-2 text-xs text-muted">
            {links.map((l) => (
              <Link key={l.to + l.label} to={l.to} className="hover:text-accent hover:underline">
                {l.label}
              </Link>
            ))}
            {task.notes && <span className="truncate">{task.notes}</span>}
          </p>
        )}
      </div>
      {!done && <DueLabel task={task} />}
      <EditButton label={`Editar ${task.title}`} title="Editar tarefa">
        {(close) => <TaskForm task={task} onDone={close} />}
      </EditButton>
      <IconButton label={`Apagar ${task.title}`} onClick={() => remove.mutate(task.id)}>
        <Trash2 size={14} />
      </IconButton>
    </li>
  )
}

/** Lista de tarefas de uma visão/filtro, com campo de criação já ligado ao mesmo filtro. */
export function TaskList({
  view = 'ABERTAS',
  filter = {},
  empty = 'Nenhuma tarefa.',
  showLinks = true,
  addable = true,
  items,
}: {
  view?: TaskView
  filter?: TaskFilter
  empty?: string
  showLinks?: boolean
  addable?: boolean
  /** Lista já carregada (não busca de novo). */
  items?: Task[]
}) {
  const fetched = useTasks(view, filter, items === undefined)
  const tasks = items === undefined ? fetched : { isPending: false, isError: false, error: null, data: items }
  const create = useCreateTask()
  const [due, setDue] = useState(view === 'HOJE' ? todayIso() : '')

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    create.mutate(
      { title: f.required('title'), dueOn: due || null, priority: f.number('priority'), ...filter },
      { onSuccess: () => form.reset() },
    )
  }

  return (
    <div>
      <Loaded query={tasks}>
        {(list) =>
          list.length === 0 ? (
            <Empty>{empty}</Empty>
          ) : (
            <ul className="divide-y divide-line">
              {list.map((t) => (
                <TaskItem key={t.id} task={t} showLinks={showLinks} />
              ))}
            </ul>
          )
        }
      </Loaded>
      {addable && view !== 'FEITAS' && (
        <form onSubmit={submit} className="mt-3 flex flex-wrap gap-2">
          <div className="min-w-48 flex-1">
            <Input name="title" placeholder="Nova tarefa" aria-label="Nova tarefa" required maxLength={200} />
          </div>
          <div className="w-36">
            <Input type="date" value={due} onChange={(e) => setDue(e.target.value)} aria-label="Prazo da tarefa" />
          </div>
          <div className="w-28">
            <Select name="priority" defaultValue="2" aria-label="Prioridade">
              {Object.entries(PRIORITY).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </Select>
          </div>
          <Button type="submit" variant="ghost" disabled={create.isPending}>
            Adicionar
          </Button>
          <div className="basis-full">
            <ErrorText error={create.error} />
          </div>
        </form>
      )}
    </div>
  )
}
