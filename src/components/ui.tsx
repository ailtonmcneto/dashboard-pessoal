"use client";

import { useFormStatus } from "react-dom";
import { Trash2 } from "lucide-react";
import type { ReactNode } from "react";

export function SubmitButton({ children, className = "btn" }: { children: ReactNode; className?: string }) {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending} className={className}>
      {children}
    </button>
  );
}

/** Botão de excluir: um form mínimo com o id escondido. */
export function DeleteButton({ id, action, label }: { id: string; action: (f: FormData) => Promise<void>; label: string }) {
  return (
    <form action={action}>
      <input type="hidden" name="id" value={id} />
      <DeleteInner label={label} />
    </form>
  );
}

function DeleteInner({ label }: { label: string }) {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending} className="btn-ghost" aria-label={label} title={label}>
      <Trash2 size={15} />
    </button>
  );
}
