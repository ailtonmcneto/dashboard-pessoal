import { NavLink, Outlet } from 'react-router'
import { PageHeader } from '../components/ui'

const TABS = [
  { to: '/saude', label: 'Hábitos', end: true },
  { to: '/saude/metricas', label: 'Métricas' },
]

export function SaudeLayout() {
  return (
    <>
      <PageHeader title="Saúde e hábitos" subtitle="O que você mede e o que você quer repetir" />
      <nav className="-mx-4 mb-6 flex gap-1 overflow-x-auto border-b border-line px-4 md:mx-0 md:px-0">
        {TABS.map((t) => (
          <NavLink
            key={t.to}
            to={t.to}
            end={t.end}
            className={({ isActive }) =>
              `whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium transition ${
                isActive ? 'border-accent text-ink' : 'border-transparent text-muted hover:text-ink'
              }`
            }
          >
            {t.label}
          </NavLink>
        ))}
      </nav>
      <Outlet />
    </>
  )
}
