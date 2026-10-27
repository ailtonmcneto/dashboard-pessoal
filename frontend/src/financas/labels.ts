import type { AccountKind, BudgetStatus, Category, Frequency, InvoiceStatus, TransactionKind } from '../api/types'
import type { Tone } from '../components/StatusBadge'

export const ACCOUNT_KIND: Record<AccountKind, string> = {
  CORRENTE: 'Conta corrente',
  POUPANCA: 'Poupança',
  CARTEIRA: 'Carteira',
  INVESTIMENTO: 'Investimento',
}

export const TRANSACTION_KIND: Record<TransactionKind, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
  TRANSFERENCIA: 'Transferência',
  PAGAMENTO_FATURA: 'Pagamento de fatura',
}

export const FREQUENCY: Record<Frequency, string> = { SEMANAL: 'Semanal', MENSAL: 'Mensal', ANUAL: 'Anual' }

export const INVOICE_STATUS: Record<InvoiceStatus, { label: string; tone: Tone }> = {
  ABERTA: { label: 'Aberta', tone: 'neutral' },
  FECHADA: { label: 'Fechada', tone: 'warning' },
  ATRASADA: { label: 'Atrasada', tone: 'critical' },
  PAGA: { label: 'Paga', tone: 'good' },
}

export const BUDGET_STATUS: Record<BudgetStatus, { label: string; tone: Tone }> = {
  OK: { label: 'Dentro do limite', tone: 'good' },
  ATENCAO: { label: 'Perto do limite', tone: 'warning' },
  ESTOURADO: { label: 'Estourado', tone: 'critical' },
}

/** "Moradia › Aluguel" para subcategorias. */
export function categoryLabel(c: Category, all: Category[]): string {
  const parent = c.parentId ? all.find((p) => p.id === c.parentId) : undefined
  return parent ? `${parent.name} › ${c.name}` : c.name
}

/** Categorias de um tipo, pai seguido das filhas, sem as arquivadas. */
export function categoryOptions(all: Category[], kind: 'RECEITA' | 'DESPESA'): Category[] {
  const active = all.filter((c) => c.kind === kind && !c.archived)
  return active
    .filter((c) => !c.parentId)
    .flatMap((root) => [root, ...active.filter((c) => c.parentId === root.id)])
}
