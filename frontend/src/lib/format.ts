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

/** 'AAAA-MM-DD' (LocalDate da API) como data local, sem deslocar o dia pelo fuso. */
export function parseLocalDate(value: string): Date {
  const [y, m, d] = value.split('-').map(Number)
  return new Date(y, m - 1, d)
}

const shortDateFmt = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' })
const weekdayDateFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: '2-digit', month: 'short' })

export const formatDate = (value: string) => shortDateFmt.format(parseLocalDate(value))
export const formatWeekdayDate = (value: string) => weekdayDateFmt.format(parseLocalDate(value))

/** Hoje como 'AAAA-MM-DD' no fuso do navegador. */
export const todayIso = () => dayKey(new Date().toISOString())

/** 'HH:mm:ss' → 'HH:mm'. */
export const formatClock = (value: string) => value.slice(0, 5)

export const WEEKDAYS = ['Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado', 'Domingo']

export function formatMinutes(total: number): string {
  const h = Math.floor(total / 60)
  const m = total % 60
  if (h === 0) return `${m} min`
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}

const grade = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 2 })
export const formatGrade = (n: number) => grade.format(n)

const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
export const formatMoney = (n: number) => money.format(n)

/** Valor compacto para eixos e rótulos curtos: R$ 1,2 mil. */
const moneyCompact = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})
export const formatMoneyCompact = (n: number) => moneyCompact.format(n)

// ---------- meses ('AAAA-MM') ----------

export function currentMonth(): string {
  return todayIso().slice(0, 7)
}

export function addMonths(month: string, n: number): string {
  const [y, m] = month.split('-').map(Number)
  const d = new Date(y, m - 1 + n, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

const monthFmt = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
const monthShortFmt = new Intl.DateTimeFormat('pt-BR', { month: 'short' })

/** "Outubro de 2026" (só a primeira letra maiúscula). */
export const formatMonth = (month: string) => {
  const text = monthFmt.format(parseLocalDate(`${month}-01`))
  return text.charAt(0).toUpperCase() + text.slice(1)
}
export const formatMonthShort = (month: string) => monthShortFmt.format(parseLocalDate(`${month}-01`)).replace('.', '')
