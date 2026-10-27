import { Pause, Play, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import type { Frequency, Recurrence } from '../api/types'
import { EditButton } from '../components/Modal'
import {
  Button,
  Card,
  Empty,
  ErrorText,
  Field,
  IconButton,
  Input,
  Loaded,
  SectionTitle,
  Select,
} from '../components/ui'
import { formData } from '../lib/forms'
import { formatDate, formatMoney, todayIso } from '../lib/format'
import {
  useAccounts,
  useCards,
  useCategories,
  useCreateRecurrence,
  useDeleteRecurrence,
  useRecurrences,
  useToggleRecurrence,
  useUpdateRecurrence,
} from './api'
import { categoryLabel, categoryOptions, FREQUENCY } from './labels'
import { makeLookup } from './lookup'

/** Cria uma recorrência ou, com `initial`, edita esse molde (o tipo não muda; vale para os próximos lançamentos). */
function RecurrenceForm({ initial, onDone }: { initial?: Recurrence; onDone?: () => void }) {
  const create = useCreateRecurrence()
  const update = useUpdateRecurrence()
  const save = initial ? update : create
  const accounts = (useAccounts().data ?? []).filter((a) => !a.archived)
  const cards = (useCards().data ?? []).filter((c) => !c.archived)
  const categories = useCategories().data ?? []
  const [kind, setKind] = useState<'DESPESA' | 'RECEITA'>(initial?.kind === 'RECEITA' ? 'RECEITA' : 'DESPESA')
  const errors = save.error instanceof ApiError ? save.error.errors : {}

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const [type, id] = (f.text('source') ?? ':').split(':')
    const req = {
      description: f.required('description'),
      amount: f.number('amount') ?? 0,
      kind,
      categoryId: f.text('categoryId'),
      accountId: type === 'a' ? id : null,
      cardId: type === 'c' ? id : null,
      frequency: f.text('frequency') as Frequency,
      startsOn: f.required('startsOn'),
      endsOn: f.text('endsOn'),
    }
    if (initial) update.mutate({ id: initial.id, ...req }, { onSuccess: onDone })
    else create.mutate(req, { onSuccess: () => form.reset() })
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      {!initial && (
        <div role="radiogroup" aria-label="Tipo" className="grid grid-cols-2 gap-1 rounded-lg bg-canvas p-1">
          {(['DESPESA', 'RECEITA'] as const).map((k) => (
            <button
              key={k}
              type="button"
              role="radio"
              aria-checked={kind === k}
              onClick={() => setKind(k)}
              className={`rounded-md px-2 py-1.5 text-sm font-medium ${kind === k ? 'bg-surface shadow-sm' : 'text-muted'}`}
            >
              {k === 'DESPESA' ? 'Despesa' : 'Receita'}
            </button>
          ))}
        </div>
      )}
      <Field label="Descrição" error={errors.description}>
        <Input
          name="description"
          required
          maxLength={120}
          placeholder={kind === 'DESPESA' ? 'Aluguel' : 'Salário'}
          defaultValue={initial?.description}
        />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Valor (R$)" error={errors.amount}>
          <Input name="amount" type="number" step="0.01" min="0.01" required defaultValue={initial?.amount} />
        </Field>
        <Field label="Frequência">
          <Select name="frequency" defaultValue={initial?.frequency ?? 'MENSAL'}>
            {Object.entries(FREQUENCY).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Primeira vez" error={errors.startsOn}>
          <Input name="startsOn" type="date" defaultValue={initial?.startsOn ?? todayIso()} required />
        </Field>
        <Field label="Até (opcional)" error={errors.endsOn}>
          <Input name="endsOn" type="date" defaultValue={initial?.endsOn ?? ''} />
        </Field>
      </div>
      <Field label={kind === 'DESPESA' ? 'Pago com' : 'Entra em'}>
        <Select
          name="source"
          required
          defaultValue={initial?.cardId ? `c:${initial.cardId}` : initial?.accountId ? `a:${initial.accountId}` : ''}
        >
          <option value="" disabled>
            Escolha…
          </option>
          {accounts.map((a) => (
            <option key={a.id} value={`a:${a.id}`}>
              {a.name}
            </option>
          ))}
          {kind === 'DESPESA' &&
            cards.map((c) => (
              <option key={c.id} value={`c:${c.id}`}>
                {c.name} (cartão)
              </option>
            ))}
        </Select>
      </Field>
      <Field label="Categoria">
        <Select name="categoryId" defaultValue={initial?.categoryId ?? ''}>
          <option value="">Sem categoria</option>
          {categoryOptions(categories, kind).map((c) => (
            <option key={c.id} value={c.id}>
              {categoryLabel(c, categories)}
            </option>
          ))}
        </Select>
      </Field>
      <p className="text-xs text-muted">
        {initial
          ? 'As mudanças valem para os próximos lançamentos; os já criados continuam como estão.'
          : 'Se a primeira vez for no passado, os lançamentos até hoje são criados na hora. ' +
            'Em conta, eles nascem como pendentes para você confirmar.'}
      </p>
      <ErrorText error={Object.keys(errors).length ? null : save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? 'Salvar' : 'Criar recorrência'}
      </Button>
    </form>
  )
}

export function RecorrenciasPage() {
  const list = useRecurrences()
  const lookup = makeLookup(useAccounts().data, useCards().data, useCategories().data)
  const toggle = useToggleRecurrence()
  const remove = useDeleteRecurrence()

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
      <Card>
        <SectionTitle>Recorrências</SectionTitle>
        <Loaded query={list}>
          {(data) =>
            data.length === 0 ? (
              <Empty>Nada recorrente ainda: aluguel, salário, assinaturas…</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {data.map((r) => (
                  <li key={r.id} className={`flex items-center gap-3 py-3 ${r.active ? '' : 'opacity-60'}`}>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-medium">{r.description}</p>
                      <p className="truncate text-xs text-muted">
                        {[
                          FREQUENCY[r.frequency],
                          r.cardId ? lookup.card(r.cardId) : lookup.account(r.accountId),
                          lookup.category(r.categoryId)?.label,
                          r.active ? `próxima ${formatDate(r.nextOn)}` : 'pausada',
                        ]
                          .filter(Boolean)
                          .join(' · ')}
                      </p>
                    </div>
                    <span className="tabular-nums">
                      {r.kind === 'RECEITA' ? '+ ' : '− '}
                      {formatMoney(r.amount)}
                    </span>
                    <EditButton label={`Editar ${r.description}`} title="Editar recorrência">
                      {(close) => <RecurrenceForm initial={r} onDone={close} />}
                    </EditButton>
                    <IconButton
                      label={r.active ? `Pausar ${r.description}` : `Retomar ${r.description}`}
                      onClick={() => toggle.mutate({ id: r.id, active: !r.active })}
                    >
                      {r.active ? <Pause size={15} /> : <Play size={15} />}
                    </IconButton>
                    <IconButton
                      label={`Apagar ${r.description}`}
                      onClick={() =>
                        confirm(`Apagar a recorrência “${r.description}”? Os lançamentos já criados continuam.`) &&
                        remove.mutate(r.id)
                      }
                    >
                      <Trash2 size={15} />
                    </IconButton>
                  </li>
                ))}
              </ul>
            )
          }
        </Loaded>
        <ErrorText error={toggle.error} />
      </Card>
      <Card className="self-start">
        <SectionTitle>Nova recorrência</SectionTitle>
        <RecurrenceForm />
      </Card>
    </div>
  )
}
