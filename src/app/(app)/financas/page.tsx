import Link from "next/link";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import { requireUser } from "@/lib/supabase/server";
import { fmtDate, fmtMonth, monthStart, money, today } from "@/lib/format";
import { Empty, PageHeader, Panel } from "@/components/page-header";
import { DeleteButton, SubmitButton } from "@/components/ui";
import { CashflowChart, type MonthFlow } from "@/components/charts";
import { addTransaction, deleteTransaction } from "./actions";

type Tx = { id: string; date: string; description: string; amount: number; type: "receita" | "despesa"; category: string };

const CATEGORIAS = ["Moradia", "Alimentação", "Mercado", "Transporte", "Saúde", "Educação", "Lazer", "Assinaturas", "Salário", "Freela", "Investimentos", "Outros"];

export default async function FinancasPage({ searchParams }: PageProps<"/financas">) {
  const { supabase } = await requireUser();
  const { mes } = await searchParams;
  const now = today();
  const start = typeof mes === "string" && /^\d{4}-\d{2}$/.test(mes) ? `${mes}-01` : monthStart(now);
  const end = monthStart(start, 1);
  const chartStart = monthStart(start, -5);

  const { data } = await supabase
    .from("transactions")
    .select("id,date,description,amount,type,category")
    .gte("date", chartStart)
    .lt("date", end)
    .order("date", { ascending: false })
    .order("created_at", { ascending: false });
  const all = (data ?? []).map((t) => ({ ...t, amount: Number(t.amount) })) as Tx[];

  const month = all.filter((t) => t.date >= start);
  const receitas = sum(month.filter((t) => t.type === "receita"));
  const despesas = sum(month.filter((t) => t.type === "despesa"));

  const flow: MonthFlow[] = Array.from({ length: 6 }, (_, i) => {
    const s = monthStart(start, i - 5);
    const e = monthStart(start, i - 4);
    const inMonth = all.filter((t) => t.date >= s && t.date < e);
    return {
      mes: fmtMonth(s),
      receitas: sum(inMonth.filter((t) => t.type === "receita")),
      despesas: sum(inMonth.filter((t) => t.type === "despesa")),
    };
  });

  const byCategory = Object.entries(
    month.filter((t) => t.type === "despesa").reduce<Record<string, number>>((acc, t) => {
      acc[t.category] = (acc[t.category] ?? 0) + t.amount;
      return acc;
    }, {}),
  ).sort((a, b) => b[1] - a[1]);
  const maxCat = byCategory[0]?.[1] ?? 0;

  const monthLabel = fmtDate(start, { month: "long", year: "numeric" });
  const prev = monthStart(start, -1).slice(0, 7);
  const next = end.slice(0, 7);
  const isCurrent = start === monthStart(now);

  return (
    <>
      <PageHeader title="Finanças">Entradas e saídas do mês, para onde o dinheiro está indo e como os últimos meses se comparam.</PageHeader>

      <div className="mb-6 flex items-center gap-2">
        <Link href={`/financas?mes=${prev}`} className="btn-ghost" aria-label="Mês anterior"><ChevronLeft size={18} /></Link>
        <p className="min-w-40 text-center font-semibold capitalize">{monthLabel}</p>
        <Link href={`/financas?mes=${next}`} className="btn-ghost" aria-label="Próximo mês"><ChevronRight size={18} /></Link>
        {!isCurrent && <Link href="/financas" className="ml-2 text-sm text-muted underline-offset-4 hover:underline">Voltar para o mês atual</Link>}
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="Receitas" value={money(receitas)} swatch="var(--in)" />
        <Stat label="Despesas" value={money(despesas)} swatch="var(--out)" />
        <Stat label="Saldo do mês" value={money(receitas - despesas)} tone={receitas - despesas >= 0 ? "good" : "bad"} />
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-5">
        <Panel title="Últimos 6 meses" className="lg:col-span-3">
          <CashflowChart data={flow} />
        </Panel>

        <Panel title="Despesas por categoria" className="lg:col-span-2">
          {byCategory.length === 0 ? (
            <Empty>Nenhuma despesa neste mês.</Empty>
          ) : (
            <ul className="space-y-3">
              {byCategory.map(([cat, val]) => (
                <li key={cat}>
                  <div className="mb-1 flex justify-between text-sm">
                    <span>{cat}</span>
                    <span className="text-muted">{money(val)} · {Math.round((val / despesas) * 100)}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-bg">
                    <div className="h-full rounded-full bg-out" style={{ width: `${(val / maxCat) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-5">
        <Panel title="Novo lançamento" className="lg:col-span-2">
          <form action={addTransaction} className="space-y-3">
            <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Tipo">
              <TypeOption value="despesa" label="Despesa" defaultChecked />
              <TypeOption value="receita" label="Receita" />
            </div>
            <input name="description" required placeholder="Descrição (ex.: Mercado do mês)" className="field" />
            <div className="grid grid-cols-2 gap-2">
              <input name="amount" required inputMode="decimal" placeholder="Valor (R$)" className="field" />
              <input name="date" type="date" required defaultValue={isCurrent ? now : start} className="field" />
            </div>
            <input name="category" list="categorias" placeholder="Categoria" className="field" />
            <datalist id="categorias">{CATEGORIAS.map((c) => <option key={c} value={c} />)}</datalist>
            <SubmitButton><Plus size={16} /> Adicionar lançamento</SubmitButton>
          </form>
        </Panel>

        <Panel title="Lançamentos do mês" className="lg:col-span-3">
          {month.length === 0 ? (
            <Empty>Nada lançado em {monthLabel}. Comece pelo formulário ao lado.</Empty>
          ) : (
            <ul className="divide-y divide-line">
              {month.map((t) => (
                <li key={t.id} className="flex items-center gap-3 py-2.5">
                  <span className="h-2 w-2 shrink-0 rounded-full" style={{ background: t.type === "receita" ? "var(--in)" : "var(--out)" }} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{t.description}</p>
                    <p className="text-xs text-muted">{fmtDate(t.date)} · {t.category}</p>
                  </div>
                  <span className="text-sm font-semibold">{t.type === "receita" ? "+" : "−"} {money(t.amount)}</span>
                  <DeleteButton id={t.id} action={deleteTransaction} label={`Excluir ${t.description}`} />
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>
    </>
  );
}

function sum(list: Tx[]) {
  return list.reduce((acc, t) => acc + t.amount, 0);
}

function Stat({ label, value, swatch, tone }: { label: string; value: string; swatch?: string; tone?: "good" | "bad" }) {
  return (
    <div className="panel">
      <p className="flex items-center gap-2 text-sm text-muted">
        {swatch && <span className="h-2 w-2 rounded-full" style={{ background: swatch }} />}
        {label}
      </p>
      <p className={`mt-1 text-2xl font-bold tracking-tight ${tone === "bad" ? "text-bad" : tone === "good" ? "text-good" : ""}`}>{value}</p>
    </div>
  );
}

function TypeOption({ value, label, defaultChecked }: { value: string; label: string; defaultChecked?: boolean }) {
  return (
    <label className="cursor-pointer">
      <input type="radio" name="type" value={value} defaultChecked={defaultChecked} className="peer sr-only" />
      <span className="block rounded-lg border border-line py-2 text-center text-sm font-medium text-muted peer-checked:border-ink peer-checked:text-ink peer-focus-visible:outline-2 peer-focus-visible:outline-sun">
        {label}
      </span>
    </label>
  );
}
