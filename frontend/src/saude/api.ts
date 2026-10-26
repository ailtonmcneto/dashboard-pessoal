import { useGet, useSend } from '../api/hooks'
import type {
  DayValue,
  Habit,
  HabitRequest,
  Measurement,
  MeasurementRequest,
  Metric,
  MetricRequest,
} from '../api/types'

const ALL = ['metrics', 'measurements', 'habits']

type WithId<T> = T & { id: string }

export const useMetrics = () => useGet<Metric[]>('/metrics')
export const useSeries = (id: string, from: string, to: string) =>
  useGet<DayValue[]>(`/metrics/${id}/series?from=${from}&to=${to}`)
export const useMeasurements = (id: string, from: string, to: string) =>
  useGet<Measurement[]>(`/metrics/${id}/measurements?from=${from}&to=${to}`)
export const useHabits = () => useGet<Habit[]>('/habits')
export const useHabitHistory = (id: string | null, from: string, to: string) =>
  useGet<string[]>(id ? `/habits/${id}/history?from=${from}&to=${to}` : null)

export const useCreateMetric = () => useSend<MetricRequest>('POST', () => '/metrics', ALL)
export const useUpdateMetric = () => useSend<WithId<MetricRequest>>('PUT', (m) => `/metrics/${m.id}`, ALL)
export const useDeleteMetric = () => useSend<string>('DELETE', (id) => `/metrics/${id}`, ALL)
export const useDefaultMetrics = () => useSend<void>('POST', () => '/metrics/defaults', ALL)
export const useAddMeasurement = () =>
  useSend<MeasurementRequest & { metricId: string }>('POST', (m) => `/metrics/${m.metricId}/measurements`, ALL)
export const useDeleteMeasurement = () => useSend<string>('DELETE', (id) => `/measurements/${id}`, ALL)

export const useCreateHabit = () => useSend<HabitRequest>('POST', () => '/habits', ALL)
export const useUpdateHabit = () => useSend<WithId<HabitRequest>>('PUT', (h) => `/habits/${h.id}`, ALL)
export const useDeleteHabit = () => useSend<string>('DELETE', (id) => `/habits/${id}`, ALL)
export const useCheck = () => useSend<{ id: string; date: string }>('PUT', (c) => `/habits/${c.id}/checks/${c.date}`, ALL)
export const useUncheck = () =>
  useSend<{ id: string; date: string }>('DELETE', (c) => `/habits/${c.id}/checks/${c.date}`, ALL)
