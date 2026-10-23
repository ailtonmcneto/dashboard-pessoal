import {
  type ButtonHTMLAttributes,
  cloneElement,
  type InputHTMLAttributes,
  type ReactElement,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
  useId,
} from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`rounded-xl border border-line bg-surface p-5 ${className}`}>{children}</section>
}

export function PageHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <header className="mb-6">
      <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
      {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
    </header>
  )
}

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'ghost' }) {
  const style =
    variant === 'primary'
      ? 'bg-accent text-white hover:bg-accent/90 disabled:opacity-60'
      : 'text-muted hover:text-ink hover:bg-canvas'
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition ${style} ${className}`}
      {...props}
    />
  )
}

const fieldClass =
  'w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm outline-none focus:border-accent focus:ring-2 focus:ring-accent-soft'

/**
 * Rótulo + campo + erro. O rótulo aponta para o campo por id (e não o envolve), para o nome
 * acessível ser só o texto do rótulo — num <select> envolvido, o valor escolhido entraria no nome.
 */
export function Field({
  label,
  error,
  children,
}: {
  label: string
  error?: string
  children: ReactElement<{ id?: string; 'aria-describedby'?: string; 'aria-invalid'?: boolean }>
}) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      {cloneElement(children, {
        id,
        'aria-describedby': error ? errorId : undefined,
        'aria-invalid': error ? true : undefined,
      })}
      {error && (
        <span id={errorId} className="block text-xs text-danger">
          {error}
        </span>
      )}
    </div>
  )
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={fieldClass} {...props} />
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={fieldClass} {...props} />
}

export function ErrorText({ error }: { error: unknown }) {
  if (!error) return null
  return <p className="text-sm text-danger">{error instanceof Error ? error.message : 'Algo deu errado'}</p>
}

export function Textarea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={fieldClass} rows={3} {...props} />
}

export function SectionTitle({ children, action }: { children: ReactNode; action?: ReactNode }) {
  return (
    <div className="mb-3 flex items-center justify-between gap-2">
      <h2 className="font-semibold">{children}</h2>
      {action}
    </div>
  )
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="text-sm text-muted">{children}</p>
}

export function IconButton({ label, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={`rounded-md p-1 text-muted transition hover:bg-canvas hover:text-ink ${className}`}
      {...props}
    />
  )
}

/** Mostra "carregando" / erro / conteúdo de uma query do TanStack. */
export function Loaded<T>({
  query,
  children,
}: {
  query: { isPending: boolean; isError: boolean; error: unknown; data: T | undefined }
  children: (data: T) => ReactNode
}) {
  if (query.isPending) return <p className="text-sm text-muted">Carregando…</p>
  if (query.isError) return <ErrorText error={query.error} />
  return <>{children(query.data as T)}</>
}
