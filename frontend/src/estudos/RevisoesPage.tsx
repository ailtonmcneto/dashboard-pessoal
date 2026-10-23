import { Plus } from 'lucide-react'
import type { FormEvent } from 'react'
import { Button, Card, Empty, ErrorText, Input, Loaded, SectionTitle } from '../components/ui'
import { formData } from '../lib/forms'
import { useCreateTopic, useDeleteTopic, useTopics } from './api'
import { TopicRow } from './TopicsSection'

export function RevisoesPage() {
  const due = useTopics({ due: true })
  const all = useTopics()
  const create = useCreateTopic()
  const remove = useDeleteTopic()

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    create.mutate({ title: formData(form).required('title') }, { onSuccess: () => form.reset() })
  }

  const upcoming = (all.data ?? []).filter((t) => !t.due)

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <div className="space-y-6">
        <Card>
          <SectionTitle>Para revisar hoje</SectionTitle>
          <Loaded query={due}>
            {(data) =>
              data.length === 0 ? (
                <Empty>Nada para revisar hoje. 🎉</Empty>
              ) : (
                <ul className="divide-y divide-line">
                  {data.map((t) => (
                    <TopicRow key={t.id} topic={t} />
                  ))}
                </ul>
              )
            }
          </Loaded>
        </Card>
        {upcoming.length > 0 && (
          <Card>
            <SectionTitle>Próximas revisões</SectionTitle>
            <ul className="divide-y divide-line">
              {[...upcoming]
                .sort((a, b) => (a.nextReviewOn ?? '').localeCompare(b.nextReviewOn ?? ''))
                .map((t) => (
                  <TopicRow key={t.id} topic={t} onDelete={() => remove.mutate(t.id)} />
                ))}
            </ul>
          </Card>
        )}
      </div>
      <Card className="self-start">
        <SectionTitle>Assunto avulso</SectionTitle>
        <p className="mb-3 text-xs text-muted">
          Assuntos de uma disciplina são criados na página dela. Aqui entram os que não são de nenhuma.
        </p>
        <form onSubmit={submit} className="flex gap-2">
          <Input name="title" placeholder="Ex.: inglês — phrasal verbs" aria-label="Novo assunto" required maxLength={200} />
          <Button type="submit" variant="ghost" disabled={create.isPending} aria-label="Adicionar assunto">
            <Plus size={16} />
          </Button>
        </form>
        <ErrorText error={create.error} />
      </Card>
    </div>
  )
}
