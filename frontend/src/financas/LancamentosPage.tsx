import { Check, Trash2 } from 'lucide-react'
import { useState } from 'react'
import type { Transaction } from '../api/types'
import { EditButton } from '../components/Modal'
import { Card, Empty, IconButton, Loaded, SectionTitle, Select } from '../components/ui'
import { ColorDot } from '../estudos/parts'
import { formatMoney, formatWeekdayDate } from '../lib/format'
import {
  useAccounts,
  useCards,
  useCategories,
  useDeleteTransaction,
  useTransactions,
  useUpdateTransaction,
} from './api'
import { categoryLabel, categoryOptions } from './labels'
import { makeLookup } from './lookup'
import { MonthPicker } from './MonthPicker'
import { useFinanceMonth } from './month'
import { TransactionForm } from './TransactionForm'

/** Sinal do valor do ponto de vista do usuário: entra (+), sai (−) ou só muda de lugar (⇄). */
function signed(t: Transaction) {
  if (t.kind === 'RECEITA') return `+ ${formatMoney(t.amount)}`
  if (t.kind === 'TRANSFERENCIA') return `⇄ ${formatMoney(t.amount)}`
  return `− ${formatMoney(t.amount)}`
}

export function LancamentosPage() {
  const { month, setMonth } = useFinanceMonth()
  const [where, setWhere] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [type, id] = where.split(':')
  const list = useTransactions(month, {
    accountId: type === 'a' ? id : undefined,
    cardId: type === 'c' ? id : undefined,
    categoryId: categoryId || undefined,
  })
  const accounts = useAccounts().data ?? []
  const cards = useCards().data ?? []
  const categories = useCategories().data ?? []
  const lookup = makeLookup(accounts, cards, categories)
  const update = useUpdateTransaction()
  const remove = useDeleteTransaction()

  function markPaid(t: Transaction) {
    update.mutate({
      id: t.id,
      kind: t.kind,
      date: t.date,
      description: t.description,
      amount: t.amount,
      categoryId: t.categoryId,
      accountId: t.accountId,
      cardId: t.cardId,
      toAccountId: t.toAccountId,
      notes: t.notes,
      paid: true,
    })
  }

  function del(t: Transaction) {
    if (t.installmentTotal) {
      const all = confirm(
        `“${t.description}” é parcela ${t.installmentNo}/${t.installmentTotal}.\nOK apaga todas as parcelas; Cancelar apaga só esta.`,
      )
      remove.mutate({ id: t.id, allInstallments: all })
    } else if (confirm(`Apagar “${t.description}”?`)) {
      remove.mutate({ id: t.id, allInstallments: false })
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <MonthPicker value={month} onChange={setMonth} />
          <div className="w-40">
            <Select aria-label="Filtrar por conta ou cartão" value={where} onChange={(e) => setWhere(e.target.value)}>
              <option value="">Todas as contas</option>
              {accounts.map((a) => (
                <option key={a.id} value={`a:${a.id}`}>
                  {a.name}
                </option>
              ))}
              {cards.map((c) => (
                <option key={c.id} value={`c:${c.id}`}>
                  {c.name}
                </option>
              ))}
            </Select>
          </div>
          <div className="w-44">
            <Select
              aria-label="Filtrar por categoria"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
            >
              <option value="">Todas as categorias</option>
              {[...categoryOptions(categories, 'DESPESA'), ...categoryOptions(categories, 'RECEITA')].map((c) => (
                <option key={c.id} value={c.id}>
                  {categoryLabel(c, categories)}
                </option>
              ))}
            </Select>
          </div>
        </div>
        <Card>
          <Loaded query={list}>
            {(data) => {
              if (data.length === 0) return <Empty>Nenhum lançamento neste mês.</Empty>
              const days = [...new Set(data.map((t) => t.date))]
              return (
                <div className="space-y-5">
                  {days.map((day) => (
                    <section key={day}>
                      <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">
                        {formatWeekdayDate(day)}
                      </h3>
                      <ul className="divide-y divide-line">
                        {data
                          .filter((t) => t.date === day)
                          .map((t) => {
                            const cat = lookup.category(t.categoryId)
                            return (
                              <li key={t.id} className="flex items-center gap-3 py-2">
                                <ColorDot color={cat?.color ?? '#d6d3d1'} />
                                <div className="min-w-0 flex-1">
                                  <p className="truncate text-sm font-medium">{t.description}</p>
                                  <p className="truncate text-xs text-muted">
                                    {[cat?.label, lookup.place(t)].filter(Boolean).join(' · ')}
                                  </p>
                                </div>
                                {!t.paid && (
                                  <button
                                    type="button"
                                    onClick={() => markPaid(t)}
                                    className="inline-flex items-center gap-1 rounded-full border border-line px-2 py-0.5 text-xs hover:border-accent hover:text-accent"
                                    title="Marcar como pago"
                                  >
                                    <Check size={12} /> Pendente
                                  </button>
                                )}
                                <span className="w-28 text-right text-sm tabular-nums">{signed(t)}</span>
                                {t.kind !== 'PAGAMENTO_FATURA' && (
                                  <EditButton label={`Editar ${t.description}`} title="Editar lançamento">
                                    {(close) => <TransactionForm initial={t} onDone={close} />}
                                  </EditButton>
                                )}
                                <IconButton label={`Apagar ${t.description}`} onClick={() => del(t)}>
                                  <Trash2 size={14} />
                                </IconButton>
                              </li>
                            )
                          })}
                      </ul>
                    </section>
                  ))}
                </div>
              )
            }}
          </Loaded>
        </Card>
      </div>
      <Card className="self-start">
        <SectionTitle>Novo lançamento</SectionTitle>
        <TransactionForm />
      </Card>
    </div>
  )
}
