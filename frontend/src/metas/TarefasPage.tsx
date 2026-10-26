import { useState } from 'react'
import type { TaskView } from '../api/types'
import { Card } from '../components/ui'
import { TaskList } from './TaskList'

const VIEWS: { value: TaskView; label: string; empty: string }[] = [
  { value: 'HOJE', label: 'Hoje', empty: 'Nada vencendo hoje.' },
  { value: 'SEMANA', label: 'Próximos 7 dias', empty: 'Nada nos próximos 7 dias.' },
  { value: 'ABERTAS', label: 'Todas abertas', empty: 'Nenhuma tarefa aberta.' },
  { value: 'FEITAS', label: 'Feitas', empty: 'Nada concluído nos últimos 30 dias.' },
]

export function TarefasPage() {
  const [view, setView] = useState<TaskView>('HOJE')
  const current = VIEWS.find((v) => v.value === view)!
  return (
    <Card>
      <div role="tablist" aria-label="Visão das tarefas" className="mb-4 inline-flex flex-wrap gap-1 rounded-lg bg-canvas p-1">
        {VIEWS.map((v) => (
          <button
            key={v.value}
            role="tab"
            type="button"
            aria-selected={view === v.value}
            onClick={() => setView(v.value)}
            className={`rounded-md px-3 py-1.5 text-sm font-medium ${view === v.value ? 'bg-surface shadow-sm' : 'text-muted'}`}
          >
            {v.label}
          </button>
        ))}
      </div>
      <p className="mb-2 text-xs text-muted">
        {view === 'HOJE' && 'Vencidas e de hoje. '}
        Tarefas ligadas a metas, projetos e disciplinas aparecem aqui também; crie essas pelas páginas delas.
      </p>
      <TaskList key={view} view={view} empty={current.empty} />
    </Card>
  )
}
