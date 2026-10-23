import { useGet, useSend } from '../api/hooks'
import type {
  Absence,
  AbsenceRequest,
  Assessment,
  AssessmentRequest,
  Course,
  CourseRequest,
  ResourceRequest,
  Slot,
  SlotRequest,
  StudyResource,
  StudySession,
  StudySessionRequest,
  StudySummary,
  Term,
  TermRequest,
  Topic,
  TopicRequest,
} from '../api/types'

/** Tudo do acadêmico se relaciona (nota mexe na disciplina, que aparece no semestre…): recarrega tudo. */
const ALL = ['terms', 'courses', 'assessments', 'schedule', 'topics', 'resources', 'study-sessions']

type WithId<T> = T & { id: string }

// ---------- leitura ----------

export const useTerms = () => useGet<Term[]>('/terms')
export const useTermCourses = (termId: string | null) =>
  useGet<Course[]>(termId ? `/terms/${termId}/courses` : null)
export const useCourse = (id: string) => useGet<Course>(`/courses/${id}`)
export const useCourseSlots = (id: string) => useGet<Slot[]>(`/courses/${id}/slots`)
export const useCourseAbsences = (id: string) => useGet<Absence[]>(`/courses/${id}/absences`)
export const useCourseAssessments = (id: string) => useGet<Assessment[]>(`/courses/${id}/assessments`)
export const useSchedule = () => useGet<Slot[]>('/schedule')
export const useUpcomingAssessments = (days: number) => useGet<Assessment[]>(`/assessments/upcoming?days=${days}`)
export const useResources = () => useGet<StudyResource[]>('/resources')
export const useTopics = (filter: { courseId?: string; due?: boolean } = {}) => {
  const qs = new URLSearchParams()
  if (filter.courseId) qs.set('courseId', filter.courseId)
  if (filter.due) qs.set('due', 'true')
  return useGet<Topic[]>(`/topics?${qs}`)
}
export const useStudySessions = (from: string, to: string) =>
  useGet<StudySession[]>(`/study-sessions?${new URLSearchParams({ from, to })}`)
export const useStudySummary = (from: string, to: string) =>
  useGet<StudySummary>(`/study-sessions/summary?${new URLSearchParams({ from, to })}`)

// ---------- escrita ----------

export const useCreateTerm = () => useSend<TermRequest, Term>('POST', () => '/terms', ALL)
export const useDeleteTerm = () => useSend<string>('DELETE', (id) => `/terms/${id}`, ALL)

export const useCreateCourse = () => useSend<CourseRequest, Course>('POST', () => '/courses', ALL)
export const useUpdateCourse = () =>
  useSend<WithId<CourseRequest>, Course>('PUT', (c) => `/courses/${c.id}`, ALL)
export const useDeleteCourse = () => useSend<string>('DELETE', (id) => `/courses/${id}`, ALL)

export const useAddSlot = (courseId: string) =>
  useSend<SlotRequest>('POST', () => `/courses/${courseId}/slots`, ALL)
export const useDeleteSlot = () => useSend<string>('DELETE', (id) => `/slots/${id}`, ALL)

export const useAddAbsence = (courseId: string) =>
  useSend<AbsenceRequest>('POST', () => `/courses/${courseId}/absences`, ALL)
export const useDeleteAbsence = () => useSend<string>('DELETE', (id) => `/absences/${id}`, ALL)

export const useAddAssessment = (courseId: string) =>
  useSend<AssessmentRequest>('POST', () => `/courses/${courseId}/assessments`, ALL)
export const useUpdateAssessment = () =>
  useSend<WithId<AssessmentRequest>>('PUT', (a) => `/assessments/${a.id}`, ALL)
export const useDeleteAssessment = () => useSend<string>('DELETE', (id) => `/assessments/${id}`, ALL)

export const useCreateResource = () => useSend<ResourceRequest>('POST', () => '/resources', ALL)
export const useUpdateResource = () =>
  useSend<WithId<ResourceRequest>>('PUT', (r) => `/resources/${r.id}`, ALL)
export const useDeleteResource = () => useSend<string>('DELETE', (id) => `/resources/${id}`, ALL)

export const useCreateTopic = () => useSend<TopicRequest>('POST', () => '/topics', ALL)
export const useReviewTopic = () =>
  useSend<{ id: string; mastery: number }>('POST', (r) => `/topics/${r.id}/review`, ALL)
export const useDeleteTopic = () => useSend<string>('DELETE', (id) => `/topics/${id}`, ALL)

export const useCreateSession = () => useSend<StudySessionRequest>('POST', () => '/study-sessions', ALL)
export const useDeleteSession = () => useSend<string>('DELETE', (id) => `/study-sessions/${id}`, ALL)
