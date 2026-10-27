import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'
import { ApiError } from './api/client'
import './index.css'
import { router } from './router'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 4xx não melhora tentando de novo
      retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
    },
  },
})

// Sessão expirou em qualquer chamada → volta para o login.
queryClient.getQueryCache().subscribe((event) => {
  const error = event.query.state.error
  if (event.type === 'updated' && error instanceof ApiError && error.status === 401 && event.query.queryKey[0] !== 'me') {
    queryClient.setQueryData(['me'], null)
  }
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
