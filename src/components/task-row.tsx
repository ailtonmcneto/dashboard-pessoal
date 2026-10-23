import { Check } from "lucide-react";
import { deleteTask, toggleTask } from "@/app/(app)/tarefas/actions";
import { fmtDate } from "@/lib/format";
import { DeleteButton, SubmitButton } from "./ui";

export type Task = { id: string; title: string; done: boolean; priority: number; due_date: string | null; goal_id: string | null };

const PRIORITY = { 1: "Alta", 2: "Média", 3: "Baixa" } as Record<number, string>;

export function TaskRow({ task, goalName, overdue, showDelete = true }: { task: Task; goalName?: string; overdue?: boolean; showDelete?: boolean }) {
  return (
    <li className="flex items-center gap-3 py-2">
      <form action={toggleTask}>
        <input type="hidden" name="id" value={task.id} />
        <input type="hidden" name="done" value={task.done ? "1" : "0"} />
        <SubmitButton
          className={`grid h-5 w-5 place-items-center rounded-full border-2 transition disabled:opacity-50 ${
            task.done ? "border-good bg-good text-surface" : "border-muted text-transparent hover:border-ink"
          }`}
        >
          <Check size={12} strokeWidth={3.5} />
          <span className="sr-only">{task.done ? "Reabrir" : "Concluir"} {task.title}</span>
        </SubmitButton>
      </form>
      <div className="min-w-0 flex-1">
        <p className={`truncate text-sm ${task.done ? "text-muted line-through" : "font-medium"}`}>{task.title}</p>
        {(task.due_date || goalName || task.priority === 1) && (
          <p className="flex flex-wrap gap-x-2 text-xs text-muted">
            {task.priority === 1 && !task.done && <span className="font-semibold text-out">Prioridade {PRIORITY[1].toLowerCase()}</span>}
            {task.due_date && <span className={overdue ? "font-semibold text-bad" : ""}>{overdue ? "Atrasada · " : ""}{fmtDate(task.due_date)}</span>}
            {goalName && <span>Meta: {goalName}</span>}
          </p>
        )}
      </div>
      {showDelete && <DeleteButton id={task.id} action={deleteTask} label={`Excluir ${task.title}`} />}
    </li>
  );
}
