import { Archive, ArchiveRestore, Plus, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import type { Account, AccountKind, Card as CardT } from '../api/types'
import { Meter } from '../components/charts'
import { StatusBadge } from '../components/StatusBadge'
import { Button, Card, Empty, ErrorText, Field, IconButton, Input, Loaded, SectionTitle, Select } from '../components/ui'
import { formData } from '../lib/forms'
import { formatDate, formatMoney, todayIso } from '../lib/format'
import {
  useAccounts,
  useCards,
  useCreateAccount,
  useCreateCard,
  useDeleteAccount,
  useDeleteCard,
  useInvoice,
  usePayInvoice,
  useUpdateAccount,
  useUpdateCard,
} from './api'
import { ACCOUNT_KIND, INVOICE_STATUS } from './labels'
import { makeLookup } from './lookup'
import { MonthPicker } from './MonthPicker'

// ---------- Contas ----------

function AccountRow({ a }: { a: Account }) {
  const update = useUpdateAccount()
  const remove = useDeleteAccount()
  const toggleArchive = () =>
    update.mutate({ id: a.id, name: a.name, kind: a.kind, openingBalance: a.openingBalance, archived: !a.archived })
  return (
    <li className={`flex items-center gap-3 py-3 ${a.archived ? 'opacity-60' : ''}`}>
      <div className="min-w-0 flex-1">
        <p className="font-medium">{a.name}</p>
        <p className="text-xs text-muted">
          {ACCOUNT_KIND[a.kind]}
          {a.archived && ' · arquivada'}
        </p>
      </div>
      <div className="text-right">
        <p className="font-semibold tabular-nums">{formatMoney(a.balance)}</p>
        {a.projectedBalance !== a.balance && (
          <p className="text-xs tabular-nums text-muted">previsto {formatMoney(a.projectedBalance)}</p>
        )}
      </div>
      <IconButton label={a.archived ? `Desarquivar ${a.name}` : `Arquivar ${a.name}`} onClick={toggleArchive}>
        {a.archived ? <ArchiveRestore size={15} /> : <Archive size={15} />}
      </IconButton>
      <IconButton
        label={`Apagar ${a.name}`}
        onClick={() => confirm(`Apagar a conta ${a.name}?`) && remove.mutate(a.id)}
      >
        <Trash2 size={15} />
      </IconButton>
      {(remove.error || update.error) && (
        <ErrorText error={remove.error ?? update.error} />
      )}
    </li>
  )
}

function NewAccountForm() {
  const create = useCreateAccount()
  const errors = create.error instanceof ApiError ? create.error.errors : {}
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    create.mutate(
      { name: f.required('name'), kind: f.text('kind') as AccountKind, openingBalance: f.number('openingBalance') },
      { onSuccess: () => form.reset() },
    )
  }
  return (
    <form onSubmit={submit} className="mt-4 grid gap-3 border-t border-line pt-4 sm:grid-cols-[1fr_auto_auto_auto] sm:items-end">
      <Field label="Nova conta" error={errors.name}>
        <Input name="name" placeholder="Ex.: Nubank, Carteira" required maxLength={60} />
      </Field>
      <Field label="Tipo">
        <Select name="kind" defaultValue="CORRENTE">
          {Object.entries(ACCOUNT_KIND).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </Select>
      </Field>
      <Field label="Saldo inicial" error={errors.openingBalance}>
        <Input name="openingBalance" type="number" step="0.01" placeholder="0,00" />
      </Field>
      <Button type="submit" disabled={create.isPending}>
        <Plus size={16} /> Conta
      </Button>
      <div className="sm:col-span-4">
        <ErrorText error={Object.keys(errors).length ? null : create.error} />
      </div>
    </form>
  )
}

// ---------- Cartões e fatura ----------

function InvoicePanel({ card, accounts }: { card: CardT; accounts: Account[] }) {
  const [month, setMonth] = useState(card.currentInvoice.month)
  const invoice = useInvoice(card.id, month)
  const pay = usePayInvoice(card.id, month)
  const lookup = makeLookup(accounts)

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = formData(e.currentTarget)
    pay.mutate({ accountId: f.required('accountId'), amount: f.number('amount'), date: f.text('date') })
  }

  return (
    <div className="mt-3 space-y-3 rounded-lg bg-canvas p-3">
      <MonthPicker value={month} onChange={setMonth} />
      <Loaded query={invoice}>
        {(inv) => {
          const status = INVOICE_STATUS[inv.summary.status]
          return (
            <>
              <div className="flex flex-wrap items-center gap-2 text-sm">
                <StatusBadge tone={status.tone} label={status.label} />
                <span className="text-muted">
                  fecha {formatDate(inv.summary.closingDate)} · vence {formatDate(inv.summary.dueDate)}
                </span>
                <span className="ml-auto font-semibold tabular-nums">{formatMoney(inv.summary.total)}</span>
              </div>
              {inv.items.length === 0 ? (
                <Empty>Fatura sem lançamentos.</Empty>
              ) : (
                <ul className="divide-y divide-line text-sm">
                  {inv.items.map((t) => (
                    <li key={t.id} className="flex gap-2 py-1.5">
                      <span className="w-16 shrink-0 whitespace-nowrap text-muted">{formatDate(t.date)}</span>
                      <span className="min-w-0 flex-1 truncate">
                        {t.kind === 'PAGAMENTO_FATURA' ? `Pagamento (${lookup.account(t.accountId)})` : t.description}
                        {t.installmentTotal && (
                          <span className="text-muted">
                            {' '}
                            · {t.installmentNo}/{t.installmentTotal}
                          </span>
                        )}
                      </span>
                      <span className="tabular-nums">
                        {t.kind === 'PAGAMENTO_FATURA' ? '+ ' : ''}
                        {formatMoney(t.amount)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
              {inv.summary.remaining > 0 && (
                <form onSubmit={submit} className="grid gap-2 sm:grid-cols-[1fr_auto_auto_auto] sm:items-end">
                  <Field label="Pagar com">
                    <Select name="accountId" defaultValue={card.paymentAccountId ?? accounts[0]?.id ?? ''} required>
                      {accounts
                        .filter((a) => !a.archived)
                        .map((a) => (
                          <option key={a.id} value={a.id}>
                            {a.name}
                          </option>
                        ))}
                    </Select>
                  </Field>
                  <Field label="Valor">
                    <Input
                      key={inv.summary.remaining}
                      name="amount"
                      type="number"
                      step="0.01"
                      min="0.01"
                      defaultValue={inv.summary.remaining.toFixed(2)}
                    />
                  </Field>
                  <Field label="Data">
                    <Input name="date" type="date" defaultValue={todayIso()} />
                  </Field>
                  <Button type="submit" disabled={pay.isPending}>
                    Pagar fatura
                  </Button>
                  <div className="sm:col-span-4">
                    <ErrorText error={pay.error} />
                  </div>
                </form>
              )}
            </>
          )
        }}
      </Loaded>
    </div>
  )
}

function CardRow({ card, accounts }: { card: CardT; accounts: Account[] }) {
  const [open, setOpen] = useState(false)
  const update = useUpdateCard()
  const remove = useDeleteCard()
  const inv = card.currentInvoice
  const status = INVOICE_STATUS[inv.status]
  return (
    <li className={`py-3 ${card.archived ? 'opacity-60' : ''}`}>
      <div className="flex flex-wrap items-center gap-3">
        <div className="min-w-0 flex-1">
          <p className="font-medium">{card.name}</p>
          <p className="text-xs text-muted">
            fecha dia {card.closingDay} · vence dia {card.dueDay}
            {card.archived && ' · arquivado'}
          </p>
        </div>
        <div className="text-right text-sm">
          <p className="tabular-nums">
            Fatura atual <strong>{formatMoney(inv.total)}</strong>
          </p>
          <StatusBadge tone={status.tone} label={`${status.label} · vence ${formatDate(inv.dueDate)}`} />
        </div>
        <Button variant="ghost" onClick={() => setOpen((o) => !o)} aria-expanded={open}>
          {open ? 'Fechar' : 'Faturas'}
        </Button>
        <IconButton
          label={card.archived ? `Desarquivar ${card.name}` : `Arquivar ${card.name}`}
          onClick={() =>
            update.mutate({
              id: card.id,
              name: card.name,
              creditLimit: card.creditLimit,
              closingDay: card.closingDay,
              dueDay: card.dueDay,
              paymentAccountId: card.paymentAccountId,
              archived: !card.archived,
            })
          }
        >
          {card.archived ? <ArchiveRestore size={15} /> : <Archive size={15} />}
        </IconButton>
        <IconButton label={`Apagar ${card.name}`} onClick={() => confirm(`Apagar o cartão ${card.name}?`) && remove.mutate(card.id)}>
          <Trash2 size={15} />
        </IconButton>
      </div>
      {card.creditLimit != null && (
        <div className="mt-2 space-y-1">
          <Meter value={card.used} max={card.creditLimit} label={`Limite usado do ${card.name}`} />
          <p className="text-xs tabular-nums text-muted">
            {formatMoney(card.used)} usados · {formatMoney(card.available ?? 0)} disponíveis de {formatMoney(card.creditLimit)}
          </p>
        </div>
      )}
      <ErrorText error={remove.error ?? update.error} />
      {open && <InvoicePanel card={card} accounts={accounts} />}
    </li>
  )
}

function NewCardForm({ accounts }: { accounts: Account[] }) {
  const create = useCreateCard()
  const errors = create.error instanceof ApiError ? create.error.errors : {}
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    create.mutate(
      {
        name: f.required('name'),
        creditLimit: f.number('creditLimit'),
        closingDay: f.number('closingDay') ?? 1,
        dueDay: f.number('dueDay') ?? 1,
        paymentAccountId: f.text('paymentAccountId'),
      },
      { onSuccess: () => form.reset() },
    )
  }
  return (
    <form onSubmit={submit} className="mt-4 grid grid-cols-2 gap-3 border-t border-line pt-4 sm:grid-cols-6 sm:items-end">
      <div className="col-span-2">
        <Field label="Novo cartão" error={errors.name}>
          <Input name="name" required maxLength={60} placeholder="Ex.: Nubank" />
        </Field>
      </div>
      <Field label="Limite" error={errors.creditLimit}>
        <Input name="creditLimit" type="number" step="0.01" min="0.01" />
      </Field>
      <Field label="Fecha dia" error={errors.closingDay}>
        <Input name="closingDay" type="number" min="1" max="28" required />
      </Field>
      <Field label="Vence dia" error={errors.dueDay}>
        <Input name="dueDay" type="number" min="1" max="28" required />
      </Field>
      <Field label="Paga com">
        <Select name="paymentAccountId" defaultValue="">
          <option value="">—</option>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </Select>
      </Field>
      <div className="col-span-2 sm:col-span-6">
        <ErrorText error={Object.keys(errors).length ? null : create.error} />
        <Button type="submit" disabled={create.isPending}>
          <Plus size={16} /> Cartão
        </Button>
      </div>
    </form>
  )
}

export function ContasPage() {
  const accounts = useAccounts()
  const cards = useCards()
  return (
    <div className="space-y-6">
      <Card>
        <SectionTitle>Contas</SectionTitle>
        <Loaded query={accounts}>
          {(list) =>
            list.length === 0 ? (
              <Empty>Nenhuma conta ainda.</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {list.map((a) => (
                  <AccountRow key={a.id} a={a} />
                ))}
              </ul>
            )
          }
        </Loaded>
        <NewAccountForm />
      </Card>
      <Card>
        <SectionTitle>Cartões de crédito</SectionTitle>
        <p className="mb-2 text-xs text-muted">
          Compras feitas a partir do dia de fechamento entram na fatura do mês seguinte.
        </p>
        <Loaded query={cards}>
          {(list) =>
            list.length === 0 ? (
              <Empty>Nenhum cartão ainda.</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {list.map((c) => (
                  <CardRow key={c.id} card={c} accounts={accounts.data ?? []} />
                ))}
              </ul>
            )
          }
        </Loaded>
        <NewCardForm accounts={(accounts.data ?? []).filter((a) => !a.archived)} />
      </Card>
    </div>
  )
}
