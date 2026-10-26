import { ArrowLeft, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import type { Metric } from '../api/types'
import { ColumnChart, LineChart } from '../components/charts'
import { EditButton } from '../components/Modal'
import { Button, Card, Empty, ErrorText, Field, IconButton, Input, Loaded, SectionTitle } from '../components/ui'
import { formData } from '../lib/forms'
import {
  addDaysIso,
  dayKey,
  formatTime,
  formatWeekdayDate,
  localInputToIso,
  parseLocalDate,
  todayIso,
} from '../lib/format'
import {
  useAddMeasurement,
  useDeleteMeasurement,
  useDeleteMetric,
  useMeasurements,
  useMetrics,
  useSeries,
  useUpdateMetric,
} from './api'
import { AGGREGATION, formatValue } from './labels'
import { MetricForm } from './MetricasPage'

const shortDay = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' })
const RANGES = [7, 30, 90]

function Chart({ metric, days }: { metric: Metric; days: number }) {
  const to = todayIso()
  const from = addDaysIso(to, -(days - 1))
  const series = useSeries(metric.id, from, to)
  const format = (v: number) => formatValue(v, metric.unit)
  const reference =
    metric.target != null
      ? {
          value: metric.target,
          label: `${metric.targetKind === 'MINIMO' ? 'meta' : 'limite'} ${format(metric.target)}`,
        }
      : undefined
  return (
    <Loaded query={series}>
      {(data) =>
        metric.aggregation === 'SOMA' ? (
          <ColumnChart
            caption={`${metric.name} por dia`}
            format={format}
            reference={reference}
            data={data.map((d) => ({
              key: d.date,
              label: shortDay.format(parseLocalDate(d.date)),
              value: d.value ?? 0,
            }))}
          />
        ) : (
          <LineChart
            caption={`${metric.name} por dia`}
            format={format}
            reference={reference}
            showEvery={Math.ceil(days / 6)}
            data={data.map((d) => ({
              key: d.date,
              label: shortDay.format(parseLocalDate(d.date)),
              value: d.value ?? null,
            }))}
          />
        )
      }
    </Loaded>
  )
}

function AddForm({ metric }: { metric: Metric }) {
  const add = useAddMeasurement()
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const when = f.text('when')
    add.mutate(
      {
        metricId: metric.id,
        value: f.number('value') ?? 0,
        measuredAt: when ? localInputToIso(when) : null,
        note: f.text('note'),
      },
      { onSuccess: () => form.reset() },
    )
  }
  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label={`Valor (${metric.unit})`}>
        <Input name="value" type="number" step="0.01" min="0" required inputMode="decimal" />
      </Field>
      <Field label="Quando (vazio = agora)">
        <Input name="when" type="datetime-local" />
      </Field>
      <Field label="Nota">
        <Input name="note" maxLength={200} />
      </Field>
      <ErrorText error={add.error} />
      <Button type="submit" disabled={add.isPending}>
        Registrar
      </Button>
    </form>
  )
}

export function MetricaPage() {
  const { id = '' } = useParams()
  const metrics = useMetrics()
  const [days, setDays] = useState(30)
  const to = todayIso()
  const measurements = useMeasurements(id, addDaysIso(to, -(days - 1)), to)
  const removeMeasurement = useDeleteMeasurement()
  const update = useUpdateMetric()
  const remove = useDeleteMetric()
  const navigate = useNavigate()

  return (
    <Loaded query={metrics}>
      {(list) => {
        const m = list.find((x) => x.id === id)
        if (!m) return <Empty>Métrica não encontrada.</Empty>
        return (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <Link to="/saude/metricas" className="text-muted hover:text-ink" aria-label="Voltar às métricas">
                <ArrowLeft size={18} />
              </Link>
              <h2 className="text-xl font-semibold">{m.name}</h2>
              <span className="text-sm text-muted">
                {AGGREGATION[m.aggregation]} · {m.unit}
              </span>
              <div className="ml-auto flex items-center gap-1">
                <Button
                  variant="ghost"
                  onClick={() =>
                    update.mutate({
                      id: m.id,
                      name: m.name,
                      unit: m.unit,
                      aggregation: m.aggregation,
                      target: m.target,
                      targetKind: m.targetKind,
                      color: m.color,
                      archived: !m.archived,
                    })
                  }
                >
                  {m.archived ? 'Desarquivar' : 'Arquivar'}
                </Button>
                <EditButton label={`Editar ${m.name}`} title="Editar métrica">
                  {(close) => <MetricForm initial={m} onDone={close} />}
                </EditButton>
                <IconButton
                  label={`Apagar ${m.name}`}
                  onClick={() =>
                    confirm(`Apagar “${m.name}” e todos os registros?`) &&
                    remove.mutate(m.id, { onSuccess: () => navigate('/saude/metricas') })
                  }
                >
                  <Trash2 size={16} />
                </IconButton>
              </div>
            </div>
            <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
              <div className="space-y-6">
                <Card>
                  <SectionTitle
                    action={
                      <div className="flex gap-1" role="group" aria-label="Período">
                        {RANGES.map((r) => (
                          <button
                            key={r}
                            type="button"
                            aria-pressed={days === r}
                            onClick={() => setDays(r)}
                            className={`rounded-md px-2 py-1 text-xs ${days === r ? 'bg-accent-soft text-ink' : 'text-muted hover:text-ink'}`}
                          >
                            {r} dias
                          </button>
                        ))}
                      </div>
                    }
                  >
                    Por dia
                  </SectionTitle>
                  <Chart metric={m} days={days} />
                </Card>
                <Card>
                  <SectionTitle>Registros</SectionTitle>
                  <Loaded query={measurements}>
                    {(data) =>
                      data.length === 0 ? (
                        <Empty>Nenhum registro no período.</Empty>
                      ) : (
                        <ul className="divide-y divide-line">
                          {data.map((x) => (
                            <li key={x.id} className="flex items-center gap-3 py-2 text-sm">
                              <span className="w-40 shrink-0 text-muted">
                                {formatWeekdayDate(dayKey(x.measuredAt))} {formatTime(x.measuredAt)}
                              </span>
                              <span className="font-medium tabular-nums">{formatValue(x.value, m.unit)}</span>
                              <span className="min-w-0 flex-1 truncate text-muted">{x.note}</span>
                              <IconButton label="Apagar registro" onClick={() => removeMeasurement.mutate(x.id)}>
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
              {!m.archived && (
                <Card className="self-start">
                  <SectionTitle>Novo registro</SectionTitle>
                  <AddForm metric={m} />
                </Card>
              )}
            </div>
          </div>
        )
      }}
    </Loaded>
  )
}
