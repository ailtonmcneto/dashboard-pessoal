import { Card, PageHeader } from '../components/ui'

export function ComingSoonPage({ title, phase, items }: { title: string; phase: number; items: string[] }) {
  return (
    <>
      <PageHeader title={title} subtitle={`Chega na fase ${phase}`} />
      <Card>
        <ul className="list-inside list-disc space-y-1 text-sm text-muted">
          {items.map((i) => (
            <li key={i}>{i}</li>
          ))}
        </ul>
      </Card>
    </>
  )
}
