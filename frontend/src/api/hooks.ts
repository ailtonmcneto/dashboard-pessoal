import { type QueryKey, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

/** GET simples; a chave do cache é o próprio caminho (prefixos permitem invalidar em grupo). */
export function useGet<T>(path: string | null, key?: QueryKey) {
  const [route, query = ''] = (path ?? '').split('?')
  return useQuery({
    queryKey: key ?? ['api', ...route.split('/').filter(Boolean), query],
    queryFn: () => api<T>(path!),
    enabled: path !== null,
  })
}

type Method = 'POST' | 'PUT' | 'DELETE'

/**
 * Mutação que, ao dar certo, invalida as consultas cujo caminho começa com algum dos prefixos
 * (ex.: ['courses', 'terms'] recarrega /courses/... e /terms/...).
 */
export function useSend<Body, Res = unknown>(
  method: Method,
  path: (body: Body) => string,
  invalidate: string[],
) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: Body) => api<Res>(path(body), method, method === 'DELETE' ? undefined : body),
    onSuccess: () =>
      Promise.all(
        invalidate.map((prefix) => qc.invalidateQueries({ queryKey: ['api', prefix], refetchType: 'all' })),
      ),
  })
}
