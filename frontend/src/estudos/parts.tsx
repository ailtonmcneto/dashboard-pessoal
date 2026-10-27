import { Link } from 'react-router'
import type { Course } from '../api/types'
import { Meter } from '../components/charts'
import { StatusBadge } from '../components/StatusBadge'
import { formatGrade } from '../lib/format'
import { neededText } from './grades'
import { ABSENCE_STATUS, GRADE_STATUS } from './labels'

export function ColorDot({ color, size = 10 }: { color: string; size?: number }) {
  return (
    <span
      aria-hidden="true"
      className="inline-block shrink-0 rounded-full"
      style={{ width: size, height: size, background: color }}
    />
  )
}

export function AbsenceMeter({ course }: { course: Course }) {
  const a = course.absences
  const tone = a.status === 'ESTOURADO' ? 'critical' : a.status === 'ATENCAO' ? 'warning' : 'chart'
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs text-muted">
        <span>Faltas</span>
        <span className="tabular-nums">
          {a.used}
          {a.max != null && ` de ${a.max}`}
        </span>
      </div>
      {a.max != null && <Meter value={a.used} max={a.max} tone={tone} label={`Faltas: ${a.used} de ${a.max}`} />}
    </div>
  )
}

export function CourseCard({ course }: { course: Course }) {
  const grade = GRADE_STATUS[course.grades.status]
  const absence = ABSENCE_STATUS[course.absences.status]
  const needed = neededText(course)
  return (
    <Link
      to={`/estudos/disciplinas/${course.id}`}
      className="block rounded-xl border border-line bg-surface p-4 transition hover:border-accent/50 hover:shadow-sm"
      style={{ boxShadow: `inset 4px 0 0 ${course.color}` }}
    >
      <div className="mb-3 flex items-start justify-between gap-2 pl-1">
        <div className="min-w-0">
          <p className="truncate font-semibold">{course.name}</p>
          <p className="truncate text-xs text-muted">
            {[course.code, course.professor].filter(Boolean).join(' · ') || ' '}
          </p>
        </div>
        <p className="text-2xl font-bold tabular-nums">
          {course.grades.average != null ? formatGrade(course.grades.average) : '—'}
        </p>
      </div>
      <div className="space-y-3 pl-1">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge tone={grade.tone} label={grade.label} />
          {course.absences.status !== 'OK' && course.absences.status !== 'SEM_LIMITE' && (
            <StatusBadge tone={absence.tone} label={absence.label} />
          )}
        </div>
        {needed && <p className="text-xs text-muted">{needed}</p>}
        <AbsenceMeter course={course} />
      </div>
    </Link>
  )
}
