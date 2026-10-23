import { MapPin, Plus } from "lucide-react";
import { requireUser } from "@/lib/supabase/server";
import { addDays, fmtDate, today } from "@/lib/format";
import { localDay, timeRange, type EventRow } from "@/lib/events";
import { Empty, PageHeader, Panel, Progress } from "@/components/page-header";
import { DeleteButton, SubmitButton } from "@/components/ui";
import { addEvent, addStudyItem, deleteEvent, deleteStudyItem, logStudy, updateStudyItem } from "./actions";

type Study = { id: string; title: string; kind: string; progress: number; status: string };

const KIND: Record<string, string> = { curso: "Curso", livro: "Livro", disciplina: "Disciplina", outro: "Outro" };
const STATUS: Record<string, string> = { ativo: "Em andamento", pausado: "Pausado", concluido: "Concluído" };

export default async function AgendaPage() {
  const { supabase } = await requireUser();
  const now = today();
  const weekAgo = addDays(now, -6);

  const [{ data: ev }, { data: items }, { data: sessions }] = await Promise.all([
    supabase.from("events").select("id,title,starts_at,ends_at,location")
      .gte("starts_at", new Date(`${now}T00:00:00-03:00`).toISOString())
      .lt("starts_at", new Date(`${addDays(now, 30)}T00:00:00-03:00`).toISOString())
      .order("starts_at"),
    supabase.from("study_items").select("id,title,kind,progress,status").order("status").order("created_at"),
    supabase.from("study_sessions").select("item_id,minutes,date").gte("date", weekAgo),
  ]);

  const events = (ev ?? []) as EventRow[];
  const byDay = new Map<string, EventRow[]>();
  for (const e of events) {
    const d = localDay(e.starts_at);
    byDay.set(d, [...(byDay.get(d) ?? []), e]);
  }

  const study = (items ?? []) as Study[];
  const minutesBy = new Map<string, number>();
  for (const s of sessions ?? []) minutesBy.set(s.item_id, (minutesBy.get(s.item_id) ?? 0) + s.minutes);
  const weekMinutes = [...minutesBy.values()].reduce((a, b) => a + b, 0);
  const active = study.filter((s) => s.status !== "concluido");

  const dayLabel = (d: string) =>
    d === now ? "Hoje" : d === addDays(now, 1) ? "Amanhã" : fmtDate(d, { weekday: "long", day: "2-digit", month: "long" });

  return (
    <>
      <PageHeader title="Agenda e estudos">Seus próximos 30 dias de compromissos e o que você está estudando, com o tempo dedicado na semana.</PageHeader>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="space-y-4">
          <Panel title="Próximos compromissos">
            {events.length === 0 ? (
              <Empty>Agenda livre nos próximos 30 dias.</Empty>
            ) : (
              <div className="space-y-5">
                {[...byDay.entries()].map(([day, list]) => (
                  <div key={day}>
                    <h3 className="mb-1.5 text-sm font-semibold capitalize text-muted">{dayLabel(day)}</h3>
                    <ul className="space-y-1">
                      {list.map((e) => (
                        <li key={e.id} className="flex items-center gap-3 rounded-lg bg-bg px-3 py-2">
                          <span className="w-24 shrink-0 text-sm font-semibold">{timeRange(e)}</span>
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-medium">{e.title}</p>
                            {e.location && <p className="flex items-center gap-1 truncate text-xs text-muted"><MapPin size={11} />{e.location}</p>}
                          </div>
                          <DeleteButton id={e.id} action={deleteEvent} label={`Excluir ${e.title}`} />
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="Novo compromisso">
            <form action={addEvent} className="space-y-2">
              <input name="title" required placeholder="Ex.: Prova de Cálculo II" className="field" />
              <div className="grid grid-cols-[1fr_auto] gap-2">
                <label className="space-y-1">
                  <span className="text-xs text-muted">Início</span>
                  <input name="starts_at" type="datetime-local" required className="field" />
                </label>
                <label className="space-y-1">
                  <span className="text-xs text-muted">Fim (opcional)</span>
                  <input name="end_time" type="time" className="field w-32" />
                </label>
              </div>
              <input name="location" placeholder="Local (opcional)" className="field" />
              <SubmitButton><Plus size={16} /> Adicionar à agenda</SubmitButton>
            </form>
          </Panel>
        </div>

        <div className="space-y-4">
          <Panel title="Estudos" action={<span className="text-sm text-muted">{formatMinutes(weekMinutes)} nos últimos 7 dias</span>}>
            {study.length === 0 ? (
              <Empty>Adicione um curso, livro ou disciplina para acompanhar o progresso.</Empty>
            ) : (
              <ul className="space-y-4">
                {study.map((s) => (
                  <li key={s.id} className={s.status === "concluido" ? "opacity-60" : ""}>
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="truncate font-medium">{s.title}</p>
                        <p className="text-xs text-muted">
                          {KIND[s.kind]} · {STATUS[s.status]}
                          {minutesBy.get(s.id) ? ` · ${formatMinutes(minutesBy.get(s.id)!)} esta semana` : ""}
                        </p>
                      </div>
                      <DeleteButton id={s.id} action={deleteStudyItem} label={`Excluir ${s.title}`} />
                    </div>
                    <div className="mt-2 flex items-center gap-3">
                      <div className="flex-1"><Progress value={s.progress} label={`Progresso de ${s.title}`} /></div>
                      <span className="w-10 text-right text-sm font-semibold">{s.progress}%</span>
                    </div>
                    <form action={updateStudyItem} className="mt-2 flex flex-wrap items-center gap-2">
                      <input type="hidden" name="id" value={s.id} />
                      <input name="progress" type="number" min="0" max="100" defaultValue={s.progress} aria-label={`Progresso de ${s.title} em %`} className="field !w-20 !py-1" />
                      <select name="status" defaultValue={s.status} aria-label={`Situação de ${s.title}`} className="field !w-auto !py-1">
                        {Object.entries(STATUS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                      </select>
                      <SubmitButton className="ml-auto text-sm font-semibold underline-offset-4 hover:underline">Atualizar</SubmitButton>
                    </form>
                  </li>
                ))}
              </ul>
            )}
          </Panel>

          {active.length > 0 && (
            <Panel title="Registrar sessão de estudo">
              <form action={logStudy} className="grid grid-cols-[1fr_auto] gap-2">
                <select name="item_id" required aria-label="O que você estudou" className="field col-span-2">
                  {active.map((s) => <option key={s.id} value={s.id}>{s.title}</option>)}
                </select>
                <input name="minutes" type="number" min="1" step="5" required placeholder="Minutos" className="field" />
                <input name="date" type="date" defaultValue={now} aria-label="Data" className="field w-40" />
                <SubmitButton className="btn col-span-2"><Plus size={16} /> Registrar sessão</SubmitButton>
              </form>
            </Panel>
          )}

          <Panel title="Novo item de estudo">
            <form action={addStudyItem} className="grid grid-cols-[1fr_auto] gap-2">
              <input name="title" required placeholder="Ex.: Algoritmos (UFPB)" className="field" />
              <select name="kind" defaultValue="curso" aria-label="Tipo" className="field w-32">
                {Object.entries(KIND).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
              <SubmitButton className="btn col-span-2"><Plus size={16} /> Adicionar</SubmitButton>
            </form>
          </Panel>
        </div>
      </div>
    </>
  );
}

function formatMinutes(m: number) {
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  return m % 60 ? `${h} h ${m % 60} min` : `${h} h`;
}
