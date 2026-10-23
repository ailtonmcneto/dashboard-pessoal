import { Check } from "lucide-react";
import { toggleHabit } from "@/app/(app)/habitos/actions";
import { SubmitButton } from "./ui";

/** Quadradinho clicável que marca/desmarca um hábito num dia. */
export function HabitCell({ habitId, date, done, label, size = "md" }: { habitId: string; date: string; done: boolean; label: string; size?: "md" | "lg" }) {
  const box = size === "lg" ? "h-9 w-9" : "h-8 w-8";
  return (
    <form action={toggleHabit}>
      <input type="hidden" name="habit_id" value={habitId} />
      <input type="hidden" name="date" value={date} />
      <input type="hidden" name="done" value={done ? "1" : "0"} />
      <SubmitButton
        className={`${box} grid place-items-center rounded-lg border transition disabled:opacity-50 ${
          done ? "border-transparent bg-sun text-sun-ink" : "border-line bg-bg text-transparent hover:border-muted"
        }`}
      >
        <Check size={16} strokeWidth={3} />
        <span className="sr-only">{done ? `Desmarcar ${label}` : `Marcar ${label}`}</span>
      </SubmitButton>
    </form>
  );
}
