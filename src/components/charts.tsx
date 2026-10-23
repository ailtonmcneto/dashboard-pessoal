"use client";

import {
  Bar, BarChart, CartesianGrid, Legend, Line, LineChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";

const axis = { stroke: "var(--muted)", fontSize: 12, tickLine: false, axisLine: false } as const;
const tooltipStyle = {
  contentStyle: { background: "var(--surface)", border: "1px solid var(--line)", borderRadius: 10, color: "var(--ink)", fontSize: 13 },
  labelStyle: { color: "var(--muted)", marginBottom: 4 },
  itemStyle: { color: "var(--ink)" },
};
const brl = (n: number) => n.toLocaleString("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 });

export type MonthFlow = { mes: string; receitas: number; despesas: number };

export function CashflowChart({ data }: { data: MonthFlow[] }) {
  return (
    <div className="h-64 w-full">
      <ResponsiveContainer>
        <BarChart data={data} barGap={2} barCategoryGap="28%" margin={{ top: 8, right: 4, left: 4, bottom: 0 }}>
          <CartesianGrid vertical={false} stroke="var(--line)" />
          <XAxis dataKey="mes" {...axis} />
          <YAxis {...axis} width={64} tickFormatter={(v: number) => brl(v)} />
          <Tooltip {...tooltipStyle} cursor={{ fill: "var(--bg)" }} formatter={(v) => brl(Number(v))} />
          <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 13, color: "var(--muted)" }} />
          <Bar dataKey="receitas" name="Receitas" fill="var(--in)" radius={[4, 4, 0, 0]} maxBarSize={28} />
          <Bar dataKey="despesas" name="Despesas" fill="var(--out)" radius={[4, 4, 0, 0]} maxBarSize={28} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function SleepChart({ data }: { data: { dia: string; sono: number | null }[] }) {
  return (
    <div className="h-48 w-full">
      <ResponsiveContainer>
        <LineChart data={data} margin={{ top: 8, right: 8, left: 4, bottom: 0 }}>
          <CartesianGrid vertical={false} stroke="var(--line)" />
          <XAxis dataKey="dia" {...axis} interval="preserveStartEnd" />
          <YAxis {...axis} width={32} domain={[0, 12]} ticks={[0, 4, 8, 12]} unit="h" />
          <Tooltip {...tooltipStyle} formatter={(v) => [`${v} h`, "Sono"]} cursor={{ stroke: "var(--muted)", strokeDasharray: "3 3" }} />
          <Line type="monotone" dataKey="sono" stroke="var(--in)" strokeWidth={2} dot={{ r: 3, fill: "var(--in)", strokeWidth: 0 }} activeDot={{ r: 5, stroke: "var(--surface)", strokeWidth: 2 }} connectNulls />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
