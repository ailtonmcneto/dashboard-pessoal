import { useOutletContext } from 'react-router'

export type MonthContext = { month: string; setMonth: (m: string) => void }

/** O mês escolhido vale para todas as abas de Finanças (troca de aba sem perder o mês). */
export function useFinanceMonth() {
  return useOutletContext<MonthContext>()
}
