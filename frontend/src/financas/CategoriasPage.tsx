import { Archive, ArchiveRestore, Trash2 } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import type { Category, CategoryKind } from '../api/types'
import { EditButton } from '../components/Modal'
import {
  Button,
  Card,
  Empty,
  ErrorText,
  Field,
  IconButton,
  Input,
  Loaded,
  SectionTitle,
  Select,
} from '../components/ui'
import { ColorDot } from '../estudos/parts'
import { COURSE_COLORS } from '../estudos/labels'
import { formData } from '../lib/forms'
import { useCategories, useCreateCategory, useDefaultCategories, useDeleteCategory, useUpdateCategory } from './api'

function CategoryItem({ c, all, child = false }: { c: Category; all: Category[]; child?: boolean }) {
  const update = useUpdateCategory()
  const remove = useDeleteCategory()
  return (
    <li className={`flex items-center gap-2 py-1.5 ${child ? 'pl-6' : ''} ${c.archived ? 'opacity-50' : ''}`}>
      <ColorDot color={c.color} />
      <span className="flex-1 text-sm">{c.name}</span>
      <EditButton label={`Editar ${c.name}`} title="Editar categoria">
        {(close) => <CategoryForm all={all} initial={c} onDone={close} />}
      </EditButton>
      <IconButton
        label={c.archived ? `Desarquivar ${c.name}` : `Arquivar ${c.name}`}
        onClick={() =>
          update.mutate({
            id: c.id,
            name: c.name,
            kind: c.kind,
            parentId: c.parentId,
            color: c.color,
            archived: !c.archived,
          })
        }
      >
        {c.archived ? <ArchiveRestore size={14} /> : <Archive size={14} />}
      </IconButton>
      <IconButton
        label={`Apagar ${c.name}`}
        onClick={() =>
          confirm(`Apagar “${c.name}”? Os lançamentos ficam sem categoria. Arquivar mantém o histórico.`) &&
          remove.mutate(c.id)
        }
      >
        <Trash2 size={14} />
      </IconButton>
    </li>
  )
}

function Tree({ all, kind }: { all: Category[]; kind: CategoryKind }) {
  const roots = all.filter((c) => c.kind === kind && !c.parentId)
  if (roots.length === 0) return <Empty>Nenhuma.</Empty>
  return (
    <ul>
      {roots.map((r) => (
        <li key={r.id}>
          <ul>
            <CategoryItem c={r} all={all} />
            {all
              .filter((c) => c.parentId === r.id)
              .map((c) => (
                <CategoryItem key={c.id} c={c} all={all} child />
              ))}
          </ul>
        </li>
      ))}
    </ul>
  )
}

/** Cria uma categoria ou, com `initial`, edita essa categoria (nome, cor e onde ela fica; o tipo não muda). */
function CategoryForm({ all, initial, onDone }: { all: Category[]; initial?: Category; onDone?: () => void }) {
  const create = useCreateCategory()
  const update = useUpdateCategory()
  const save = initial ? update : create
  const [kind, setKind] = useState<CategoryKind>(initial?.kind ?? 'DESPESA')
  const [color, setColor] = useState(initial?.color ?? COURSE_COLORS[1])
  const errors = save.error instanceof ApiError ? save.error.errors : {}
  // só dois níveis: quem já tem subcategorias não pode virar subcategoria
  const hasChildren = initial != null && all.some((c) => c.parentId === initial.id)
  const parents = all.filter((c) => c.kind === kind && !c.parentId && !c.archived && c.id !== initial?.id)

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const req = { name: f.required('name'), kind, parentId: f.text('parentId'), color }
    if (initial) update.mutate({ id: initial.id, ...req, archived: initial.archived }, { onSuccess: onDone })
    else create.mutate(req, { onSuccess: () => form.reset() })
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Tipo">
        <Select value={kind} onChange={(e) => setKind(e.target.value as CategoryKind)} disabled={initial != null}>
          <option value="DESPESA">Despesa</option>
          <option value="RECEITA">Receita</option>
        </Select>
      </Field>
      <Field label="Nome" error={errors.name}>
        <Input name="name" required maxLength={60} defaultValue={initial?.name} />
      </Field>
      {!hasChildren && (
        <Field label="Dentro de (opcional)" error={errors.parentId}>
          <Select name="parentId" defaultValue={initial?.parentId ?? ''} key={kind}>
            <option value="">— categoria principal —</option>
            {parents.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </Field>
      )}
      <fieldset>
        <legend className="mb-1 text-sm font-medium">Cor</legend>
        <div className="flex flex-wrap gap-2">
          {COURSE_COLORS.map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => setColor(c)}
              aria-label={`Cor ${c}`}
              aria-pressed={color === c}
              className={`size-6 rounded-full ring-offset-2 ${color === c ? 'ring-2 ring-ink' : ''}`}
              style={{ background: c }}
            />
          ))}
        </div>
      </fieldset>
      <ErrorText error={Object.keys(errors).length ? null : save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? 'Salvar' : 'Criar'}
      </Button>
    </form>
  )
}

export function CategoriasPage() {
  const categories = useCategories()
  const defaults = useDefaultCategories()

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_1fr_320px]">
      <Loaded query={categories}>
        {(list) =>
          list.length === 0 ? (
            <Card className="lg:col-span-2">
              <Empty>Nenhuma categoria ainda.</Empty>
              <Button className="mt-3" onClick={() => defaults.mutate()} disabled={defaults.isPending}>
                Criar categorias sugeridas
              </Button>
              <ErrorText error={defaults.error} />
            </Card>
          ) : (
            <>
              <Card>
                <SectionTitle>Despesas</SectionTitle>
                <Tree all={list} kind="DESPESA" />
              </Card>
              <Card>
                <SectionTitle>Receitas</SectionTitle>
                <Tree all={list} kind="RECEITA" />
              </Card>
            </>
          )
        }
      </Loaded>
      <Card className="self-start">
        <SectionTitle>Nova categoria</SectionTitle>
        <CategoryForm all={categories.data ?? []} />
      </Card>
    </div>
  )
}
