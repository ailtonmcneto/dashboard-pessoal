import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'
import type { AuthStatus, LoginRequest, Me, RegisterRequest } from '../api/types'

/** Usuário logado, ou null se não houver sessão. */
export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      try {
        return await api<Me>('/auth/me')
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null
        throw e
      }
    },
    staleTime: Infinity,
    retry: false,
  })
}

export function useAuthStatus() {
  return useQuery({ queryKey: ['auth-status'], queryFn: () => api<AuthStatus>('/auth/status') })
}

export function useLogin() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (req: LoginRequest) => api<Me>('/auth/login', 'POST', req),
    onSuccess: (me) => qc.setQueryData(['me'], me),
  })
}

export function useRegister() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (req: RegisterRequest) => api<Me>('/auth/register', 'POST', req),
    onSuccess: (me) => qc.setQueryData(['me'], me),
  })
}

export function useLogout() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => api<void>('/auth/logout', 'POST'),
    onSuccess: () => {
      qc.clear()
      qc.setQueryData(['me'], null)
    },
  })
}
