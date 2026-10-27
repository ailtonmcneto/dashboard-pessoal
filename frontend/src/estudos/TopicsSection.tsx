import { Plus, Trash2 } from 'lucide-react'
import type { FormEvent } from 'react'
import type { Topic } from '../api/types'
import { EditButton } from '../components/Modal'
import { Button, Card, Empty, ErrorText, Field, IconButton, Input, Loaded, SectionTitle } from '../components/ui'
import { formData } from '../lib/forms'
import { formatDate } from '../lib/format'
import { useCreateTopic, useDeleteTopic, useReviewTopic, useTopics, useUpdateTopic } from './api'
import { MASTERY } from './labels'

/** Botões 0–5 para registrar uma revisão ("quanto você domina agora?"). */
export function ReviewButtons({ topic }: { topic: Topic }) {
  const review = useReviewTopic()
  return (
    <div className="flex gap-1" role="group" aria-label={`Revisar ${topic.title}`}>
      {MASTERY.map((label, level) => (
        <button
          key={level}
          type="button"
          title={label}
          aria-label={`${label} (${level})`}
          disabled={review.isPending}
          onClick={() => review.mutate({ id: topic.id, mastery: level })}
          className={`size-7 rounded-md border text-xs tabular-nums transition hover:border-accent hover:text-accent ${
            topic.mastery === level && topic.lastReviewedOn ? 'border-accent bg-accent-soft' : 'border-line'
          }`}
        >
          {level}
        </button>
      ))}
    </div>
  )
}

function TopicForm({ topic, onDone }: { topic: Topic; onDone: () => void }) {
  const update = useUpdateTopic()

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    update.mutate(
      {
        id: topic.id,
        title: formData(e.currentTarget).required('title'),
        courseId: topic.courseId,
        resourceId: topic.resourceId,
        parentId: topic.parentId,
      },
      { onSuccess: onDone },
    )
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Assunto">
        <Input name="title" required maxLength={200} defaultValue={topic.title} />
      </Field>
      <ErrorText error={update.error} />
      <Button type="submit" disabled={update.isPending}>
        Salvar
      </Button>
    </form>
  )
}

export function TopicRow({ topic, onDelete }: { topic: Topic; onDelete?: () => void }) {
  return (
    <li className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2">
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium">{topic.title}</p>
        <p className="text-xs text-muted">
          {topic.lastReviewedOn
            ? `Domínio: ${MASTERY[topic.mastery]} · ${
                topic.due ? 'revisar hoje' : `próxima ${formatDate(topic.nextReviewOn!)}`
              }`
            : 'Nunca revisado'}
        </p>
      </div>
      <ReviewButtons topic={topic} />
      {onDelete && (
        <EditButton label={`Editar ${topic.title}`} title="Editar assunto">
          {(close) => <TopicForm topic={topic} onDone={close} />}
        </EditButton>
      )}
      {onDelete && (
        <IconButton label={`Excluir ${topic.title}`} onClick={onDelete}>
          <Trash2 size={14} />
        </IconButton>
      )}
    </li>
  )
}

export function TopicsSection({ courseId }: { courseId: string }) {
  const list = useTopics({ courseId })
  const create = useCreateTopic()
  const remove = useDeleteTopic()

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    create.mutate({ title: formData(form).required('title'), courseId }, { onSuccess: () => form.reset() })
  }

  return (
    <Card>
      <SectionTitle>Assuntos</SectionTitle>
      <p className="mb-3 text-xs text-muted">
        Depois de estudar, marque de 0 a 5 o quanto você domina: quanto maior, mais tarde volta para revisão.
      </p>
      <Loaded query={list}>
        {(data) =>
          data.length === 0 ? (
            <Empty>Nenhum assunto ainda.</Empty>
          ) : (
            <ul className="divide-y divide-line">
              {data.map((t) => (
                <TopicRow key={t.id} topic={t} onDelete={() => remove.mutate(t.id)} />
              ))}
            </ul>
          )
        }
      </Loaded>
      <form onSubmit={submit} className="mt-3 flex gap-2">
        <Input name="title" placeholder="Novo assunto" aria-label="Novo assunto" required maxLength={200} />
        <Button type="submit" variant="ghost" disabled={create.isPending}>
          <Plus size={16} />
        </Button>
      </form>
      <ErrorText error={create.error} />
    </Card>
  )
}
