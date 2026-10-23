import { TZ, fmtDateTime } from "./format";

export type EventRow = { id: string; title: string; starts_at: string; ends_at: string | null; location: string | null };

/** Data local (YYYY-MM-DD, Brasília) de um timestamp. */
export const localDay = (ts: string) => new Intl.DateTimeFormat("en-CA", { timeZone: TZ }).format(new Date(ts));

export function timeRange(e: EventRow) {
  const t = (ts: string) => fmtDateTime(ts, { hour: "2-digit", minute: "2-digit" });
  return e.ends_at ? `${t(e.starts_at)}–${t(e.ends_at)}` : t(e.starts_at);
}
