import { createBrowserRouter } from 'react-router'
import { AppLayout } from './components/AppLayout'
import { DisciplinaPage } from './estudos/DisciplinaPage'
import { EstudosLayout } from './estudos/EstudosLayout'
import { HorarioPage } from './estudos/HorarioPage'
import { RecursosPage } from './estudos/RecursosPage'
import { RevisoesPage } from './estudos/RevisoesPage'
import { SemestrePage } from './estudos/SemestrePage'
import { SessoesPage } from './estudos/SessoesPage'
import { CategoriasPage } from './financas/CategoriasPage'
import { ContasPage } from './financas/ContasPage'
import { FinancasLayout } from './financas/FinancasLayout'
import { LancamentosPage } from './financas/LancamentosPage'
import { OrcamentoPage } from './financas/OrcamentoPage'
import { RecorrenciasPage } from './financas/RecorrenciasPage'
import { VisaoGeralPage } from './financas/VisaoGeralPage'
import { AgendaPage } from './pages/AgendaPage'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { HojePage } from './pages/HojePage'
import { LoginPage } from './pages/LoginPage'
import { HabitosPage } from './saude/HabitosPage'
import { MetricaPage } from './saude/MetricaPage'
import { MetricasPage } from './saude/MetricasPage'
import { SaudeLayout } from './saude/SaudeLayout'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HojePage /> },
      { path: 'agenda', element: <AgendaPage /> },
      {
        path: 'estudos',
        element: <EstudosLayout />,
        children: [
          { index: true, element: <SemestrePage /> },
          { path: 'horario', element: <HorarioPage /> },
          { path: 'revisoes', element: <RevisoesPage /> },
          { path: 'cursos', element: <RecursosPage /> },
          { path: 'sessoes', element: <SessoesPage /> },
          { path: 'disciplinas/:id', element: <DisciplinaPage /> },
        ],
      },
      {
        path: 'financas',
        element: <FinancasLayout />,
        children: [
          { index: true, element: <VisaoGeralPage /> },
          { path: 'lancamentos', element: <LancamentosPage /> },
          { path: 'contas', element: <ContasPage /> },
          { path: 'orcamento', element: <OrcamentoPage /> },
          { path: 'recorrencias', element: <RecorrenciasPage /> },
          { path: 'categorias', element: <CategoriasPage /> },
        ],
      },
      {
        path: 'saude',
        element: <SaudeLayout />,
        children: [
          { index: true, element: <HabitosPage /> },
          { path: 'metricas', element: <MetricasPage /> },
          { path: 'metricas/:id', element: <MetricaPage /> },
        ],
      },
      {
        path: 'metas',
        element: (
          <ComingSoonPage title="Metas e planos" phase={5} items={['Metas com marcos', 'Projetos', 'Tarefas']} />
        ),
      },
    ],
  },
])
