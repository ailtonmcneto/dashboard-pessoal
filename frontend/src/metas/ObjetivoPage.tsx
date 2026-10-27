import { ArrowLeft, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import type { Goal, GoalStatus, Milestone } from '../api/types'
import { EditButton } from '../components/Modal'
import { StatusBadge } from '../components/StatusBadge'
import { Button, Card, Empty, ErrorText, Field, IconButton, Input, Loaded, SectionTitle } from '../components/ui'
import { formData } from '../lib/forms'
import { formatDate } from '../lib/format'
import {
  useAddMilestone,
  useDeleteGoal,
  useDeleteMilestone,
  useGoal,
  useGoalStatus,
  useToggleMilestone,
  useUpdateGoal,
  useUpdateMilestone,
} from './api'
import { GoalBar } from './GoalCard'
import { AREA, PACE, PROGRESS_MODE, progressText } from './labels'
import { GoalForm } from './ObjetivosPage'
import { TaskList } from './TaskList'

/** Caixa do marco que marca na hora (otimista) e volta se a API falhar. */
function MilestoneCheck({ id, title, done }: { id: string; title: string; done: boolean }) {
  const toggle = useToggleMilestone()
  const [checked, setChecked] = useState(done)
  return (
    <input
      type="checkbox"
      checked={checked}
      onChange={() => {
        const next = !checked
        setChecked(next)
        toggle.mutate({ id, done: next }, { onError: () => setChecked(done) })
      }}
      aria-label={`Concluir marco ${title}`}
      className="size-4 accent-[var(--color-accent)]"
    />
  )
}

function MilestoneForm({ milestone, onDone }: { milestone: Milestone; onDone: () => void }) {
  const update = useUpdateMilestone()
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = formData(e.currentTarget)
    update.mutate({ id: milestone.id, title: f.required('title'), dueOn: f.text('dueOn') }, { onSuccess: onDone })
  }
  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Marco">
        <Input name="title" required maxLength={120} defaultValue={milestone.title} />
      </Field>
      <Field label="Prazo">
        <Input name="dueOn" type="date" defaultValue={milestone.dueOn ?? ''} />
      </Field>
      <ErrorText error={update.error} />
      <Button type="submit" disabled={update.isPending}>
        Salvar
      </Button>
    </form>
  )
}

function Milestones({ goal }: { goal: Goal }) {
  const add = useAddMilestone(goal.id)
  const remove = useDeleteMilestone()
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    add.mutate({ title: f.required('title'), dueOn: f.text('dueOn') }, { onSuccess: () => form.reset() })
  }
  return (
    <Card>
      <SectionTitle>Marcos</SectionTitle>
      {goal.progressMode !== 'MARCOS' && (
        <p className="mb-2 text-xs text-muted">
          Esta meta mede o progresso de outro jeito; os marcos servem de roteiro.
        </p>
      )}
      {goal.milestones.length === 0 ? (
        <Empty>Nenhum marco ainda.</Empty>
      ) : (
        <ol className="divide-y divide-line">
          {goal.milestones.map((m, i) => (
            <li key={m.id} className="flex items-center gap-3 py-2">
              <MilestoneCheck id={m.id} title={m.title} done={m.done} />
              <span className="w-5 text-xs tabular-nums text-muted">{i + 1}.</span>
              <span className={`flex-1 text-sm ${m.done ? 'text-muted line-through' : 'font-medium'}`}>{m.title}</span>
              <span className="text-xs text-muted">
                {m.done ? `feito ${formatDate(m.doneOn!)}` : m.dueOn ? `até ${formatDate(m.dueOn)}` : ''}
              </span>
              <EditButton label={`Editar marco ${m.title}`} title="Editar marco">
                {(close) => <MilestoneForm milestone={m} onDone={close} />}
              </EditButton>
              <IconButton label={`Apagar marco ${m.title}`} onClick={() => remove.mutate(m.id)}>
                <Trash2 size={14} />
              </IconButton>
            </li>
          ))}
        </ol>
      )}
      <form onSubmit={submit} className="mt-3 flex flex-wrap gap-2">
        <div className="min-w-48 flex-1">
          <Input name="title" placeholder="Novo marco" aria-label="Novo marco" required maxLength={120} />
        </div>
        <div className="w-36">
          <Input name="dueOn" type="date" aria-label="Prazo do marco" />
        </div>
        <Button type="submit" variant="ghost" disabled={add.isPending}>
          Adicionar
        </Button>
      </form>
      <ErrorText error={add.error} />
    </Card>
  )
}

/** Atualizar o valor de uma meta manual. */
function ManualValue({ goal }: { goal: Goal }) {
  const update = useUpdateGoal()
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    update.mutate({
      id: goal.id,
      title: goal.title,
      description: goal.description,
      area: goal.area,
      startsOn: goal.startsOn,
      dueOn: goal.dueOn,
      progressMode: goal.progressMode,
      targetValue: goal.targetValue,
      unit: goal.unit,
      accountId: goal.accountId,
      currentValue: formData(e.currentTarget).number('currentValue'),
    })
  }
  return (
    <form onSubmit={submit} className="mt-3 flex items-end gap-2">
      <div className="w-32">
        <Input
          key={goal.currentValue}
          name="currentValue"
          type="number"
          step="0.01"
          min="0"
          defaultValue={goal.currentValue ?? 0}
          aria-label={`Valor atual (${goal.unit ?? ''})`}
        />
      </div>
      <Button type="submit" variant="ghost" disabled={update.isPending}>
        Atualizar
      </Button>
      <ErrorText error={update.error} />
    </form>
  )
}

export function ObjetivoPage() {
  const { id = '' } = useParams()
  const goal = useGoal(id)
  const status = useGoalStatus()
  const remove = useDeleteGoal()
  const navigate = useNavigate()

  const setStatus = (s: GoalStatus) => status.mutate({ id, status: s })

  return (
    <Loaded query={goal}>
      {(g) => {
        const pace = PACE[g.pace]
        return (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <Link to="/metas/objetivos" className="text-muted hover:text-ink" aria-label="Voltar às metas">
                <ArrowLeft size={18} />
              </Link>
              <div className="min-w-0">
                <h2 className="text-xl font-semibold">{g.title}</h2>
                <p className="text-xs text-muted">
                  {AREA[g.area]} · {PROGRESS_MODE[g.progressMode].label}
                  {g.startsOn && ` · de ${formatDate(g.startsOn)}`}
                  {g.dueOn && ` até ${formatDate(g.dueOn)}`}
                </p>
              </div>
              <div className="ml-auto flex items-center gap-1">
                {g.status === 'ATIVA' ? (
                  <>
                    <Button onClick={() => setStatus('CONCLUIDA')}>Concluir</Button>
                    <Button variant="ghost" onClick={() => setStatus('ABANDONADA')}>
                      Abandonar
                    </Button>
                  </>
                ) : (
                  <Button variant="ghost" onClick={() => setStatus('ATIVA')}>
                    Reabrir
                  </Button>
                )}
                <EditButton label="Editar meta" title="Editar meta">
                  {(close) => <GoalForm initial={g} onDone={close} />}
                </EditButton>
                <IconButton
                  label="Apagar meta"
                  onClick={() =>
                    confirm(`Apagar “${g.title}”? Tarefas e projetos ligados continuam.`) &&
                    remove.mutate(g.id, { onSuccess: () => navigate('/metas/objetivos') })
                  }
                >
                  <Trash2 size={16} />
                </IconButton>
              </div>
            </div>

            <Card>
              <div className="mb-2 flex flex-wrap items-baseline gap-3">
                <p className="text-4xl font-bold tabular-nums">{g.percent}%</p>
                <StatusBadge tone={pace.tone} label={pace.label} />
                <span className="text-sm text-muted">{progressText(g)}</span>
              </div>
              <GoalBar goal={g} />
              {g.expectedPercent != null && g.status === 'ATIVA' && (
                <p className="mt-2 text-xs text-muted">
                  A marca na barra é onde você deveria estar hoje pelo prazo: {g.expectedPercent}%.
                </p>
              )}
              {g.progressMode === 'MANUAL' && g.status === 'ATIVA' && <ManualValue goal={g} />}
              {g.description && <p className="mt-3 whitespace-pre-line text-sm">{g.description}</p>}
              <ErrorText error={status.error} />
            </Card>

            <div className="grid gap-6 lg:grid-cols-2">
              <Milestones goal={g} />
              <Card>
                <SectionTitle>Tarefas da meta</SectionTitle>
                <TaskList filter={{ goalId: g.id }} showLinks={false} empty="Nenhuma tarefa ligada." />
              </Card>
            </div>
          </div>
        )
      }}
    </Loaded>
  )
}
