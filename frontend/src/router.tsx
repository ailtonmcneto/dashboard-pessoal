import { createBrowserRouter } from 'react-router'
import { AppLayout } from './components/AppLayout'
import { DisciplinaPage } from './estudos/DisciplinaPage'
import { EstudosLayout } from './estudos/EstudosLayout'
import { HorarioPage } from './estudos/HorarioPage'
import { RecursosPage } from './estudos/RecursosPage'
import { RevisoesPage } from './estudos/RevisoesPage'
import { SemestrePage } from './estudos/SemestrePage'
import { SessoesPage } from './estudos/SessoesPage'
import { AgendaPage } from './pages/AgendaPage'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { HojePage } from './pages/HojePage'
import { LoginPage } from './pages/LoginPage'

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
        element: (
          <ComingSoonPage
            title="Finanças"
            phase={3}
            items={['Contas e cartões', 'Lançamentos, parcelas e recorrências', 'Orçamento por categoria']}
          />
        ),
      },
      {
        path: 'saude',
        element: (
          <ComingSoonPage title="Saúde e hábitos" phase={4} items={['Métricas (sono, peso, água…)', 'Hábitos com sequência']} />
        ),
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
