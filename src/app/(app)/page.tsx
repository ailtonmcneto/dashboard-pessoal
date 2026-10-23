import Link from "next/link";
import { requireUser } from "@/lib/supabase/server";
import { TZ, addDays, fmtDate, monthStart, money, today } from "@/lib/format";
import { localDay, timeRange, type EventRow } from "@/lib/events";
import { Empty, Panel, Progress } from "@/components/page-header";
import { HabitCell } from "@/components/habit-cell";
import { TaskRow, type Task } from "@/components/task-row";

export default async function HojePage() {
  const { supabase } = await requireUser();
  const now = today();
  const tomorrowEnd = new Date(`${addDays(now, 2)}T00:00:00-03:00`).toISOString();

  const [habits, logs, tasks, events, tx, health, goals] = await Promise.all([
    supabase.from("habits").select("id,name").eq("archived", false).order("created_at"),
    supabase.from("habit_logs").select("habit_id").eq("date", now),
    supabase.from("tasks").select("id,title,done,priority,due_date,goal_id").eq("done", false).lte("due_date", now).order("due_date").order("priority"),
    supabase.from("events").select("id,title,starts_at,ends_at,location").gte("starts_at", new Date().toISOString()).lt("starts_at", tomorrowEnd).order("starts_at"),
    supabase.from("transactions").select("amount,type").gte("date", monthStart(now)).lt("date", monthStart(now, 1)),
    supabase.from("health_logs").select("sleep_hours,water_ml,workout_minutes,mood").eq("date", now).maybeSingle(),
    supabase.from("goals").select("id,title,target,current"),
  ]);

  const doneToday = new Set((logs.data ?? []).map((l) => l.habit_id));
  const habitList = habits.data ?? [];
  const pending = (tasks.data ?? []) as Task[];
  const upcoming = (events.data ?? []) as EventRow[];

  const receitas = (tx.data ?? []).filter((t) => t.type === "receita").reduce((a, t) => a + Number(t.amount), 0);
  const despesas = (tx.data ?? []).filter((t) => t.type === "despesa").reduce((a, t) => a + Number(t.amount), 0);

  const topGoals = (goals.data ?? [])
    .map((g) => ({ ...g, pct: (Number(g.current) / Number(g.target)) * 100 }))
    .filter((g) => g.pct < 100)
    .sort((a, b) => b.pct - a.pct)
    .slice(0, 3);

  const h = health.data;

  return (
    <>
      <DayHero date={now} habitsDone={doneToday.size} habitsTotal={habitList.length} tasksDue={pending.length} />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <Panel title="Hábitos de hoje" action={<Link href="/habitos" className="text-sm text-muted hover:text-ink">Ver semana</Link>}>
          {habitList.length === 0 ? (
            <Empty><Link href="/habitos" className="underline">Crie seu primeiro hábito</Link> para marcar aqui todo dia.</Empty>
          ) : (
            <ul className="space-y-2">
              {habitList.map((hb) => (
                <li key={hb.id} className="flex items-center gap-3">
                  <HabitCell habitId={hb.id} date={now} done={doneToday.has(hb.id)} label={hb.name} size="lg" />
                  <span className={`text-sm ${doneToday.has(hb.id) ? "text-muted line-through" : "font-medium"}`}>{hb.name}</span>
                </li>
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Para hoje" action={<Link href="/tarefas" className="text-sm text-muted hover:text-ink">Todas as tarefas</Link>}>
          {pending.length === 0 ? (
            <Empty>Nada vencendo hoje.</Empty>
          ) : (
            <ul className="divide-y divide-line">
              {pending.slice(0, 7).map((t) => <TaskRow key={t.id} task={t} overdue={!!t.due_date && t.due_date < now} showDelete={false} />)}
            </ul>
          )}
        </Panel>

        <Panel title="Hoje e amanhã" action={<Link href="/agenda" className="text-sm text-muted hover:text-ink">Agenda</Link>}>
          {upcoming.length === 0 ? (
            <Empty>Nenhum compromisso até amanhã.</Empty>
          ) : (
            <ul className="space-y-2">
              {upcoming.map((e) => (
                <li key={e.id} className="flex gap-3 text-sm">
                  <span className="w-28 shrink-0 text-muted">
                    {localDay(e.starts_at) === now ? "" : "Amanhã, "}{timeRange(e)}
                  </span>
                  <span className="font-medium">{e.title}</span>
                </li>
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Dinheiro no mês" action={<Link href="/financas" className="text-sm text-muted hover:text-ink">Finanças</Link>}>
          <p className={`text-3xl font-bold tracking-tight ${receitas - despesas < 0 ? "text-bad" : ""}`}>{money(receitas - despesas)}</p>
          <p className="mt-1 text-sm text-muted">saldo de {fmtDate(now, { month: "long" })}</p>
          <dl className="mt-4 grid grid-cols-2 gap-2 text-sm">
            <div><dt className="flex items-center gap-1.5 text-muted"><span className="h-2 w-2 rounded-full bg-in" />Receitas</dt><dd className="font-semibold">{money(receitas)}</dd></div>
            <div><dt className="flex items-center gap-1.5 text-muted"><span className="h-2 w-2 rounded-full bg-out" />Despesas</dt><dd className="font-semibold">{money(despesas)}</dd></div>
          </dl>
        </Panel>

        <Panel title="Corpo hoje" action={<Link href="/habitos" className="text-sm text-muted hover:text-ink">{h ? "Editar" : "Registrar"}</Link>}>
          <dl className="grid grid-cols-3 gap-2">
            <BodyStat label="Sono" value={h?.sleep_hours != null ? `${Number(h.sleep_hours).toLocaleString("pt-BR")} h` : null} />
            <BodyStat label="Água" value={h?.water_ml != null ? `${(h.water_ml / 1000).toLocaleString("pt-BR")} L` : null} />
            <BodyStat label="Treino" value={h?.workout_minutes != null ? `${h.workout_minutes} min` : null} />
          </dl>
          {!h && <p className="mt-3 text-sm text-muted">Sem registro hoje ainda.</p>}
        </Panel>

        <Panel title="Metas mais perto" action={<Link href="/tarefas" className="text-sm text-muted hover:text-ink">Metas</Link>}>
          {topGoals.length === 0 ? (
            <Empty>Nenhuma meta em andamento.</Empty>
          ) : (
            <ul className="space-y-4">
              {topGoals.map((g) => (
                <li key={g.id}>
                  <div className="mb-1.5 flex justify-between gap-2 text-sm">
                    <span className="truncate font-medium">{g.title}</span>
                    <span className="text-muted">{Math.round(g.pct)}%</span>
                  </div>
                  <Progress value={g.pct} label={`Progresso de ${g.title}`} />
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>
    </>
  );
}

function BodyStat({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="text-xs text-muted">{label}</dt>
      <dd className={`text-xl font-bold tracking-tight ${value ? "" : "text-muted"}`}>{value ?? "—"}</dd>
    </div>
  );
}

/**
 * Cabeçalho do dia: a data grande e o arco do sol de João Pessoa
 * (nascer ~5h, pôr ~17h30), com o sol na posição da hora atual.
 */
function DayHero({ date, habitsDone, habitsTotal, tasksDue }: { date: string; habitsDone: number; habitsTotal: number; tasksDue: number }) {
  const parts = new Intl.DateTimeFormat("en-GB", { timeZone: TZ, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).formatToParts(new Date());
  const hour = Number(parts.find((p) => p.type === "hour")!.value) + Number(parts.find((p) => p.type === "minute")!.value) / 60;
  const RISE = 5, SET = 17.5;
  const t = Math.max(0, Math.min(1, (hour - RISE) / (SET - RISE)));
  const isDay = hour >= RISE && hour <= SET;
  const W = 320, H = 96, R = 140, cx = W / 2, cy = H + 44;
  // o arco cruza o horizonte em a0 e π−a0: o sol nasce e se põe exatamente na linha
  const a0 = Math.asin((cy - H) / R);
  const angle = Math.PI - a0 - t * (Math.PI - 2 * a0);
  const sx = cx + R * Math.cos(angle);
  const sy = cy - R * Math.sin(angle);

  const weekday = fmtDate(date, { weekday: "long" });
  const dayMonth = fmtDate(date, { day: "numeric", month: "long" });

  const summary = [
    habitsTotal ? `${habitsDone} de ${habitsTotal} hábitos feitos` : null,
    tasksDue ? `${tasksDue} ${tasksDue === 1 ? "tarefa vence" : "tarefas vencem"} até hoje` : "nenhuma tarefa vencendo",
  ].filter(Boolean).join(", ");

  return (
    <header className="mb-8 flex flex-col-reverse gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-lg capitalize text-muted">{weekday}</p>
        <h1 className="text-5xl font-extrabold leading-none tracking-tight md:text-7xl">{dayMonth}</h1>
        <p className="mt-3 text-muted first-letter:uppercase">{summary}.</p>
      </div>
      <svg viewBox={`0 0 ${W} ${H + 4}`} className="w-56 shrink-0 sm:w-72" role="img" aria-label={isDay ? "Posição do sol agora" : "O sol já se pôs"}>
        <defs>
          <clipPath id="sky"><rect x="0" y="0" width={W} height={H} /></clipPath>
        </defs>
        <g clipPath="url(#sky)">
          <circle cx={cx} cy={cy} r={R} fill="none" stroke="var(--line)" strokeWidth="2" strokeDasharray="2 6" strokeLinecap="round" />
          {isDay && <circle cx={sx} cy={sy} r="14" fill="var(--sun)" />}
        </g>
        <line x1="0" y1={H + 1} x2={W} y2={H + 1} stroke="var(--ink)" strokeWidth="2" />
        {!isDay && <circle cx={hour < RISE ? cx - R * Math.cos(a0) : cx + R * Math.cos(a0)} cy={H + 1} r="5" fill="var(--muted)" />}
      </svg>
    </header>
  );
}
