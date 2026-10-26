// Gráficos simples em HTML/CSS seguindo a skill de dataviz: série única na cor --color-chart,
// barras ≤ 24px com ponta arredondada de 4px e base reta, linha de base fina, valores em cor
// de texto (nunca na cor do dado), tooltip ao passar o mouse e tabela para leitores de tela.

import { type ReactNode, useState } from 'react'

export type Datum = { key: string; label: string; value: number }

/** Colunas verticais de uma série (ex.: minutos por dia). */
export function ColumnChart({
  data,
  format,
  height = 140,
  caption,
  reference,
}: {
  data: Datum[]
  format: (v: number) => string
  height?: number
  caption: string
  /** Linha fina de referência (ex.: meta), com rótulo. */
  reference?: { value: number; label: string }
}) {
  const max = Math.max(...data.map((d) => d.value), reference?.value ?? 0, 1)
  const peak = data.reduce((best, d) => (d.value > best.value ? d : best), data[0])

  return (
    <figure>
      <div className="relative flex items-end gap-0.5 border-b border-line" style={{ height }} aria-hidden="true">
        {reference && (
          <div
            className="pointer-events-none absolute inset-x-0 border-t border-muted/60"
            style={{ bottom: (reference.value / max) * (height - 20) }}
          >
            <span className="absolute -top-4 right-0 text-[10px] text-muted">{reference.label}</span>
          </div>
        )}
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

export type LinePoint = { key: string; label: string; value: number | null }

/**
 * Linha de uma série ao longo do tempo (ex.: peso). A linha liga os dias que têm registro (medições
 * espaçadas são o normal) e cada registro ganha um ponto. A escala vertical acompanha os dados
 * (não começa no zero), porque o que importa é a tendência.
 */
export function LineChart({
  data,
  format,
  height = 160,
  caption,
  reference,
  showEvery = 5,
}: {
  data: LinePoint[]
  format: (v: number) => string
  height?: number
  caption: string
  reference?: { value: number; label: string }
  /** Mostra o rótulo do eixo a cada N pontos. */
  showEvery?: number
}) {
  const [hover, setHover] = useState<number | null>(null)
  const values = data.map((d) => d.value).filter((v): v is number => v !== null)
  const width = 600
  const pad = 8
  if (values.length === 0) {
    return <p className="text-sm text-muted">Sem registros no período.</p>
  }
  let min = Math.min(...values, reference?.value ?? Infinity)
  let max = Math.max(...values, reference?.value ?? -Infinity)
  if (min === max) {
    min -= 1
    max += 1
  }
  const span = max - min
  min -= span * 0.1
  max += span * 0.1
  const x = (i: number) => (data.length === 1 ? width / 2 : (i / (data.length - 1)) * width)
  const y = (v: number) => pad + (1 - (v - min) / (max - min)) * (height - 2 * pad)

  const path = data
    .map((d, i) => (d.value === null ? null : `${x(i).toFixed(1)},${y(d.value).toFixed(1)}`))
    .filter(Boolean)
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${p}`)
    .join(' ')

  const h = hover !== null ? data[hover] : null
  return (
    <figure>
      <div className="relative" aria-hidden="true">
        <svg viewBox={`0 0 ${width} ${height}`} className="h-auto w-full overflow-visible" preserveAspectRatio="none">
          <line x1={0} x2={width} y1={height - 0.5} y2={height - 0.5} stroke="var(--color-line)" strokeWidth={1} />
          {reference && (
            <line
              x1={0}
              x2={width}
              y1={y(reference.value)}
              y2={y(reference.value)}
              stroke="var(--color-muted)"
              strokeOpacity={0.6}
              strokeWidth={1}
              vectorEffect="non-scaling-stroke"
            />
          )}
          {hover !== null && (
            <line x1={x(hover)} x2={x(hover)} y1={0} y2={height} stroke="var(--color-line)" strokeWidth={1} vectorEffect="non-scaling-stroke" />
          )}
          {values.length > 1 && (
            <path d={path} fill="none" stroke="var(--color-chart)" strokeWidth={2} vectorEffect="non-scaling-stroke" strokeLinejoin="round" />
          )}
          {data.map((d, i) =>
            d.value === null ? null : (
              <circle
                key={d.key}
                cx={x(i)}
                cy={y(d.value)}
                r={hover === i ? 5 : 3}
                fill="var(--color-chart)"
                stroke="var(--color-surface)"
                strokeWidth={2}
                vectorEffect="non-scaling-stroke"
              />
            ),
          )}
          {/* áreas de hover maiores que o ponto */}
          {data.map((d, i) => (
            <rect
              key={`hit-${d.key}`}
              x={x(i) - width / data.length / 2}
              y={0}
              width={width / data.length}
              height={height}
              fill="transparent"
              onMouseEnter={() => setHover(i)}
              onMouseLeave={() => setHover(null)}
            />
          ))}
        </svg>
        {reference && (
          <span className="absolute right-0 text-[10px] text-muted" style={{ top: `calc(${(y(reference.value) / height) * 100}% - 14px)` }}>
            {reference.label}
          </span>
        )}
        {h && (
          <div
            className="pointer-events-none absolute top-0 z-10 -translate-x-1/2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-xs text-white shadow"
            style={{ left: `${(x(hover!) / width) * 100}%` }}
          >
            {h.label}: {h.value === null ? 'sem registro' : format(h.value)}
          </div>
        )}
      </div>
      <div className="mt-1 flex justify-between text-[11px] text-muted" aria-hidden="true">
        {data.map((d, i) => (i % showEvery === 0 || i === data.length - 1 ? <span key={d.key}>{d.label}</span> : null))}
      </div>
      <SrTable
        caption={caption}
        data={data.filter((d) => d.value !== null).map((d) => ({ key: d.key, label: d.label, value: d.value! }))}
        format={format}
      />
    </figure>
  )
}
