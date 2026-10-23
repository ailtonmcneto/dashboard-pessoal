import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import type { EventKind } from '../api/types'
import { EventList } from '../agenda/EventList'
import { KIND_LABEL, useCreateEvent, useDeleteEvent, useUpcomingEvents } from '../agenda/events'
import { Button, Card, ErrorText, Field, Input, PageHeader, Select } from '../components/ui'
import { localInputToIso } from '../lib/format'

function NewEventForm() {
  const create = useCreateEvent()
  const [title, setTitle] = useState('')
  const [kind, setKind] = useState<EventKind>('COMPROMISSO')
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [location, setLocation] = useState('')

  const fieldErrors = create.error instanceof ApiError ? create.error.errors : {}

  function submit(e: FormEvent) {
    e.preventDefault()
    create.mutate(
      {
        title,
        kind,
        startsAt: localInputToIso(startsAt),
        endsAt: endsAt ? localInputToIso(endsAt) : null,
        location: location || null,
      },
      {
        onSuccess: () => {
          setTitle('')
          setStartsAt('')
          setEndsAt('')
          setLocation('')
        },
      },
    )
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <h2 className="font-semibold">Novo evento</h2>
      <Field label="Título" error={fieldErrors.title}>
        <Input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
      </Field>
      <Field label="Tipo">
        <Select value={kind} onChange={(e) => setKind(e.target.value as EventKind)}>
          {Object.entries(KIND_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </Field>
      <div className="grid gap-3">
        <Field label="Início" error={fieldErrors.startsAt}>
          <Input type="datetime-local" value={startsAt} onChange={(e) => setStartsAt(e.target.value)} required />
        </Field>
        <Field label="Fim" error={fieldErrors.endsAt}>
          <Input type="datetime-local" value={endsAt} onChange={(e) => setEndsAt(e.target.value)} min={startsAt} />
        </Field>
      </div>
      <Field label="Local">
        <Input value={location} onChange={(e) => setLocation(e.target.value)} maxLength={200} />
      </Field>
      <ErrorText error={create.error} />
      <Button type="submit" disabled={create.isPending}>
        Adicionar
      </Button>
    </form>
  )
}

export function AgendaPage() {
  const events = useUpcomingEvents(30)
  const remove = useDeleteEvent()

  return (
    <>
      <PageHeader title="Agenda" subtitle="Compromissos e consultas dos próximos 30 dias" />
      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <Card>
          {events.isPending ? (
            <p className="text-sm text-muted">Carregando…</p>
          ) : events.isError ? (
            <ErrorText error={events.error} />
          ) : (
            <EventList events={events.data} onDelete={(id) => remove.mutate(id)} />
          )}
        </Card>
        <Card className="self-start">
          <NewEventForm />
        </Card>
      </div>
    </>
  )
}
