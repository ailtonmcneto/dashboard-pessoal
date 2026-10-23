import { useState } from 'react'
import { Link } from 'react-router'
import { EventList } from '../agenda/EventList'
import { useUpcomingEvents } from '../agenda/events'
import { useMe } from '../auth/auth'
import { Card, Empty, ErrorText, Loaded, PageHeader, SectionTitle } from '../components/ui'
import { useSchedule, useTopics, useUpcomingAssessments } from '../estudos/api'
import { ASSESSMENT_KIND } from '../estudos/labels'
import { ColorDot } from '../estudos/parts'
import { formatClock, formatDate, todayIso } from '../lib/format'

const todayFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })

function greeting(hour: number) {
  if (hour < 12) return 'Bom dia'
  if (hour < 18) return 'Boa tarde'
  return 'Boa noite'
}

function MoreLink({ to, children }: { to: string; children: string }) {
  return (
    <Link to={to} className="text-sm text-accent hover:underline">
      {children}
    </Link>
  )
}

function ClassesToday({ weekday }: { weekday: number }) {
  const schedule = useSchedule()
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/estudos/horario">Horário</MoreLink>}>Aulas de hoje</SectionTitle>
      <Loaded query={schedule}>
        {(slots) => {
          const today = slots.filter((s) => s.weekday === weekday)
          return today.length === 0 ? (
            <Empty>Sem aulas hoje.</Empty>
          ) : (
            <ul className="space-y-2">
              {today.map((s) => (
                <li key={s.id} className="flex items-center gap-2 text-sm">
                  <span className="w-24 tabular-nums text-muted">
                    {formatClock(s.startTime)}–{formatClock(s.endTime)}
                  </span>
                  <ColorDot color={s.color} />
                  <span className="font-medium">{s.courseName}</span>
                  {s.room && <span className="text-muted">· {s.room}</span>}
                </li>
              ))}
            </ul>
          )
        }}
      </Loaded>
    </Card>
  )
}

function UpcomingAssessments() {
  const list = useUpcomingAssessments(14)
  const today = todayIso()
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/estudos">Semestre</MoreLink>}>Provas e entregas (14 dias)</SectionTitle>
      <Loaded query={list}>
        {(data) =>
          data.length === 0 ? (
            <Empty>Nada nos próximos 14 dias.</Empty>
          ) : (
            <ul className="space-y-2">
              {data.map((a) => (
                <li key={a.id} className="flex items-center gap-2 text-sm">
                  <span className={`w-20 shrink-0 whitespace-nowrap tabular-nums ${a.dueOn === today ? 'font-semibold' : 'text-muted'}`}>
                    {a.dueOn === today ? 'Hoje' : formatDate(a.dueOn!)}
                  </span>
                  <ColorDot color={a.color} />
                  <Link to={`/estudos/disciplinas/${a.courseId}`} className="min-w-0 flex-1 truncate hover:underline">
                    {a.title} · {a.courseName}
                  </Link>
                  <span className="text-xs text-muted">{ASSESSMENT_KIND[a.kind]}</span>
                </li>
              ))}
            </ul>
          )
        }
      </Loaded>
    </Card>
  )
}

function ReviewsToday() {
  const due = useTopics({ due: true })
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/estudos/revisoes">Revisar</MoreLink>}>Revisões</SectionTitle>
      <Loaded query={due}>
        {(data) =>
          data.length === 0 ? (
            <Empty>Nada para revisar hoje.</Empty>
          ) : (
            <p className="text-sm">
              <span className="text-2xl font-bold tabular-nums">{data.length}</span>{' '}
              {data.length === 1 ? 'assunto para revisar' : 'assuntos para revisar'}
            </p>
          )
        }
      </Loaded>
    </Card>
  )
}

export function HojePage() {
  const me = useMe()
  const events = useUpcomingEvents(7)
  const [now] = useState(() => new Date())
  const firstName = me.data?.name.split(' ')[0]
  const weekday = ((now.getDay() + 6) % 7) + 1

  return (
    <>
      <PageHeader title={`${greeting(now.getHours())}, ${firstName}`} subtitle={todayFmt.format(now)} />
      <div className="grid gap-6 lg:grid-cols-2">
        <ClassesToday weekday={weekday} />
        <UpcomingAssessments />
        <Card>
          <SectionTitle action={<MoreLink to="/agenda">Abrir agenda</MoreLink>}>Próximos 7 dias</SectionTitle>
          {events.isPending ? (
            <Empty>Carregando…</Empty>
          ) : events.isError ? (
            <ErrorText error={events.error} />
          ) : (
            <EventList events={events.data} />
          )}
        </Card>
        <ReviewsToday />
      </div>
    </>
  )
}
