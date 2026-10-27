import type { Course } from '../api/types'
import { formatGrade } from '../lib/format'

/** Frase curta sobre a nota necessária no que ainda não foi corrigido. */
export function neededText(course: Course): string | null {
  const g = course.grades
  if (g.neededOnPending == null) return null
  if (g.neededOnPending <= 0) return 'Já garantiu a média'
  if (g.neededOnPending > 10) return 'Não dá mais para atingir a média'
  return `Precisa de ${formatGrade(g.neededOnPending)} no que falta`
}
