import type { Account, Card, Category, Transaction } from '../api/types'
import { categoryLabel } from './labels'

/** Resolve ids de conta/cartão/categoria em nomes para exibir os lançamentos. */
export function makeLookup(accounts: Account[] = [], cards: Card[] = [], categories: Category[] = []) {
  const account = (id?: string | null) => accounts.find((a) => a.id === id)?.name
  const card = (id?: string | null) => cards.find((c) => c.id === id)?.name
  const category = (id?: string | null) => {
    const c = categories.find((x) => x.id === id)
    return c ? { label: categoryLabel(c, categories), color: c.color } : undefined
  }
  /** "Corrente", "Nubank · 2/3", "Corrente → Poupança". */
  const place = (t: Transaction) => {
    if (t.kind === 'TRANSFERENCIA') return `${account(t.accountId)} → ${account(t.toAccountId)}`
    if (t.kind === 'PAGAMENTO_FATURA') return `${account(t.accountId)} → ${card(t.cardId)}`
    const where = t.cardId ? card(t.cardId) : account(t.accountId)
    return t.installmentTotal ? `${where} · ${t.installmentNo}/${t.installmentTotal}` : (where ?? '')
  }
  return { account, card, category, place }
}
