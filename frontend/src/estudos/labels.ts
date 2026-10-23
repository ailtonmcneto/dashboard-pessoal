import type {
  AbsenceStatus,
  AssessmentKind,
  CourseStatus,
  GradeStatus,
  ResourceKind,
  ResourceStatus,
} from '../api/types'
import type { Tone } from '../components/StatusBadge'

export const GRADE_STATUS: Record<GradeStatus, { label: string; tone: Tone }> = {
  SEM_NOTAS: { label: 'Sem notas', tone: 'neutral' },
  EM_DIA: { label: 'Em dia', tone: 'good' },
  EM_RISCO: { label: 'Em risco', tone: 'warning' },
  APROVADO: { label: 'Aprovado por nota', tone: 'good' },
  REPROVADO: { label: 'Reprovado por nota', tone: 'critical' },
}

export const ABSENCE_STATUS: Record<AbsenceStatus, { label: string; tone: Tone }> = {
  SEM_LIMITE: { label: 'Sem limite definido', tone: 'neutral' },
  OK: { label: 'Faltas ok', tone: 'good' },
  ATENCAO: { label: 'Perto do limite', tone: 'warning' },
  ESTOURADO: { label: 'Limite estourado', tone: 'critical' },
}

export const COURSE_STATUS: Record<CourseStatus, string> = {
  CURSANDO: 'Cursando',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado',
  TRANCADO: 'Trancado',
}

export const ASSESSMENT_KIND: Record<AssessmentKind, string> = {
  PROVA: 'Prova',
  TRABALHO: 'Trabalho',
  SEMINARIO: 'Seminário',
  LISTA: 'Lista',
  PROJETO: 'Projeto',
  OUTRO: 'Outro',
}

export const RESOURCE_KIND: Record<ResourceKind, string> = {
  CURSO: 'Curso',
  LIVRO: 'Livro',
  VIDEO: 'Vídeo',
  ARTIGO: 'Artigo',
  OUTRO: 'Outro',
}

export const RESOURCE_STATUS: Record<ResourceStatus, string> = {
  PLANEJADO: 'Planejado',
  ATIVO: 'Em andamento',
  PAUSADO: 'Pausado',
  CONCLUIDO: 'Concluído',
}

export const MASTERY = ['Não sei', 'Muito fraco', 'Fraco', 'Razoável', 'Bom', 'Domino']

/** Cores para disciplinas (o usuário pode trocar). Só identificam; o nome sempre vem junto. */
export const COURSE_COLORS = ['#0f766e', '#2563eb', '#9333ea', '#db2777', '#ea580c', '#ca8a04', '#16a34a', '#475569']
