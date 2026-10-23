import { addDays } from "./format";

/** Dias seguidos com o hábito feito, contando de hoje (ou de ontem, se hoje ainda está em aberto). */
export function streak(doneDates: Set<string>, todayIso: string): number {
  let day = doneDates.has(todayIso) ? todayIso : addDays(todayIso, -1);
  let n = 0;
  while (doneDates.has(day)) {
    n++;
    day = addDays(day, -1);
  }
  return n;
}
