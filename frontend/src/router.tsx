import { createBrowserRouter } from 'react-router'
import { AppLayout } from './components/AppLayout'
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
        element: (
          <ComingSoonPage
            title="Estudos"
            phase={2}
            items={['Semestres e disciplinas', 'Horário semanal e faltas', 'Provas, trabalhos e médias', 'Assuntos, cursos e sessões de estudo']}
          />
        ),
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
