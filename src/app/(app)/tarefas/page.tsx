import { Plus } from "lucide-react";
import { requireUser } from "@/lib/supabase/server";
import { addDays, fmtDate, today } from "@/lib/format";
import { Empty, PageHeader, Panel, Progress } from "@/components/page-header";
import { DeleteButton, SubmitButton } from "@/components/ui";
import { TaskRow, type Task } from "@/components/task-row";
import { addGoal, addTask, deleteGoal, updateGoal } from "./actions";

type Goal = { id: string; title: string; area: string; target: number; current: number; unit: string; deadline: string | null };

export default async function TarefasPage() {
  const { supabase } = await requireUser();
  const now = today();

  const [{ data: open }, { data: done }, { data: goalsData }] = await Promise.all([
    supabase.from("tasks").select("id,title,done,priority,due_date,goal_id").eq("done", false)
      .order("due_date", { ascending: true, nullsFirst: false }).order("priority"),
    supabase.from("tasks").select("id,title,done,priority,due_date,goal_id").eq("done", true)
      .order("done_at", { ascending: false }).limit(15),
    supabase.from("goals").select("*").order("deadline", { ascending: true, nullsFirst: false }),
  ]);

  const tasks = (open ?? []) as Task[];
  const goals = (goalsData ?? []).map((g) => ({ ...g, target: Number(g.target), current: Number(g.current) })) as Goal[];
  const goalName = (id: string | null) => goals.find((g) => g.id === id)?.title;
  const weekEnd = addDays(now, 7);

  const groups: { title: string; items: Task[]; overdue?: boolean }[] = [
    { title: "Atrasadas", items: tasks.filter((t) => t.due_date && t.due_date < now), overdue: true },
    { title: "Hoje", items: tasks.filter((t) => t.due_date === now) },
    { title: "Próximos 7 dias", items: tasks.filter((t) => t.due_date && t.due_date > now && t.due_date <= weekEnd) },
    { title: "Mais adiante", items: tasks.filter((t) => t.due_date && t.due_date > weekEnd) },
    { title: "Sem data", items: tasks.filter((t) => !t.due_date) },
  ].filter((g) => g.items.length > 0);

  return (
    <>
      <PageHeader title="Tarefas e metas">O que precisa ser feito, organizado por prazo, e as metas maiores que essas tarefas ajudam a mover.</PageHeader>

      <div className="grid gap-4 lg:grid-cols-5">
        <div className="space-y-4 lg:col-span-3">
          <Panel>
            <form action={addTask} className="grid gap-2 sm:grid-cols-[1fr_auto_auto]">
              <input name="title" required placeholder="O que precisa ser feito?" className="field" />
              <input name="due_date" type="date" aria-label="Prazo" className="field sm:w-40" />
              <select name="priority" defaultValue="2" aria-label="Prioridade" className="field sm:w-28">
                <option value="1">Alta</option>
                <option value="2">Média</option>
                <option value="3">Baixa</option>
              </select>
              {goals.length > 0 && (
                <select name="goal_id" defaultValue="" aria-label="Meta relacionada" className="field sm:col-span-2">
                  <option value="">Sem meta relacionada</option>
                  {goals.map((g) => <option key={g.id} value={g.id}>{g.title}</option>)}
                </select>
              )}
              <SubmitButton className={`btn ${goals.length ? "" : "sm:col-span-3"}`}><Plus size={16} /> Adicionar tarefa</SubmitButton>
            </form>
          </Panel>

          {groups.length === 0 ? (
            <Panel><Empty>Nenhuma tarefa em aberto. Aproveite ou planeje a próxima.</Empty></Panel>
          ) : (
            groups.map((g) => (
              <Panel key={g.title} title={`${g.title} (${g.items.length})`}>
                <ul className="divide-y divide-line">
                  {g.items.map((t) => <TaskRow key={t.id} task={t} goalName={goalName(t.goal_id)} overdue={g.overdue} />)}
                </ul>
              </Panel>
            ))
          )}

          {(done ?? []).length > 0 && (
            <details className="panel">
              <summary className="cursor-pointer text-sm font-semibold text-muted">Concluídas recentemente</summary>
              <ul className="mt-3 divide-y divide-line">
                {(done as Task[]).map((t) => <TaskRow key={t.id} task={t} />)}
              </ul>
            </details>
          )}
        </div>

        <div className="space-y-4 lg:col-span-2">
          <Panel title="Metas">
            {goals.length === 0 ? (
              <Empty>Defina uma meta com um número para acompanhar, como “Ler 12 livros” ou “Juntar R$ 5.000”.</Empty>
            ) : (
              <ul className="space-y-5">
                {goals.map((g) => {
                  const pct = (g.current / g.target) * 100;
                  return (
                    <li key={g.id}>
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="font-medium">{g.title}</p>
                          <p className="text-xs text-muted">
                            {g.area}{g.deadline ? ` · até ${fmtDate(g.deadline, { day: "2-digit", month: "short", year: "numeric" })}` : ""}
                          </p>
                        </div>
                        <DeleteButton id={g.id} action={deleteGoal} label={`Excluir meta ${g.title}`} />
                      </div>
                      <div className="mt-2 flex items-center gap-3">
                        <div className="flex-1"><Progress value={pct} label={`Progresso de ${g.title}`} /></div>
                        <span className="w-12 text-right text-sm font-semibold">{Math.min(100, Math.round(pct))}%</span>
                      </div>
                      <form action={updateGoal} className="mt-2 flex items-center gap-2 text-sm text-muted">
                        <input type="hidden" name="id" value={g.id} />
                        <input name="current" inputMode="decimal" defaultValue={g.current} aria-label={`Valor atual de ${g.title}`} className="field !w-24 !py-1" />
                        <span>de {g.target.toLocaleString("pt-BR")} {g.unit}</span>
                        <SubmitButton className="ml-auto text-sm font-semibold text-ink underline-offset-4 hover:underline">Atualizar</SubmitButton>
                      </form>
                    </li>
                  );
                })}
              </ul>
            )}
          </Panel>

          <Panel title="Nova meta">
            <form action={addGoal} className="space-y-2">
              <input name="title" required placeholder="Ex.: Ler 12 livros em 2027" className="field" />
              <div className="grid grid-cols-2 gap-2">
                <input name="target" required inputMode="decimal" placeholder="Alvo (ex.: 12)" className="field" />
                <input name="unit" placeholder="Unidade (livros, R$, km)" className="field" />
                <select name="area" defaultValue="Pessoal" aria-label="Área" className="field">
                  {["Pessoal", "Saúde", "Treino", "Finanças", "Carreira", "Estudos", "Projetos"].map((a) => <option key={a}>{a}</option>)}
                </select>
                <input name="deadline" type="date" aria-label="Prazo da meta" className="field" />
              </div>
              <SubmitButton><Plus size={16} /> Criar meta</SubmitButton>
            </form>
          </Panel>
        </div>
      </div>
    </>
  );
}
