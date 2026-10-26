import { AlertTriangle, CheckCircle2, ChevronRight, Flame, XCircle } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { EventList } from '../agenda/EventList'
import { useGet } from '../api/hooks'
import type { Hoje, HojeAlert } from '../api/types'
import { useMe } from '../auth/auth'
import { StatusBadge } from '../components/StatusBadge'
import { Card, Empty, Loaded, PageHeader, SectionTitle } from '../components/ui'
import { ASSESSMENT_KIND } from '../estudos/labels'
import { ColorDot } from '../estudos/parts'
import { formatClock, formatDate, formatMinutes, formatMoney, todayIso } from '../lib/format'
import { TaskList } from '../metas/TaskList'
import { DayToggle } from '../saude/HabitToggle'
import { formatValue, statusFor } from '../saude/labels'
import { QuickAdd } from '../saude/MetricasPage'

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

function Alerts({ alerts }: { alerts: HojeAlert[] }) {
  if (alerts.length === 0) {
    return (
      <p className="flex items-center gap-2 rounded-xl border border-line bg-surface px-4 py-3 text-sm">
        <CheckCircle2 size={16} className="text-good" aria-hidden="true" />
        Tudo em dia: nenhum alerta para hoje.
      </p>
    )
  }
  return (
    <Card>
      <SectionTitle>Atenção</SectionTitle>
      <ul className="divide-y divide-line">
        {alerts.map((a, i) => {
          const Icon = a.severity === 'CRITICA' ? XCircle : AlertTriangle
          return (
            <li key={`${a.kind}-${i}`}>
              <Link to={a.link} className="group flex items-center gap-3 py-2.5">
                <Icon
                  size={18}
                  className={a.severity === 'CRITICA' ? 'text-critical' : 'text-warning'}
                  aria-label={a.severity === 'CRITICA' ? 'Crítico' : 'Atenção'}
                />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium">{a.title}</p>
                  <p className="text-xs text-muted">{a.detail}</p>
                </div>
                <ChevronRight size={16} className="text-muted group-hover:text-ink" aria-hidden="true" />
              </Link>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}

function Stat({ label, value, to }: { label: string; value: string; to: string }) {
  return (
    <Link to={to} className="rounded-xl border border-line bg-surface p-3 transition hover:border-accent/50">
      <p className="text-xs text-muted">{label}</p>
      <p className="text-lg font-bold tabular-nums">{value}</p>
    </Link>
  )
}

function Summary({ h }: { h: Hoje }) {
  const habitsDone = h.habits.filter((x) => x.doneToday).length
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
      <Stat label="Tarefas de hoje" value={String(h.tasks.length)} to="/metas" />
      <Stat label="Hábitos" value={`${habitsDone}/${h.habits.length}`} to="/saude" />
      <Stat label="Aulas hoje" value={String(h.classes.length)} to="/estudos/horario" />
      <Stat label="Estudo na semana" value={formatMinutes(h.studyMinutesWeek)} to="/estudos/sessoes" />
      <Stat label="Saldo em contas" value={formatMoney(h.money.totalBalance)} to="/financas" />
    </div>
  )
}

function Habits({ h }: { h: Hoje }) {
  const today = todayIso()
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/saude">Hábitos</MoreLink>}>Hábitos de hoje</SectionTitle>
      {h.habits.length === 0 ? (
        <Empty>Nenhum hábito para hoje.</Empty>
      ) : (
        <>
        <p className="mb-3 text-xs text-muted">
          {h.habits.filter((x) => x.doneToday).length} de {h.habits.length} feitos
        </p>
        <ul className="space-y-2">
          {h.habits.map((x) => (
            <li key={x.id} className="flex items-center gap-3">
              <DayToggle habit={x} date={today} due done={x.doneToday} size="lg" />
              <span className={`flex-1 text-sm ${x.doneToday ? 'text-muted line-through' : 'font-medium'}`}>{x.name}</span>
              {x.currentStreak > 0 && (
                <span className="inline-flex items-center gap-1 text-xs tabular-nums text-muted">
                  <Flame size={13} className="text-warning" aria-hidden="true" />
                  {x.currentStreak}
                </span>
              )}
            </li>
          ))}
        </ul>
        </>
      )}
    </Card>
  )
}

function Classes({ h }: { h: Hoje }) {
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/estudos/horario">Horário</MoreLink>}>Aulas de hoje</SectionTitle>
      {h.classes.length === 0 ? (
        <Empty>Sem aulas hoje.</Empty>
      ) : (
        <ul className="space-y-2">
          {h.classes.map((s) => (
            <li key={s.id} className="flex items-center gap-2 text-sm">
              <span className="w-24 tabular-nums text-muted">
                {formatClock(s.startTime)}–{formatClock(s.endTime)}
              </span>
              <ColorDot color={s.color} />
              <Link to={`/estudos/disciplinas/${s.courseId}`} className="font-medium hover:underline">
                {s.courseName}
              </Link>
              {s.room && <span className="text-muted">· {s.room}</span>}
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

function Assessments({ h }: { h: Hoje }) {
  const today = todayIso()
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/estudos">Semestre</MoreLink>}>Provas e entregas (14 dias)</SectionTitle>
      {h.assessments.length === 0 ? (
        <Empty>Nada nos próximos 14 dias.</Empty>
      ) : (
        <ul className="space-y-2">
          {h.assessments.map((a) => (
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
      )}
    </Card>
  )
}

function Health({ h }: { h: Hoje }) {
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/saude/metricas">Métricas</MoreLink>}>Saúde hoje</SectionTitle>
      {h.metrics.length === 0 ? (
        <Empty>Nenhuma métrica com meta.</Empty>
      ) : (
        <ul className="space-y-3">
          {h.metrics.map((m) => {
            const status = statusFor(m)
            return (
              <li key={m.id} className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2 text-sm">
                  <span className="font-medium">{m.name}</span>
                  <StatusBadge tone={status.tone} label={status.label} />
                  <span className="ml-auto tabular-nums text-muted">
                    {formatValue(m.today ?? 0, m.unit)} / {formatValue(m.target!, m.unit)}
                  </span>
                </div>
                <QuickAdd metric={m} />
              </li>
            )
          })}
        </ul>
      )}
    </Card>
  )
}

function Reviews({ h }: { h: Hoje }) {
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/estudos/revisoes">Revisar</MoreLink>}>Revisões</SectionTitle>
      {h.reviewsDue === 0 ? (
        <Empty>Nada para revisar hoje.</Empty>
      ) : (
        <>
          <p className="text-sm">
            <span className="text-2xl font-bold tabular-nums">{h.reviewsDue}</span>{' '}
            {h.reviewsDue === 1 ? 'assunto para revisar' : 'assuntos para revisar'}
          </p>
          <p className="mt-1 truncate text-xs text-muted">
            {h.reviews.map((t) => t.title).join(' · ')}
            {h.reviewsDue > h.reviews.length && ' …'}
          </p>
        </>
      )}
    </Card>
  )
}

function Money({ h }: { h: Hoje }) {
  const m = h.money
  return (
    <Card>
      <SectionTitle action={<MoreLink to="/financas">Finanças</MoreLink>}>Dinheiro do mês</SectionTitle>
      <dl className="grid grid-cols-2 gap-3 text-sm">
        <div>
          <dt className="text-muted">Entrou</dt>
          <dd className="text-xl font-bold tabular-nums">{formatMoney(m.income)}</dd>
        </div>
        <div>
          <dt className="text-muted">Saiu</dt>
          <dd className="text-xl font-bold tabular-nums">{formatMoney(m.expense)}</dd>
        </div>
        {m.pendingCount > 0 && (
          <div className="col-span-2 flex items-center gap-2">
            <AlertTriangle size={14} className="text-warning" aria-hidden="true" />
            <Link to="/financas/lancamentos" className="hover:underline">
              {m.pendingCount} {m.pendingCount === 1 ? 'conta a pagar' : 'contas a pagar'} até o fim do mês (
              {formatMoney(m.pendingAmount)})
            </Link>
          </div>
        )}
      </dl>
    </Card>
  )
}

export function HojePage() {
  const me = useMe()
  const hoje = useGet<Hoje>('/hoje')
  const [now] = useState(() => new Date())
  const firstName = me.data?.name.split(' ')[0]

  return (
    <>
      <PageHeader title={`${greeting(now.getHours())}, ${firstName}`} subtitle={todayFmt.format(now)} />
      <Loaded query={hoje}>
        {(h) => (
          <div className="space-y-6">
            <Alerts alerts={h.alerts} />
            <Summary h={h} />
            <div className="grid gap-6 lg:grid-cols-2">
              <Card>
                <SectionTitle action={<MoreLink to="/metas">Tarefas</MoreLink>}>Tarefas de hoje</SectionTitle>
                <TaskList view="HOJE" items={h.tasks} empty="Nada vencendo hoje." />
              </Card>
              <Habits h={h} />
              <Classes h={h} />
              <Assessments h={h} />
              <Health h={h} />
              <Reviews h={h} />
              <Card>
                <SectionTitle action={<MoreLink to="/agenda">Abrir agenda</MoreLink>}>Próximos 7 dias</SectionTitle>
                <EventList events={h.events} />
              </Card>
              <Money h={h} />
            </div>
          </div>
        )}
      </Loaded>
    </>
  )
}
