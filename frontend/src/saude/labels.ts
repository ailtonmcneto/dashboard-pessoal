import type { Aggregation, Habit, Metric, MetricStatus } from '../api/types'
import type { Tone } from '../components/StatusBadge'

export const AGGREGATION: Record<Aggregation, string> = {
  SOMA: 'Soma do dia',
  MEDIA: 'Média do dia',
  ULTIMO: 'Último do dia',
}

export const METRIC_STATUS: Record<MetricStatus, { label: string; tone: Tone }> = {
  SEM_DADOS: { label: 'Sem registro hoje', tone: 'neutral' },
  SEM_META: { label: 'Registrado', tone: 'neutral' },
  ATINGIDA: { label: 'Meta atingida', tone: 'good' },
  NAO_ATINGIDA: { label: 'Abaixo da meta', tone: 'warning' },
}

/** Para meta máxima, "não atingida" significa que passou do limite. */
export function statusFor(m: Metric): { label: string; tone: Tone } {
  if (m.todayStatus === 'NAO_ATINGIDA' && m.targetKind === 'MAXIMO') {
    return { label: 'Acima do limite', tone: 'warning' }
  }
  return METRIC_STATUS[m.todayStatus]
}

const WEEKDAY_SHORT = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

export function scheduleLabel(h: Habit): string {
  if (h.schedule === 'DIARIO') return 'Todo dia'
  if (h.schedule === 'X_POR_SEMANA') return `${h.timesPerWeek}× por semana`
  return h.weekdays.map((d) => WEEKDAY_SHORT[d - 1]).join(', ')
}

export const WEEKDAYS_SHORT = WEEKDAY_SHORT

const num = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
export const formatValue = (v: number, unit: string) => `${num.format(v)} ${unit}`
