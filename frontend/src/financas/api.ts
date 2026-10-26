import { useGet, useSend } from '../api/hooks'
import type {
  Account,
  AccountRequest,
  Budget,
  BudgetRequest,
  Card,
  CardRequest,
  Category,
  CategoryRequest,
  Invoice,
  MonthSummary,
  MonthTotals,
  PayInvoiceRequest,
  Recurrence,
  RecurrenceRequest,
  Transaction,
  TransactionRequest,
} from '../api/types'

/** Quase tudo em finanças mexe em saldo/resumo: recarrega o módulo inteiro. */
const ALL = ['accounts', 'cards', 'categories', 'transactions', 'recurrences', 'budgets', 'finance']

type WithId<T> = T & { id: string }

// ---------- leitura ----------

export const useAccounts = () => useGet<Account[]>('/accounts')
export const useCards = () => useGet<Card[]>('/cards')
export const useInvoice = (cardId: string | null, month: string) =>
  useGet<Invoice>(cardId ? `/cards/${cardId}/invoices/${month}` : null)
export const useCategories = () => useGet<Category[]>('/categories')
export const useTransactions = (
  month: string,
  filter: { accountId?: string; cardId?: string; categoryId?: string },
) => {
  const qs = new URLSearchParams({ month })
  Object.entries(filter).forEach(([k, v]) => v && qs.set(k, v))
  return useGet<Transaction[]>(`/transactions?${qs}`)
}
export const useRecurrences = () => useGet<Recurrence[]>('/recurrences')
export const useBudgets = (month: string) => useGet<Budget[]>(`/budgets?month=${month}`)
export const useMonthSummary = (month: string) => useGet<MonthSummary>(`/finance/summary?month=${month}`)
export const useHistory = (until: string, months = 6) =>
  useGet<MonthTotals[]>(`/finance/history?until=${until}&months=${months}`)

// ---------- escrita ----------

export const useCreateAccount = () => useSend<AccountRequest>('POST', () => '/accounts', ALL)
export const useUpdateAccount = () => useSend<WithId<AccountRequest>>('PUT', (a) => `/accounts/${a.id}`, ALL)
export const useDeleteAccount = () => useSend<string>('DELETE', (id) => `/accounts/${id}`, ALL)

export const useCreateCard = () => useSend<CardRequest>('POST', () => '/cards', ALL)
export const useUpdateCard = () => useSend<WithId<CardRequest>>('PUT', (c) => `/cards/${c.id}`, ALL)
export const useDeleteCard = () => useSend<string>('DELETE', (id) => `/cards/${id}`, ALL)
export const usePayInvoice = (cardId: string, month: string) =>
  useSend<PayInvoiceRequest>('POST', () => `/cards/${cardId}/invoices/${month}/pay`, ALL)

export const useCreateCategory = () => useSend<CategoryRequest>('POST', () => '/categories', ALL)
export const useUpdateCategory = () => useSend<WithId<CategoryRequest>>('PUT', (c) => `/categories/${c.id}`, ALL)
export const useDeleteCategory = () => useSend<string>('DELETE', (id) => `/categories/${id}`, ALL)
export const useDefaultCategories = () => useSend<void>('POST', () => '/categories/defaults', ALL)

export const useCreateTransaction = () => useSend<TransactionRequest>('POST', () => '/transactions', ALL)
export const useUpdateTransaction = () =>
  useSend<WithId<TransactionRequest>>('PUT', (t) => `/transactions/${t.id}`, ALL)
export const useDeleteTransaction = () =>
  useSend<{ id: string; allInstallments: boolean }>(
    'DELETE',
    (d) => `/transactions/${d.id}?allInstallments=${d.allInstallments}`,
    ALL,
  )

export const useCreateRecurrence = () => useSend<RecurrenceRequest>('POST', () => '/recurrences', ALL)
export const useUpdateRecurrence = () => useSend<WithId<RecurrenceRequest>>('PUT', (r) => `/recurrences/${r.id}`, ALL)
export const useDeleteRecurrence = () => useSend<string>('DELETE', (id) => `/recurrences/${id}`, ALL)
export const useToggleRecurrence = () =>
  useSend<{ id: string; active: boolean }>('POST', (r) => `/recurrences/${r.id}/${r.active ? 'resume' : 'pause'}`, ALL)

export const useUpsertBudget = () => useSend<BudgetRequest>('PUT', () => '/budgets', ALL)
export const useDeleteBudget = () => useSend<string>('DELETE', (id) => `/budgets/${id}`, ALL)
export const useCopyBudgets = () =>
  useSend<{ from: string; to: string }>('POST', (c) => `/budgets/copy?from=${c.from}&to=${c.to}`, ALL)
