import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import type { Goal, GoalArea, ProgressMode } from '../api/types'
import { Button, Card, Empty, ErrorText, Field, Input, Loaded, SectionTitle, Select, Textarea } from '../components/ui'
import { useAccounts } from '../financas/api'
import { formData } from '../lib/forms'
import { todayIso } from '../lib/format'
import { useCreateGoal, useGoals, useUpdateGoal } from './api'
import { GoalCard } from './GoalCard'
import { AREA, PROGRESS_MODE } from './labels'

/** Cria uma meta (e abre a página dela) ou, com `initial`, edita essa meta. */
export function GoalForm({ initial, onDone }: { initial?: Goal; onDone?: () => void }) {
  const create = useCreateGoal()
  const update = useUpdateGoal()
  const save = initial ? update : create
  const accounts = (useAccounts().data ?? []).filter((a) => !a.archived || a.id === initial?.accountId)
  const [mode, setMode] = useState<ProgressMode>(initial?.progressMode ?? 'MARCOS')
  const navigate = useNavigate()
  const errors = save.error instanceof ApiError ? save.error.errors : {}

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = formData(e.currentTarget)
    const req = {
      title: f.required('title'),
      description: f.text('description'),
      area: f.text('area') as GoalArea,
      startsOn: f.text('startsOn'),
      dueOn: f.text('dueOn'),
      progressMode: mode,
      targetValue: f.number('targetValue'),
      currentValue: f.number('currentValue'),
      unit: f.text('unit'),
      accountId: f.text('accountId'),
    }
    if (initial) update.mutate({ id: initial.id, ...req }, { onSuccess: onDone })
    else create.mutate(req, { onSuccess: (g) => navigate(`/metas/objetivos/${g.id}`) })
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Meta" error={errors.title}>
        <Input name="title" required maxLength={120} placeholder="Ex.: terminar o TCC" defaultValue={initial?.title} />
      </Field>
      <Field label="Área">
        <Select name="area" defaultValue={initial?.area ?? 'PESSOAL'}>
          {Object.entries(AREA).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </Select>
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Início" error={errors.startsOn}>
          <Input name="startsOn" type="date" defaultValue={initial ? (initial.startsOn ?? '') : todayIso()} />
        </Field>
        <Field label="Prazo" error={errors.dueOn}>
          <Input name="dueOn" type="date" defaultValue={initial?.dueOn ?? ''} />
        </Field>
      </div>
      <Field label="Como medir o progresso">
        <Select value={mode} onChange={(e) => setMode(e.target.value as ProgressMode)}>
          {Object.entries(PROGRESS_MODE).map(([v, m]) => (
            <option key={v} value={v}>
              {m.label}
            </option>
          ))}
        </Select>
      </Field>
      <p className="text-xs text-muted">{PROGRESS_MODE[mode].hint}</p>
      {mode === 'MANUAL' && (
        <div className="grid grid-cols-3 gap-3">
          <Field label="Atual">
            <Input name="currentValue" type="number" step="0.01" min="0" defaultValue={initial?.currentValue ?? 0} />
          </Field>
          <Field label="Alvo" error={errors.targetValue}>
            <Input
              name="targetValue"
              type="number"
              step="0.01"
              min="0.01"
              required
              defaultValue={initial?.targetValue ?? ''}
            />
          </Field>
          <Field label="Unidade">
            <Input name="unit" maxLength={20} placeholder="livros" defaultValue={initial?.unit ?? ''} />
          </Field>
        </div>
      )}
      {mode === 'FINANCEIRO' &&
        (accounts.length === 0 ? (
          <p className="text-sm text-danger">Crie uma conta em Finanças para usar este modo.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            <Field label="Conta">
              <Select name="accountId" required defaultValue={initial?.accountId ?? ''}>
                <option value="" disabled>
                  Escolha…
                </option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Valor-alvo (R$)" error={errors.targetValue}>
              <Input
                name="targetValue"
                type="number"
                step="0.01"
                min="0.01"
                required
                defaultValue={initial?.targetValue ?? ''}
              />
            </Field>
          </div>
        ))}
      <Field label="Descrição">
        <Textarea name="description" maxLength={2000} rows={2} defaultValue={initial?.description ?? ''} />
      </Field>
      <ErrorText error={Object.keys(errors).length ? null : save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? 'Salvar' : 'Criar meta'}
      </Button>
    </form>
  )
}

export function ObjetivosPage() {
  const goals = useGoals()
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
      <Loaded query={goals}>
        {(list) =>
          list.length === 0 ? (
            <Card>
              <Empty>Nenhuma meta ainda.</Empty>
            </Card>
          ) : (
            <div className="grid content-start gap-4 sm:grid-cols-2">
              {list.map((g) => (
                <GoalCard key={g.id} goal={g} />
              ))}
            </div>
          )
        }
      </Loaded>
      <Card className="self-start">
        <SectionTitle>Nova meta</SectionTitle>
        <GoalForm />
      </Card>
    </div>
  )
}
