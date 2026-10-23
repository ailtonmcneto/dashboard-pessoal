import { BookOpen, CalendarDays, HeartPulse, LogOut, Sun, Target, Wallet } from 'lucide-react'
import { Navigate, NavLink, Outlet } from 'react-router'
import { useLogout, useMe } from '../auth/auth'

const LINKS = [
  { to: '/', label: 'Hoje', icon: Sun },
  { to: '/estudos', label: 'Estudos', icon: BookOpen },
  { to: '/financas', label: 'Finanças', icon: Wallet },
  { to: '/saude', label: 'Saúde e hábitos', short: 'Saúde', icon: HeartPulse },
  { to: '/metas', label: 'Metas e planos', short: 'Metas', icon: Target },
  { to: '/agenda', label: 'Agenda', icon: CalendarDays },
]

export function AppLayout() {
  const me = useMe()
  const logout = useLogout()

  if (me.isPending) return <p className="p-8 text-sm text-muted">Carregando…</p>
  if (me.isError) return <p className="p-8 text-sm text-danger">Não foi possível falar com a API.</p>
  if (!me.data) return <Navigate to="/login" replace />

  const signOut = (
    <button
      onClick={() => logout.mutate()}
      className="flex items-center gap-2 text-sm text-muted hover:text-ink"
    >
      <LogOut size={15} /> Sair
    </button>
  )

  return (
    <div className="mx-auto flex max-w-7xl gap-8 px-4 md:px-6">
      <aside className="sticky top-0 hidden h-screen w-56 shrink-0 flex-col py-8 md:flex">
        <p className="mb-8 px-3 text-2xl font-bold tracking-tight">Vida</p>
        <nav className="space-y-1">
          {LINKS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  isActive ? 'bg-surface text-ink shadow-[inset_3px_0_0_var(--color-accent)]' : 'text-muted hover:text-ink'
                }`
              }
            >
              <Icon size={18} strokeWidth={1.75} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto space-y-2 px-3">
          <p className="truncate text-xs text-muted" title={me.data.email}>
            {me.data.name}
          </p>
          {signOut}
        </div>
      </aside>

      <main className="min-w-0 flex-1 pb-28 pt-6 md:pb-12 md:pt-8">
        <div className="mb-6 flex items-center justify-between md:hidden">
          <p className="text-xl font-bold tracking-tight">Vida</p>
          {signOut}
        </div>
        <Outlet />
      </main>

      <nav className="fixed inset-x-0 bottom-0 z-10 grid grid-cols-6 border-t border-line bg-surface md:hidden">
        {LINKS.map(({ to, label, short, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex flex-col items-center gap-1 py-2 text-[11px] ${isActive ? 'text-accent' : 'text-muted'}`
            }
          >
            <Icon size={20} strokeWidth={1.75} />
            {short ?? label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
