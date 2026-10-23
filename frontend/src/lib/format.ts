const dayFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })
const timeFmt = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' })

export const formatDay = (iso: string) => dayFmt.format(new Date(iso))
export const formatTime = (iso: string) => timeFmt.format(new Date(iso))

/** Chave AAAA-MM-DD no fuso do navegador, para agrupar por dia. */
export function dayKey(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Valor de <input type="datetime-local"> (hora local) → ISO UTC. */
export const localInputToIso = (value: string) => new Date(value).toISOString()

export const browserTimezone = () => Intl.DateTimeFormat().resolvedOptions().timeZone
