export const TZ = "America/Sao_Paulo";

/** Data de hoje (YYYY-MM-DD) no fuso de Brasília. */
export function today(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: TZ }).format(new Date());
}

/** Soma dias a uma data YYYY-MM-DD. */
export function addDays(iso: string, days: number): string {
  const d = new Date(`${iso}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** Primeiro dia do mês de uma data YYYY-MM-DD, deslocado `offset` meses. */
export function monthStart(iso: string, offset = 0): string {
  const [y, m] = iso.split("-").map(Number);
  const d = new Date(Date.UTC(y, m - 1 + offset, 1));
  return d.toISOString().slice(0, 10);
}

const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
export const money = (n: number) => brl.format(n);

export function fmtDate(iso: string, opts: Intl.DateTimeFormatOptions = { day: "2-digit", month: "short" }) {
  return new Intl.DateTimeFormat("pt-BR", { ...opts, timeZone: "UTC" }).format(new Date(`${iso}T12:00:00Z`));
}

export function fmtDateTime(ts: string, opts: Intl.DateTimeFormatOptions) {
  return new Intl.DateTimeFormat("pt-BR", { ...opts, timeZone: TZ }).format(new Date(ts));
}

export function fmtMonth(iso: string) {
  return fmtDate(iso, { month: "short" }).replace(".", "");
}

/** Converte "2026-10-22T14:30" (datetime-local, horário de Brasília) para ISO com fuso. */
export function localToIso(value: string): string {
  return new Date(`${value}:00-03:00`).toISOString();
}
