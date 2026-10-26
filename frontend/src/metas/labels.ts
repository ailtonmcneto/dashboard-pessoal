import type { Goal, GoalArea, Pace, ProgressMode, ProjectKind, ProjectStatus } from '../api/types'
import type { Tone } from '../components/StatusBadge'
import { formatMoney } from '../lib/format'

export const AREA: Record<GoalArea, string> = {
  ESTUDOS: 'Estudos',
  FINANCAS: 'Finanças',
  SAUDE: 'Saúde',
  CARREIRA: 'Carreira',
  PESSOAL: 'Pessoal',
}

export const PROGRESS_MODE: Record<ProgressMode, { label: string; hint: string }> = {
  MARCOS: { label: 'Por marcos', hint: 'Divida a meta em etapas e marque cada uma.' },
  TAREFAS: { label: 'Por tarefas', hint: 'Avança conforme as tarefas ligadas à meta são feitas.' },
  MANUAL: { label: 'Valor manual', hint: 'Você atualiza o número (ex.: 3 de 12 livros).' },
  FINANCEIRO: { label: 'Saldo de uma conta', hint: 'Acompanha o saldo de uma conta até o valor-alvo.' },
}

export const PACE: Record<Pace, { label: string; tone: Tone }> = {
  SEM_PRAZO: { label: 'Sem prazo', tone: 'neutral' },
  ADIANTADA: { label: 'Adiantada', tone: 'good' },
  NO_RITMO: { label: 'No ritmo', tone: 'good' },
  ATRASADA: { label: 'Atrasada', tone: 'warning' },
  CONCLUIDA: { label: 'Concluída', tone: 'good' },
  ABANDONADA: { label: 'Abandonada', tone: 'neutral' },
}

export const PROJECT_KIND: Record<ProjectKind, string> = { ESTUDO: 'Estudo', PESSOAL: 'Pessoal', TRABALHO: 'Trabalho' }

export const PROJECT_STATUS: Record<ProjectStatus, string> = {
  PLANEJADO: 'Planejado',
  ATIVO: 'Ativo',
  PAUSADO: 'Pausado',
  CONCLUIDO: 'Concluído',
}

export const PRIORITY: Record<number, string> = { 1: 'Alta', 2: 'Média', 3: 'Baixa' }

/** "2 de 4 marcos", "3 de 12 livros", "R$ 2.500 de R$ 10.000". */
export function progressText(g: Goal): string {
  const num = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
  switch (g.progressMode) {
    case 'MARCOS':
      return g.target ? `${g.current} de ${g.target} marcos` : 'Sem marcos ainda'
    case 'TAREFAS':
      return g.target ? `${g.current} de ${g.target} tarefas` : 'Sem tarefas ligadas'
    case 'FINANCEIRO':
      return `${formatMoney(g.current)} de ${formatMoney(g.target ?? 0)}${g.accountName ? ` · ${g.accountName}` : ''}`
    default:
      return `${num.format(g.current)} de ${num.format(g.target ?? 0)}${g.unit ? ` ${g.unit}` : ''}`
  }
}
