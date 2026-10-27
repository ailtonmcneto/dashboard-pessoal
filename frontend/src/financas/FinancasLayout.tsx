import { useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { PageHeader } from '../components/ui'
import { currentMonth } from '../lib/format'
import type { MonthContext } from './month'

const TABS = [
  { to: '/financas', label: 'Visão geral', end: true },
  { to: '/financas/lancamentos', label: 'Lançamentos' },
  { to: '/financas/contas', label: 'Contas e cartões' },
  { to: '/financas/orcamento', label: 'Orçamento' },
  { to: '/financas/recorrencias', label: 'Recorrências' },
  { to: '/financas/categorias', label: 'Categorias' },
]

export function FinancasLayout() {
  const [month, setMonth] = useState(currentMonth)
  return (
    <>
      <PageHeader title="Finanças" subtitle="Contas, cartões, lançamentos e orçamento" />
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
      <Outlet context={{ month, setMonth } satisfies MonthContext} />
    </>
  )
}
