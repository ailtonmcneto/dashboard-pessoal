import { Flag, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link } from 'react-router'
import type { Task, TaskView } from '../api/types'
import { Button, Empty, ErrorText, IconButton, Input, Loaded, Select } from '../components/ui'
import { formData } from '../lib/forms'
import { formatDate, todayIso } from '../lib/format'
import { type TaskFilter, useCreateTask, useDeleteTask, useTasks, useToggleTask } from './api'
import { PRIORITY } from './labels'

function DueLabel({ task }: { task: Task }) {
  if (!task.dueOn) return null
  const today = todayIso()
  const text = task.dueOn === today ? 'hoje' : formatDate(task.dueOn)
  return (
    <span className={`whitespace-nowrap text-xs tabular-nums ${task.overdue ? 'font-semibold text-danger' : 'text-muted'}`}>
      {task.overdue ? `atrasada · ${text}` : text}
    </span>
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
}: {
  view?: TaskView
  filter?: TaskFilter
  empty?: string
  showLinks?: boolean
  addable?: boolean
}) {
  const tasks = useTasks(view, filter)
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
