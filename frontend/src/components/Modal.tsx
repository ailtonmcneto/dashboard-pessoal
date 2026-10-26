import { Pencil, X } from 'lucide-react'
import { type ReactNode, useEffect, useId, useRef, useState } from 'react'

/**
 * Janela modal com o <dialog> nativo: foco preso dentro, Esc fecha, o resto da página fica inerte.
 */
export function Modal({
  title,
  open,
  onClose,
  children,
}: {
  title: string
  open: boolean
  onClose: () => void
  children: ReactNode
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])
  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => e.target === ref.current && onClose()} // clique fora fecha
      aria-labelledby={titleId}
      className="m-auto w-[min(32rem,calc(100vw-2rem))] rounded-xl border border-line bg-surface p-0 text-ink shadow-xl backdrop:bg-ink/40"
    >
      {open && (
        <div className="p-5">
          <div className="mb-4 flex items-center justify-between gap-2">
            <h2 id={titleId} className="text-lg font-semibold">
              {title}
            </h2>
            <button
              type="button"
              onClick={onClose}
              aria-label="Fechar"
              className="rounded-md p-1 text-muted hover:bg-canvas hover:text-ink"
            >
              <X size={18} />
            </button>
          </div>
          {children}
        </div>
      )}
    </dialog>
  )
}

/**
 * Lápis que abre um modal de edição. {@code children} recebe {@code close} para o formulário
 * fechar a janela quando salvar.
 */
export function EditButton({
  label,
  title,
  children,
}: {
  label: string
  title: string
  children: (close: () => void) => ReactNode
}) {
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)
  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label={label}
        title={label}
        className="rounded-md p-1 text-muted transition hover:bg-canvas hover:text-ink"
      >
        <Pencil size={15} />
      </button>
      <Modal title={title} open={open} onClose={close}>
        {children(close)}
      </Modal>
    </>
  )
}
