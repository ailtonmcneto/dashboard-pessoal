import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { EventItem, EventKind, EventRequest } from '../api/types'

export const KIND_LABEL: Record<EventKind, string> = {
  COMPROMISSO: 'Compromisso',
  CONSULTA: 'Consulta',
  OUTRO: 'Outro',
}

/** Eventos dos próximos `days` dias a partir de agora. */
export function useUpcomingEvents(days: number) {
  return useQuery({
    queryKey: ['events', 'upcoming', days],
    queryFn: () => {
      const from = new Date()
      const to = new Date(from.getTime() + days * 24 * 60 * 60 * 1000)
      const qs = new URLSearchParams({ from: from.toISOString(), to: to.toISOString() })
      return api<EventItem[]>(`/events?${qs}`)
    },
  })
}

export function useCreateEvent() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (req: EventRequest) => api<EventItem>('/events', 'POST', req),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['events'], refetchType: 'all' }),
  })
}

export function useDeleteEvent() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/events/${id}`, 'DELETE'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['events'], refetchType: 'all' }),
  })
}
