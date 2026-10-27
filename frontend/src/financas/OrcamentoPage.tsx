import { Copy, Trash2 } from 'lucide-react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import { Button, Card, Empty, ErrorText, Field, IconButton, Input, Loaded, SectionTitle, Select } from '../components/ui'
import { formData } from '../lib/forms'
import { addMonths, formatMonth } from '../lib/format'
import { useBudgets, useCategories, useCopyBudgets, useDeleteBudget, useUpsertBudget } from './api'
import { categoryLabel, categoryOptions } from './labels'
import { MonthPicker } from './MonthPicker'
import { useFinanceMonth } from './month'
import { BudgetMeters } from './VisaoGeralPage'

export function OrcamentoPage() {
  const { month, setMonth } = useFinanceMonth()
  const budgets = useBudgets(month)
  const categories = useCategories().data ?? []
  const upsert = useUpsertBudget()
  const remove = useDeleteBudget()
  const copy = useCopyBudgets()
  const errors = upsert.error instanceof ApiError ? upsert.error.errors : {}
  const previous = addMonths(month, -1)

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    upsert.mutate(
      { categoryId: f.required('categoryId'), month, limitAmount: f.number('limitAmount') ?? 0 },
      { onSuccess: () => form.reset() },
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <MonthPicker value={month} onChange={setMonth} />
          <Button variant="ghost" onClick={() => copy.mutate({ from: previous, to: month })} disabled={copy.isPending}>
            <Copy size={15} /> Copiar de {formatMonth(previous)}
          </Button>
        </div>
        <ErrorText error={copy.error} />
        <Card>
          <Loaded query={budgets}>
            {(list) =>
              list.length === 0 ? (
                <Empty>Nenhum limite definido para este mês.</Empty>
              ) : (
                <>
                  <BudgetMeters budgets={list} />
                  <ul className="mt-4 flex flex-wrap gap-2 border-t border-line pt-3">
                    {list.map((b) => (
                      <li key={b.id}>
                        <IconButton
                          label={`Remover limite de ${categories.find((c) => c.id === b.categoryId)?.name}`}
                          onClick={() => remove.mutate(b.id)}
                          className="inline-flex items-center gap-1 text-xs"
                        >
                          <Trash2 size={13} /> {categories.find((c) => c.id === b.categoryId)?.name}
                        </IconButton>
                      </li>
                    ))}
                  </ul>
                </>
              )
            }
          </Loaded>
        </Card>
      </div>
      <Card className="self-start">
        <SectionTitle>Definir limite</SectionTitle>
        <p className="mb-3 text-xs text-muted">
          O gasto de uma categoria inclui as subcategorias. Definir de novo a mesma categoria atualiza o limite.
        </p>
        <form onSubmit={submit} className="space-y-3">
          <Field label="Categoria" error={errors.categoryId}>
            <Select name="categoryId" required defaultValue="">
              <option value="" disabled>
                Escolha…
              </option>
              {categoryOptions(categories, 'DESPESA').map((c) => (
                <option key={c.id} value={c.id}>
                  {categoryLabel(c, categories)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Limite no mês (R$)" error={errors.limitAmount}>
            <Input name="limitAmount" type="number" step="0.01" min="0.01" required />
          </Field>
          <ErrorText error={Object.keys(errors).length ? null : upsert.error} />
          <Button type="submit" disabled={upsert.isPending}>
            Salvar
          </Button>
        </form>
      </Card>
    </div>
  )
}
