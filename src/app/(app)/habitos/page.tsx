import { Flame, Plus } from "lucide-react";
import { requireUser } from "@/lib/supabase/server";
import { addDays, fmtDate, today } from "@/lib/format";
import { streak } from "@/lib/habits";
import { Empty, PageHeader, Panel } from "@/components/page-header";
import { DeleteButton, SubmitButton } from "@/components/ui";
import { HabitCell } from "@/components/habit-cell";
import { SleepChart } from "@/components/charts";
import { addHabit, deleteHabit, saveHealth } from "./actions";

type Health = { date: string; sleep_hours: number | null; water_ml: number | null; weight_kg: number | null; workout_minutes: number | null; mood: number | null; notes: string | null };

const dec = (n: number) => n.toLocaleString("pt-BR", { maximumFractionDigits: 1 });

const MOODS = ["Péssimo", "Ruim", "Ok", "Bom", "Ótimo"];

export default async function HabitosPage() {
  const { supabase } = await requireUser();
  const now = today();
  const days = Array.from({ length: 7 }, (_, i) => addDays(now, i - 6));

  const [{ data: habits }, { data: logs }, { data: health }] = await Promise.all([
    supabase.from("habits").select("id,name").eq("archived", false).order("created_at"),
    supabase.from("habit_logs").select("habit_id,date").gte("date", addDays(now, -90)),
    supabase.from("health_logs").select("*").gte("date", addDays(now, -13)).order("date"),
  ]);

  const doneBy = new Map<string, Set<string>>();
  for (const l of logs ?? []) {
    if (!doneBy.has(l.habit_id)) doneBy.set(l.habit_id, new Set());
    doneBy.get(l.habit_id)!.add(l.date);
  }

  const healthRows = (health ?? []) as Health[];
  const todayHealth = healthRows.find((h) => h.date === now);
  const last7 = healthRows.filter((h) => h.date > addDays(now, -7));
  const sleepVals = last7.map((h) => h.sleep_hours).filter((v): v is number => v != null).map(Number);
  const avgSleep = sleepVals.length ? sleepVals.reduce((a, b) => a + b, 0) / sleepVals.length : null;
  const workout7 = last7.reduce((a, h) => a + (h.workout_minutes ?? 0), 0);
  const lastWeight = [...healthRows].reverse().find((h) => h.weight_kg != null)?.weight_kg;

  const sleepSeries = Array.from({ length: 14 }, (_, i) => {
    const d = addDays(now, i - 13);
    const row = healthRows.find((h) => h.date === d);
    return { dia: fmtDate(d, { day: "2-digit" }), sono: row?.sleep_hours != null ? Number(row.sleep_hours) : null };
  });

  return (
    <>
      <PageHeader title="Hábitos e saúde">Marque o que fez em cada dia e registre sono, água, treino e humor. A sequência mostra há quantos dias você não quebra a corrente.</PageHeader>

      <Panel title="Últimos 7 dias">
        {(habits ?? []).length === 0 ? (
          <Empty>Nenhum hábito ainda. Adicione o primeiro logo abaixo, como “Ler 20 min” ou “Beber 2 L de água”.</Empty>
        ) : (
          <div className="-mx-1 overflow-x-auto px-1">
            <table className="w-full min-w-[560px] border-separate border-spacing-y-1.5">
              <thead>
                <tr className="text-xs text-muted">
                  <th className="text-left font-medium">Hábito</th>
                  {days.map((d) => (
                    <th key={d} className={`w-10 font-medium ${d === now ? "text-ink" : ""}`}>
                      <span className="block capitalize">{fmtDate(d, { weekday: "short" }).replace(".", "")}</span>
                      <span className="block">{fmtDate(d, { day: "2-digit" })}</span>
                    </th>
                  ))}
                  <th className="w-16 font-medium">Sequência</th>
                  <th className="w-8"><span className="sr-only">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {habits!.map((h) => {
                  const set = doneBy.get(h.id) ?? new Set<string>();
                  const s = streak(set, now);
                  return (
                    <tr key={h.id}>
                      <td className="pr-3 text-sm font-medium">{h.name}</td>
                      {days.map((d) => (
                        <td key={d} className="text-center">
                          <div className="inline-block">
                            <HabitCell habitId={h.id} date={d} done={set.has(d)} label={`${h.name} em ${fmtDate(d)}`} />
                          </div>
                        </td>
                      ))}
                      <td className="text-center text-sm">
                        <span className={`inline-flex items-center gap-1 ${s > 0 ? "font-semibold" : "text-muted"}`}>
                          <Flame size={14} className={s > 0 ? "text-out" : ""} /> {s}
                        </span>
                      </td>
                      <td><DeleteButton id={h.id} action={deleteHabit} label={`Excluir hábito ${h.name}`} /></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        <form action={addHabit} className="mt-4 flex gap-2">
          <input name="name" required placeholder="Novo hábito" className="field max-w-xs" />
          <SubmitButton><Plus size={16} /> Adicionar</SubmitButton>
        </form>
      </Panel>

      <div className="mt-4 grid gap-4 lg:grid-cols-5">
        <Panel title="Registro de hoje" className="lg:col-span-2">
          <form action={saveHealth} className="space-y-3">
            <input type="hidden" name="date" value={now} />
            <div className="grid grid-cols-2 gap-3">
              <NumField name="sleep_hours" label="Sono (h)" step="0.5" value={todayHealth?.sleep_hours} />
              <NumField name="water_ml" label="Água (ml)" step="250" value={todayHealth?.water_ml} />
              <NumField name="workout_minutes" label="Treino (min)" step="5" value={todayHealth?.workout_minutes} />
              <NumField name="weight_kg" label="Peso (kg)" step="0.1" value={todayHealth?.weight_kg} />
            </div>
            <fieldset>
              <legend className="mb-1.5 text-sm font-medium">Humor</legend>
              <div className="grid grid-cols-5 gap-1.5">
                {MOODS.map((m, i) => (
                  <label key={m} className="cursor-pointer">
                    <input type="radio" name="mood" value={i + 1} defaultChecked={todayHealth?.mood === i + 1} className="peer sr-only" />
                    <span className="block rounded-lg border border-line py-1.5 text-center text-xs text-muted peer-checked:border-transparent peer-checked:bg-sun peer-checked:font-semibold peer-checked:text-sun-ink peer-focus-visible:outline-2 peer-focus-visible:outline-sun">
                      {m}
                    </span>
                  </label>
                ))}
              </div>
            </fieldset>
            <textarea name="notes" rows={2} placeholder="Notas do dia" defaultValue={todayHealth?.notes ?? ""} className="field resize-none" />
            <SubmitButton>{todayHealth ? "Atualizar registro" : "Salvar registro"}</SubmitButton>
          </form>
        </Panel>

        <div className="space-y-4 lg:col-span-3">
          <div className="grid grid-cols-3 gap-4">
            <Mini label="Sono médio (7 dias)" value={avgSleep != null ? `${dec(avgSleep)} h` : "—"} />
            <Mini label="Treino (7 dias)" value={`${workout7} min`} />
            <Mini label="Último peso" value={lastWeight != null ? `${dec(Number(lastWeight))} kg` : "—"} />
          </div>
          <Panel title="Sono nas últimas 2 semanas">
            {sleepVals.length === 0 && healthRows.length === 0 ? (
              <Empty>Registre suas horas de sono para ver a curva aqui.</Empty>
            ) : (
              <SleepChart data={sleepSeries} />
            )}
          </Panel>
        </div>
      </div>
    </>
  );
}

function NumField({ name, label, step, value }: { name: string; label: string; step: string; value?: number | null }) {
  return (
    <label className="block space-y-1">
      <span className="text-sm font-medium">{label}</span>
      <input name={name} type="number" min="0" step={step} defaultValue={value ?? ""} className="field" />
    </label>
  );
}

function Mini({ label, value }: { label: string; value: string }) {
  return (
    <div className="panel !p-4">
      <p className="text-xs text-muted">{label}</p>
      <p className="mt-1 text-xl font-bold tracking-tight">{value}</p>
    </div>
  );
}
