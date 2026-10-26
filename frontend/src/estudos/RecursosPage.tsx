import { ExternalLink, Minus, Plus, Trash2 } from 'lucide-react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { ResourceKind, ResourceStatus, StudyResource } from '../api/types'
import { Meter } from '../components/charts'
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
import { formData } from '../lib/forms'
import { useCreateResource, useDeleteResource, useResources, useUpdateResource } from './api'
import { RESOURCE_KIND, RESOURCE_STATUS } from './labels'

function ResourceRow({ r }: { r: StudyResource }) {
  const update = useUpdateResource()
  const remove = useDeleteResource()

  const save = (changes: Partial<StudyResource>) =>
    update.mutate({
      id: r.id,
      title: r.title,
      kind: r.kind,
      provider: r.provider,
      url: r.url,
      totalUnits: r.totalUnits,
      doneUnits: r.doneUnits,
      status: r.status,
      startedOn: r.startedOn,
      finishedOn: r.finishedOn,
      ...changes,
    })

  return (
    <li className="space-y-2 py-3">
      <div className="flex flex-wrap items-center gap-2">
        <div className="min-w-0 flex-1">
          <p className="flex items-center gap-1 font-medium">
            <span className="truncate">{r.title}</span>
            {r.url && (
              <a
                href={r.url}
                target="_blank"
                rel="noreferrer"
                className="text-muted hover:text-accent"
                aria-label="Abrir link"
              >
                <ExternalLink size={14} />
              </a>
            )}
          </p>
          <p className="text-xs text-muted">{[RESOURCE_KIND[r.kind], r.provider].filter(Boolean).join(' · ')}</p>
        </div>
        <div className="w-36">
          <Select
            aria-label={`Situação de ${r.title}`}
            value={r.status}
            onChange={(e) => save({ status: e.target.value as ResourceStatus, finishedOn: null })}
          >
            {Object.entries(RESOURCE_STATUS).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </div>
        <EditButton label={`Editar ${r.title}`} title="Editar recurso">
          {(close) => <ResourceForm initial={r} onDone={close} />}
        </EditButton>
        <IconButton label={`Excluir ${r.title}`} onClick={() => remove.mutate(r.id)}>
          <Trash2 size={15} />
        </IconButton>
      </div>
      {r.totalUnits != null && (
        <div className="flex items-center gap-2">
          <IconButton
            label="Voltar uma unidade"
            disabled={r.doneUnits === 0 || update.isPending}
            onClick={() => save({ doneUnits: r.doneUnits - 1 })}
          >
            <Minus size={14} />
          </IconButton>
          <div className="flex-1">
            <Meter value={r.doneUnits} max={r.totalUnits} label={`Progresso de ${r.title}`} />
          </div>
          <IconButton
            label="Avançar uma unidade"
            disabled={r.doneUnits >= r.totalUnits || update.isPending}
            onClick={() => save({ doneUnits: r.doneUnits + 1 })}
          >
            <Plus size={14} />
          </IconButton>
          <span className="w-24 text-right text-xs tabular-nums text-muted">
            {r.doneUnits}/{r.totalUnits} · {r.progressPercent}%
          </span>
        </div>
      )}
      <ErrorText error={update.error} />
    </li>
  )
}

/** Cria um recurso ou, com `initial`, edita esse recurso. */
function ResourceForm({ initial, onDone }: { initial?: StudyResource; onDone?: () => void }) {
  const create = useCreateResource()
  const update = useUpdateResource()
  const save = initial ? update : create
  const errors = save.error instanceof ApiError ? save.error.errors : {}

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const f = formData(form)
    const req = {
      title: f.required('title'),
      kind: f.text('kind') as ResourceKind,
      provider: f.text('provider'),
      url: f.text('url'),
      totalUnits: f.number('totalUnits'),
      doneUnits: f.number('doneUnits'),
      status: f.text('status') as ResourceStatus,
    }
    if (initial) {
      const sameStatus = req.status === initial.status
      update.mutate(
        {
          id: initial.id,
          ...req,
          startedOn: initial.startedOn,
          finishedOn: sameStatus ? initial.finishedOn : null,
        },
        { onSuccess: onDone },
      )
    } else create.mutate(req, { onSuccess: () => form.reset() })
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Título" error={errors.title}>
        <Input name="title" required maxLength={200} defaultValue={initial?.title} />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Tipo">
          <Select name="kind" defaultValue={initial?.kind ?? 'CURSO'}>
            {Object.entries(RESOURCE_KIND).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Situação">
          <Select name="status" defaultValue={initial?.status ?? 'ATIVO'}>
            {Object.entries(RESOURCE_STATUS).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Total (aulas, cap.…)" error={errors.totalUnits}>
          <Input name="totalUnits" type="number" min="1" defaultValue={initial?.totalUnits ?? ''} />
        </Field>
        <Field label="Já feitas" error={errors.doneUnits}>
          <Input name="doneUnits" type="number" min="0" defaultValue={initial?.doneUnits ?? ''} />
        </Field>
      </div>
      <Field label="Plataforma / autor" error={errors.provider}>
        <Input name="provider" maxLength={120} defaultValue={initial?.provider ?? ''} />
      </Field>
      <Field label="Link" error={errors.url}>
        <Input name="url" type="url" placeholder="https://" maxLength={500} defaultValue={initial?.url ?? ''} />
      </Field>
      <ErrorText error={Object.keys(errors).length ? null : save.error} />
      <Button type="submit" disabled={save.isPending}>
        {initial ? 'Salvar' : 'Adicionar'}
      </Button>
    </form>
  )
}

export function RecursosPage() {
  const list = useResources()

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <Card>
        <SectionTitle>Cursos, livros e afins</SectionTitle>
        <Loaded query={list}>
          {(data) =>
            data.length === 0 ? (
              <Empty>Nada cadastrado ainda.</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {data.map((r) => (
                  <ResourceRow key={r.id} r={r} />
                ))}
              </ul>
            )
          }
        </Loaded>
      </Card>
      <Card className="self-start">
        <SectionTitle>Adicionar</SectionTitle>
        <ResourceForm />
      </Card>
    </div>
  )
}
