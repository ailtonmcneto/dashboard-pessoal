import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import { Button, ErrorText, Field, Input, Select } from '../components/ui'
import { formData } from '../lib/forms'
import { todayIso } from '../lib/format'
import { useAccounts, useCards, useCategories, useCreateTransaction } from './api'
import { categoryLabel, categoryOptions } from './labels'

type Kind = 'DESPESA' | 'RECEITA' | 'TRANSFERENCIA'

const KINDS: { value: Kind; label: string }[] = [
  { value: 'DESPESA', label: 'Despesa' },
  { value: 'RECEITA', label: 'Receita' },
  { value: 'TRANSFERENCIA', label: 'Transferência' },
]

export function TransactionForm() {
  const create = useCreateTransaction()
  const accounts = (useAccounts().data ?? []).filter((a) => !a.archived)
  const cards = (useCards().data ?? []).filter((c) => !c.archived)
  const allCategories = useCategories().data ?? []
  const [kind, setKind] = useState<Kind>('DESPESA')
  const [source, setSource] = useState('') // 'a:<id>' conta | 'c:<id>' cartão
  const errors = create.error instanceof ApiError ? create.error.errors : {}
  const onCard = source.startsWith('c:')

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const [type, id] = (f.text('source') ?? ':').split(':')
    create.mutate(
      {
        kind,
        description: f.required('description'),
        amount: f.number('amount') ?? 0,
        date: f.required('date'),
        categoryId: kind === 'TRANSFERENCIA' ? null : f.text('categoryId'),
        accountId: type === 'a' ? id : null,
        cardId: type === 'c' ? id : null,
        toAccountId: kind === 'TRANSFERENCIA' ? f.text('toAccountId') : null,
        installments: onCard ? f.number('installments') : null,
        paid: onCard || kind === 'TRANSFERENCIA' ? true : f.text('paid') === 'on',
      },
      {
        onSuccess: () => {
          form.reset()
          setSource('')
        },
      },
    )
  }

  if (accounts.length === 0) {
    return <p className="text-sm text-muted">Cadastre uma conta em “Contas e cartões” para lançar.</p>
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <div role="radiogroup" aria-label="Tipo de lançamento" className="grid grid-cols-3 gap-1 rounded-lg bg-canvas p-1">
        {KINDS.map((k) => (
          <button
            key={k.value}
            type="button"
            role="radio"
            aria-checked={kind === k.value}
            onClick={() => {
              setKind(k.value)
              setSource('')
            }}
            className={`rounded-md px-2 py-1.5 text-sm font-medium ${kind === k.value ? 'bg-surface shadow-sm' : 'text-muted'}`}
          >
            {k.label}
          </button>
        ))}
      </div>
      <Field label="Descrição" error={errors.description}>
        <Input name="description" required maxLength={120} />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Valor (R$)" error={errors.amount}>
          <Input name="amount" type="number" step="0.01" min="0.01" required inputMode="decimal" />
        </Field>
        <Field label="Data" error={errors.date}>
          <Input name="date" type="date" defaultValue={todayIso()} required />
        </Field>
      </div>
      <Field label={kind === 'TRANSFERENCIA' ? 'De' : kind === 'RECEITA' ? 'Entra em' : 'Pago com'}>
        <Select name="source" value={source} onChange={(e) => setSource(e.target.value)} required>
          <option value="" disabled>
            Escolha…
          </option>
          <optgroup label="Contas">
            {accounts.map((a) => (
              <option key={a.id} value={`a:${a.id}`}>
                {a.name}
              </option>
            ))}
          </optgroup>
          {kind === 'DESPESA' && cards.length > 0 && (
            <optgroup label="Cartões">
              {cards.map((c) => (
                <option key={c.id} value={`c:${c.id}`}>
                  {c.name}
                </option>
              ))}
            </optgroup>
          )}
        </Select>
      </Field>
      {kind === 'TRANSFERENCIA' ? (
        <Field label="Para">
          <Select name="toAccountId" required defaultValue="">
            <option value="" disabled>
              Escolha…
            </option>
            {accounts
              .filter((a) => `a:${a.id}` !== source)
              .map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name}
                </option>
              ))}
          </Select>
        </Field>
      ) : (
        <Field label="Categoria">
          <Select name="categoryId" defaultValue="">
            <option value="">Sem categoria</option>
            {categoryOptions(allCategories, kind).map((c) => (
              <option key={c.id} value={c.id}>
                {categoryLabel(c, allCategories)}
              </option>
            ))}
          </Select>
        </Field>
      )}
      {onCard && (
        <Field label="Parcelas" error={errors.installments}>
          <Input name="installments" type="number" min="1" max="48" defaultValue="1" />
        </Field>
      )}
      {!onCard && kind !== 'TRANSFERENCIA' && (
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" name="paid" defaultChecked className="size-4 accent-[var(--color-accent)]" />
          {kind === 'RECEITA' ? 'Já recebido' : 'Já pago'}
        </label>
      )}
      <ErrorText error={Object.keys(errors).length ? null : create.error} />
      <Button type="submit" disabled={create.isPending}>
        Lançar
      </Button>
    </form>
  )
}
