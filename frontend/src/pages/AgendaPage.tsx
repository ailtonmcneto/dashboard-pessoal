import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import type { EventItem, EventKind } from '../api/types'
import { EventList } from '../agenda/EventList'
import { KIND_LABEL, useCreateEvent, useDeleteEvent, useUpcomingEvents, useUpdateEvent } from '../agenda/events'
import { EditButton } from '../components/Modal'
import { Button, Card, ErrorText, Field, Input, PageHeader, Select, Textarea } from '../components/ui'
import { isoToLocalInput, localInputToIso } from '../lib/format'

/** Cria um evento ou, com `initial`, edita esse evento (o mesmo formulário, já preenchido). */
function EventForm({ initial, onDone }: { initial?: EventItem; onDone?: () => void }) {
  const create = useCreateEvent()
  const update = useUpdateEvent()
  const save = initial ? update : create
  const [title, setTitle] = useState(initial?.title ?? '')
  const [kind, setKind] = useState<EventKind>(initial?.kind ?? 'COMPROMISSO')
  const [startsAt, setStartsAt] = useState(initial ? isoToLocalInput(initial.startsAt) : '')
  const [endsAt, setEndsAt] = useState(initial?.endsAt ? isoToLocalInput(initial.endsAt) : '')
  const [location, setLocation] = useState(initial?.location ?? '')
  const [notes, setNotes] = useState(initial?.notes ?? '')

  const fieldErrors = save.error instanceof ApiError ? save.error.errors : {}

  function submit(e: FormEvent) {
    e.preventDefault()
    const req = {
      title,
      kind,
      startsAt: localInputToIso(startsAt),
      endsAt: endsAt ? localInputToIso(endsAt) : null,
      location: location || null,
      notes: notes || null,
    }
    if (initial) {
      update.mutate({ id: initial.id, ...req }, { onSuccess: onDone })
      return
    }
    create.mutate(req, {
      onSuccess: () => {
        setTitle('')
        setStartsAt('')
        setEndsAt('')
        setLocation('')
        setNotes('')
      },
    })
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      {!initial && <h2 className="font-semibold">Novo evento</h2>}
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
      {initial && (
        <Field label="Observações">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      )}
      <ErrorText error={save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? 'Salvar' : 'Adicionar'}
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
            <EventList
              events={events.data}
              onDelete={(id) => remove.mutate(id)}
              edit={(ev) => (
                <EditButton label={`Editar ${ev.title}`} title="Editar evento">
                  {(close) => <EventForm initial={ev} onDone={close} />}
                </EditButton>
              )}
            />
          )}
        </Card>
        <Card className="self-start">
          <EventForm />
        </Card>
      </div>
    </>
  )
}
