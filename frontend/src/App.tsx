import { useQuery } from '@tanstack/react-query'

function App() {
  const health = useQuery({
    queryKey: ['health'],
    queryFn: async () => {
      const res = await fetch('/api/actuator/health')
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      return (await res.json()) as { status: string }
    },
    retry: false,
  })

  return (
    <main className="mx-auto max-w-xl p-8">
      <h1 className="text-3xl font-bold tracking-tight">Vida</h1>
      <p className="mt-2 text-gray-600">Dashboard pessoal — estudos, finanças, saúde e metas.</p>
      <p className="mt-6 text-sm">
        API:{' '}
        {health.isPending ? 'verificando…' : health.isError ? 'fora do ar' : health.data.status}
      </p>
    </main>
  )
}

export default App
