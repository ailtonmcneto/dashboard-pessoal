import { ChevronRight, Plus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '../api/client'
import type { Aggregation, Metric } from '../api/types'
import { Meter } from '../components/charts'
import { EditButton } from '../components/Modal'
import { StatusBadge } from '../components/StatusBadge'
import { Button, Card, Empty, ErrorText, Field, Input, Loaded, SectionTitle, Select } from '../components/ui'
import { formData } from '../lib/forms'
import { useAddMeasurement, useCreateMetric, useDefaultMetrics, useMetrics, useUpdateMetric } from './api'
import { AGGREGATION, formatValue, statusFor } from './labels'

/** Registro rápido: só o valor, no momento atual. */
export function QuickAdd({ metric }: { metric: Metric }) {
  const add = useAddMeasurement()
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const value = formData(form).number('value')
    if (value === null) return
    add.mutate({ metricId: metric.id, value }, { onSuccess: () => form.reset() })
  }
  return (
    <form onSubmit={submit} className="flex gap-2">
      <Input
        name="value"
        type="number"
        step="0.01"
        min="0"
        inputMode="decimal"
        placeholder={metric.unit}
        aria-label={`Registrar ${metric.name} (${metric.unit})`}
        required
      />
      <Button type="submit" variant="ghost" disabled={add.isPending} aria-label={`Adicionar ${metric.name}`}>
        <Plus size={16} />
      </Button>
      {add.error ? <ErrorText error={add.error} /> : null}
    </form>
  )
}

function MetricCard({ m }: { m: Metric }) {
  const status = statusFor(m)
  const over = m.targetKind === 'MAXIMO' && m.today != null && m.target != null && m.today > m.target
  return (
    <Card className={m.archived ? 'opacity-60' : ''}>
      <div className="mb-2 flex items-start justify-between gap-2">
        <div>
          <p className="flex items-center gap-2 font-semibold">
            <span className="size-2.5 rounded-full" style={{ background: m.color }} aria-hidden="true" />
            {m.name}
          </p>
          <p className="text-xs text-muted">{AGGREGATION[m.aggregation]}</p>
        </div>
        <div className="flex items-center">
          <EditButton label={`Editar ${m.name}`} title="Editar métrica">
            {(close) => <MetricForm initial={m} onDone={close} />}
          </EditButton>
          <Link
            to={`/saude/metricas/${m.id}`}
            className="text-muted hover:text-ink"
            aria-label={`Detalhes de ${m.name}`}
          >
            <ChevronRight size={18} />
          </Link>
        </div>
      </div>
      <p className="text-2xl font-bold tabular-nums">{m.today != null ? formatValue(m.today, m.unit) : '—'}</p>
      {m.target != null && (
        <div className="my-2 space-y-1">
          <Meter
            value={m.today ?? 0}
            max={m.target}
            tone={over ? 'warning' : 'chart'}
            label={`${m.name}: ${m.today ?? 0} de ${m.target} ${m.unit}`}
          />
          <p className="text-xs text-muted">
            {m.targetKind === 'MINIMO' ? 'meta' : 'limite'} {formatValue(m.target, m.unit)}
          </p>
        </div>
      )}
      <div className="my-2 flex flex-wrap items-center gap-2 text-xs text-muted">
        <StatusBadge tone={status.tone} label={status.label} />
        {m.average7 != null && <span>média 7 dias: {formatValue(m.average7, m.unit)}</span>}
      </div>
      {!m.archived && <QuickAdd metric={m} />}
    </Card>
  )
}

/** Cria uma métrica ou, com `initial`, edita essa métrica (os registros continuam). */
export function MetricForm({ initial, onDone }: { initial?: Metric; onDone?: () => void }) {
  const create = useCreateMetric()
  const update = useUpdateMetric()
  const save = initial ? update : create
  const [hasTarget, setHasTarget] = useState(initial?.target != null)
  const errors = save.error instanceof ApiError ? save.error.errors : {}
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const req = {
      name: f.required('name'),
      unit: f.required('unit'),
      aggregation: f.text('aggregation') as Aggregation,
      target: hasTarget ? f.number('target') : null,
      targetKind: hasTarget ? (f.text('targetKind') as 'MINIMO' | 'MAXIMO') : null,
    }
    if (initial) {
      update.mutate({ id: initial.id, ...req, color: initial.color, archived: initial.archived }, { onSuccess: onDone })
    } else create.mutate(req, { onSuccess: () => form.reset() })
  }
  return (
    <form onSubmit={submit} className="space-y-3">
      <div className="grid grid-cols-2 gap-3">
        <Field label="Nome" error={errors.name}>
          <Input name="name" required maxLength={40} placeholder="Ex.: Passos" defaultValue={initial?.name} />
        </Field>
        <Field label="Unidade" error={errors.unit}>
          <Input name="unit" required maxLength={12} placeholder="Ex.: passos" defaultValue={initial?.unit} />
        </Field>
      </div>
      <Field label="Valor do dia">
        <Select name="aggregation" defaultValue={initial?.aggregation ?? 'SOMA'}>
          {Object.entries(AGGREGATION).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </Select>
      </Field>
      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={hasTarget}
          onChange={(e) => setHasTarget(e.target.checked)}
          className="size-4 accent-[var(--color-accent)]"
        />
        Tem meta diária
      </label>
      {hasTarget && (
        <div className="grid grid-cols-2 gap-3">
          <Field label="Tipo de meta">
            <Select name="targetKind" defaultValue={initial?.targetKind ?? 'MINIMO'}>
              <option value="MINIMO">Pelo menos</option>
              <option value="MAXIMO">No máximo</option>
            </Select>
          </Field>
          <Field label="Valor" error={errors.target}>
            <Input name="target" type="number" step="0.01" min="0.01" required defaultValue={initial?.target ?? ''} />
          </Field>
        </div>
      )}
      <ErrorText error={Object.keys(errors).length ? null : save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? 'Salvar' : 'Criar métrica'}
      </Button>
    </form>
  )
}

export function MetricasPage() {
  const metrics = useMetrics()
  const defaults = useDefaultMetrics()
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
      <Loaded query={metrics}>
        {(list) =>
          list.length === 0 ? (
            <Card>
              <Empty>Nenhuma métrica ainda.</Empty>
              <Button className="mt-3" onClick={() => defaults.mutate()} disabled={defaults.isPending}>
                Usar sugestões (sono, água, treino, peso, humor)
              </Button>
              <ErrorText error={defaults.error} />
            </Card>
          ) : (
            <div className="grid content-start gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {list.map((m) => (
                <MetricCard key={m.id} m={m} />
              ))}
            </div>
          )
        }
      </Loaded>
      <Card className="self-start">
        <SectionTitle>Nova métrica</SectionTitle>
        <MetricForm />
      </Card>
    </div>
  )
}
