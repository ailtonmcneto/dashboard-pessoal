import { useGet, useSend } from '../api/hooks'
import type { Goal, GoalRequest, GoalStatus, MilestoneRequest, Project, ProjectRequest, Task, TaskRequest, TaskView } from '../api/types'

/** Tarefa mexe em meta e projeto (progresso): recarrega os três. */
const ALL = ['goals', 'milestones', 'projects', 'tasks']

type WithId<T> = T & { id: string }

export type TaskFilter = { goalId?: string; projectId?: string; courseId?: string }

export const useGoals = () => useGet<Goal[]>('/goals')
export const useGoal = (id: string) => useGet<Goal>(`/goals/${id}`)
export const useProjects = () => useGet<Project[]>('/projects')
/** {@code enabled = false} quando a lista já veio de outro lugar (ex.: a tela Hoje). */
export const useTasks = (view: TaskView, filter: TaskFilter = {}, enabled = true) => {
  const qs = new URLSearchParams({ view })
  Object.entries(filter).forEach(([k, v]) => v && qs.set(k, v))
  return useGet<Task[]>(enabled ? `/tasks?${qs}` : null)
}

export const useCreateGoal = () => useSend<GoalRequest, Goal>('POST', () => '/goals', ALL)
export const useUpdateGoal = () => useSend<WithId<GoalRequest>, Goal>('PUT', (g) => `/goals/${g.id}`, ALL)
export const useGoalStatus = () =>
  useSend<{ id: string; status: GoalStatus }>('PUT', (g) => `/goals/${g.id}/status`, ALL)
export const useDeleteGoal = () => useSend<string>('DELETE', (id) => `/goals/${id}`, ALL)

export const useAddMilestone = (goalId: string) =>
  useSend<MilestoneRequest>('POST', () => `/goals/${goalId}/milestones`, ALL)
export const useToggleMilestone = () =>
  useSend<{ id: string; done: boolean }>('POST', (m) => `/milestones/${m.id}/${m.done ? 'done' : 'undone'}`, ALL)
export const useDeleteMilestone = () => useSend<string>('DELETE', (id) => `/milestones/${id}`, ALL)

export const useCreateProject = () => useSend<ProjectRequest>('POST', () => '/projects', ALL)
export const useUpdateProject = () => useSend<WithId<ProjectRequest>>('PUT', (p) => `/projects/${p.id}`, ALL)
export const useDeleteProject = () => useSend<string>('DELETE', (id) => `/projects/${id}`, ALL)

export const useCreateTask = () => useSend<TaskRequest>('POST', () => '/tasks', ALL)
export const useToggleTask = () =>
  useSend<{ id: string; done: boolean }>('POST', (t) => `/tasks/${t.id}/${t.done ? 'done' : 'undone'}`, ALL)
export const useDeleteTask = () => useSend<string>('DELETE', (id) => `/tasks/${id}`, ALL)
