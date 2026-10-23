// Gráficos simples em HTML/CSS seguindo a skill de dataviz: série única na cor --color-chart,
// barras ≤ 24px com ponta arredondada de 4px e base reta, linha de base fina, valores em cor
// de texto (nunca na cor do dado), tooltip ao passar o mouse e tabela para leitores de tela.

import type { ReactNode } from 'react'

export type Datum = { key: string; label: string; value: number }

/** Colunas verticais de uma série (ex.: minutos por dia). */
export function ColumnChart({
  data,
  format,
  height = 140,
  caption,
}: {
  data: Datum[]
  format: (v: number) => string
  height?: number
  caption: string
}) {
  const max = Math.max(...data.map((d) => d.value), 1)
  const peak = data.reduce((best, d) => (d.value > best.value ? d : best), data[0])

  return (
    <figure>
      <div className="flex items-end gap-0.5 border-b border-line" style={{ height }} aria-hidden="true">
        {data.map((d) => {
          const h = d.value === 0 ? 0 : Math.max(2, (d.value / max) * (height - 20))
          return (
            <div key={d.key} className="group relative flex h-full flex-1 flex-col items-center justify-end">
              {/* rótulo seletivo: só no pico */}
              {d === peak && d.value > 0 && (
                <span className="mb-1 text-[11px] tabular-nums text-muted">{format(d.value)}</span>
              )}
              <div
                className="w-full max-w-6 rounded-t bg-chart transition-opacity group-hover:opacity-80"
                style={{ height: h }}
              />
              <div className="pointer-events-none absolute bottom-full z-10 mb-1 hidden whitespace-nowrap rounded-md bg-ink px-2 py-1 text-xs text-white shadow group-hover:block">
                {d.label}: {format(d.value)}
              </div>
            </div>
          )
        })}
      </div>
      <div className="mt-1 flex gap-0.5" aria-hidden="true">
        {data.map((d) => (
          <span key={d.key} className="flex-1 truncate text-center text-[11px] text-muted">
            {d.label}
          </span>
        ))}
      </div>
      <SrTable caption={caption} data={data} format={format} />
    </figure>
  )
}

/** Barras horizontais com rótulo e valor na ponta (ex.: minutos por disciplina). */
export function BarList({
  data,
  format,
  caption,
  marker,
}: {
  data: Datum[]
  format: (v: number) => string
  caption: string
  marker?: (d: Datum) => ReactNode
}) {
  const max = Math.max(...data.map((d) => d.value), 1)
  return (
    <figure>
      <ul className="space-y-2.5" aria-hidden="true">
        {data.map((d) => (
          <li key={d.key} className="group" title={`${d.label}: ${format(d.value)}`}>
            <div className="mb-1 flex items-center gap-2 text-sm">
              {marker?.(d)}
              <span className="min-w-0 flex-1 truncate">{d.label}</span>
              <span className="tabular-nums text-muted">{format(d.value)}</span>
            </div>
            <div className="h-2.5 rounded-r bg-canvas">
              <div
                className="h-full rounded-r bg-chart group-hover:opacity-80"
                style={{ width: `${(d.value / max) * 100}%` }}
              />
            </div>
          </li>
        ))}
      </ul>
      <SrTable caption={caption} data={data} format={format} />
    </figure>
  )
}

/** Medidor de uma razão contra um limite (progresso, faltas usadas). */
export function Meter({ value, max, tone = 'chart', label }: {
  value: number
  max: number
  tone?: 'chart' | 'warning' | 'critical'
  label: string
}) {
  const pct = max <= 0 ? (value > 0 ? 100 : 0) : Math.min(100, (value / max) * 100)
  const color = { chart: 'bg-chart', warning: 'bg-warning', critical: 'bg-critical' }[tone]
  return (
    <div
      role="meter"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
      className="h-2 overflow-hidden rounded-full bg-canvas"
    >
      <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
    </div>
  )
}

function SrTable({ caption, data, format }: { caption: string; data: Datum[]; format: (v: number) => string }) {
  return (
    <table className="sr-only">
      <caption>{caption}</caption>
      <tbody>
        {data.map((d) => (
          <tr key={d.key}>
            <th scope="row">{d.label}</th>
            <td>{format(d.value)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export type Series = { key: string; label: string; color: string }
export type GroupDatum = { key: string; label: string; values: Record<string, number> }

/**
 * Colunas agrupadas (ex.: receitas × despesas por mês). Legenda sempre visível; cada grupo mostra
 * os valores no tooltip; tabela para leitor de tela.
 */
export function GroupedColumns({
  data,
  series,
  format,
  height = 160,
  caption,
}: {
  data: GroupDatum[]
  series: Series[]
  format: (v: number) => string
  height?: number
  caption: string
}) {
  const max = Math.max(...data.flatMap((d) => series.map((s) => d.values[s.key] ?? 0)), 1)
  return (
    <figure>
      <ul className="mb-3 flex flex-wrap gap-4 text-xs text-muted">
        {series.map((s) => (
          <li key={s.key} className="flex items-center gap-1.5">
            <span className="size-2.5 rounded-sm" style={{ background: s.color }} aria-hidden="true" />
            {s.label}
          </li>
        ))}
      </ul>
      <div className="flex items-end gap-3 border-b border-line" style={{ height }} aria-hidden="true">
        {data.map((d) => (
          <div key={d.key} className="group relative flex h-full flex-1 items-end justify-center gap-0.5">
            {series.map((s) => {
              const v = d.values[s.key] ?? 0
              return (
                <div
                  key={s.key}
                  className="w-full max-w-5 rounded-t group-hover:opacity-80"
                  style={{ height: v === 0 ? 0 : Math.max(2, (v / max) * (height - 8)), background: s.color }}
                />
              )
            })}
            <div className="pointer-events-none absolute bottom-full z-10 mb-1 hidden whitespace-nowrap rounded-md bg-ink px-2 py-1 text-xs text-white shadow group-hover:block">
              <p className="font-medium">{d.label}</p>
              {series.map((s) => (
                <p key={s.key}>
                  {s.label}: {format(d.values[s.key] ?? 0)}
                </p>
              ))}
            </div>
          </div>
        ))}
      </div>
      <div className="mt-1 flex gap-3" aria-hidden="true">
        {data.map((d) => (
          <span key={d.key} className="flex-1 truncate text-center text-[11px] text-muted">
            {d.label}
          </span>
        ))}
      </div>
      <table className="sr-only">
        <caption>{caption}</caption>
        <thead>
          <tr>
            <th scope="col">Período</th>
            {series.map((s) => (
              <th key={s.key} scope="col">
                {s.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((d) => (
            <tr key={d.key}>
              <th scope="row">{d.label}</th>
              {series.map((s) => (
                <td key={s.key}>{format(d.values[s.key] ?? 0)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  )
}
