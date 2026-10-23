import { useState } from 'react'
import { Link } from 'react-router'
import { EventList } from '../agenda/EventList'
import { useUpcomingEvents } from '../agenda/events'
import { useMe } from '../auth/auth'
import { Card, ErrorText, PageHeader } from '../components/ui'

const todayFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })

function greeting(hour: number) {
  if (hour < 12) return 'Bom dia'
  if (hour < 18) return 'Boa tarde'
  return 'Boa noite'
}

export function HojePage() {
  const me = useMe()
  const events = useUpcomingEvents(7)
  const [now] = useState(() => new Date())
  const firstName = me.data?.name.split(' ')[0]

  return (
    <>
      <PageHeader title={`${greeting(now.getHours())}, ${firstName}`} subtitle={todayFmt.format(now)} />
      <Card>
        <div className="mb-4 flex items-baseline justify-between">
          <h2 className="font-semibold">Próximos 7 dias</h2>
          <Link to="/agenda" className="text-sm text-accent hover:underline">
            Abrir agenda
          </Link>
        </div>
        {events.isPending ? (
          <p className="text-sm text-muted">Carregando…</p>
        ) : events.isError ? (
          <ErrorText error={events.error} />
        ) : (
          <EventList events={events.data} />
        )}
      </Card>
    </>
  )
}
