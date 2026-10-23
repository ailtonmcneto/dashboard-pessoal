import { AlertTriangle } from 'lucide-react'
import type { Budget } from '../api/types'
import { Link } from 'react-router'
import { BarList, GroupedColumns, Meter } from '../components/charts'
import { StatusBadge } from '../components/StatusBadge'
import { Button, Card, Empty, ErrorText, Loaded, SectionTitle } from '../components/ui'
import { ColorDot } from '../estudos/parts'
import { formatMoney, formatMoneyCompact, formatMonthShort } from '../lib/format'
import { useAccounts, useBudgets, useCategories, useDefaultCategories, useHistory, useMonthSummary } from './api'
import { BUDGET_STATUS } from './labels'
import { MonthPicker } from './MonthPicker'
import { useFinanceMonth } from './month'

function Tile({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <Card>
      <p className="text-sm text-muted">{label}</p>
      <p className="text-xl font-bold tabular-nums sm:text-2xl">{value}</p>
      {hint && <p className="mt-1 text-xs text-muted">{hint}</p>}
    </Card>
  )
}

/** Primeiros passos: sem conta ou sem categoria, o resto não faz sentido ainda. */
function Onboarding() {
  const accounts = useAccounts()
  const categories = useCategories()
  const defaults = useDefaultCategories()
  if (!accounts.data || !categories.data) return null
  if (accounts.data.length > 0 && categories.data.length > 0) return null
  return (
    <Card className="border-accent/40 bg-accent-soft/40">
      <SectionTitle>Primeiros passos</SectionTitle>
      <ol className="list-inside list-decimal space-y-2 text-sm">
        <li className={accounts.data.length > 0 ? 'text-muted line-through' : ''}>
          Cadastre suas contas e cartões em{' '}
          <Link to="/financas/contas" className="text-accent hover:underline">
            Contas e cartões
          </Link>
          .
        </li>
        <li className={categories.data.length > 0 ? 'text-muted line-through' : ''}>
          Crie as categorias de receita e despesa.{' '}
          {categories.data.length === 0 && (
            <Button variant="ghost" className="ml-1 px-2 py-0.5" onClick={() => defaults.mutate()} disabled={defaults.isPending}>
              Usar sugestões
            </Button>
          )}
        </li>
      </ol>
      <ErrorText error={defaults.error} />
    </Card>
  )
}

export function VisaoGeralPage() {
  const { month, setMonth } = useFinanceMonth()
  const summary = useMonthSummary(month)
  const history = useHistory(month)
  const budgets = useBudgets(month)

  return (
    <div className="space-y-6">
      <MonthPicker value={month} onChange={setMonth} />
      <Onboarding />
      <Loaded query={summary}>
        {(s) => (
          <>
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
              <Tile label="Receitas" value={formatMoney(s.income)} />
              <Tile label="Despesas" value={formatMoney(s.expense)} />
              <Tile label="Resultado do mês" value={formatMoney(s.net)} hint={s.net < 0 ? 'Gastou mais do que recebeu' : undefined} />
              <Tile label="Saldo em contas" value={formatMoney(s.totalBalance)} hint="hoje, só o que já foi pago" />
            </div>
            {s.pendingCount > 0 && (
              <Link
                to="/financas/lancamentos"
                className="flex items-center gap-2 rounded-xl border border-line bg-surface p-4 text-sm hover:border-accent/50"
              >
                <AlertTriangle size={16} className="text-warning" aria-hidden="true" />
                <span>
                  <strong>{s.pendingCount}</strong> {s.pendingCount === 1 ? 'conta a pagar' : 'contas a pagar'} até o fim
                  do mês, somando <strong>{formatMoney(s.pendingAmount)}</strong>
                </span>
              </Link>
            )}
            <div className="grid gap-6 lg:grid-cols-2">
              <Card>
                <SectionTitle>Despesas por categoria</SectionTitle>
                {s.expenseByCategory.length === 0 ? (
                  <Empty>Nenhuma despesa no mês.</Empty>
                ) : (
                  <BarList
                    caption="Despesas do mês por categoria"
                    format={formatMoney}
                    data={s.expenseByCategory.map((c) => ({ key: c.categoryId ?? 'none', label: c.name, value: c.amount }))}
                    marker={(d) => (
                      <ColorDot color={s.expenseByCategory.find((c) => (c.categoryId ?? 'none') === d.key)!.color} />
                    )}
                  />
                )}
              </Card>
              <Card>
                <SectionTitle>Últimos 6 meses</SectionTitle>
                <Loaded query={history}>
                  {(h) => (
                    <GroupedColumns
                      caption="Receitas e despesas nos últimos 6 meses"
                      format={formatMoneyCompact}
                      series={[
                        { key: 'income', label: 'Receitas', color: 'var(--color-income)' },
                        { key: 'expense', label: 'Despesas', color: 'var(--color-expense)' },
                      ]}
                      data={h.map((m) => ({
                        key: m.month,
                        label: formatMonthShort(m.month),
                        values: { income: m.income, expense: m.expense },
                      }))}
                    />
                  )}
                </Loaded>
              </Card>
            </div>
          </>
        )}
      </Loaded>
      <Loaded query={budgets}>
        {(list) =>
          list.length > 0 && (
            <Card>
              <SectionTitle action={<Link to="/financas/orcamento" className="text-sm text-accent hover:underline">Orçamento</Link>}>
                Orçamento do mês
              </SectionTitle>
              <BudgetMeters budgets={list.slice(0, 4)} />
            </Card>
          )
        }
      </Loaded>
    </div>
  )
}

export function BudgetMeters({ budgets }: { budgets: Budget[] }) {
  const categories = useCategories()
  return (
    <ul className="space-y-4">
      {budgets.map((b) => {
        const status = BUDGET_STATUS[b.status]
        const name = categories.data?.find((c) => c.id === b.categoryId)?.name ?? '…'
        return (
          <li key={b.id} className="space-y-1.5">
            <div className="flex flex-wrap items-center gap-2 text-sm">
              <span className="font-medium">{name}</span>
              <StatusBadge tone={status.tone} label={status.label} />
              <span className="ml-auto tabular-nums text-muted">
                {formatMoney(b.spent)} de {formatMoney(b.limitAmount)} · {b.percent}%
              </span>
            </div>
            <Meter
              value={b.spent}
              max={b.limitAmount}
              tone={b.status === 'ESTOURADO' ? 'critical' : b.status === 'ATENCAO' ? 'warning' : 'chart'}
              label={`${name}: ${b.percent}% do orçamento`}
            />
          </li>
        )
      })}
    </ul>
  )
}
