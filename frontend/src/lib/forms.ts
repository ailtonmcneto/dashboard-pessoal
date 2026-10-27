/** Lê um <form> não controlado. Campos vazios viram null; números viram number. */
export function formData(form: HTMLFormElement) {
  const data = new FormData(form)
  const text = (name: string): string | null => {
    const v = data.get(name)
    return typeof v === 'string' && v.trim() !== '' ? v.trim() : null
  }
  return {
    text,
    required: (name: string) => text(name) ?? '',
    number: (name: string): number | null => {
      const v = text(name)
      return v === null ? null : Number(v.replace(',', '.'))
    },
  }
}
