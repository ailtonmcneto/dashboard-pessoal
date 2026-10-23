"use server";

import { revalidatePath } from "next/cache";
import { requireUser } from "@/lib/supabase/server";

const str = (v: FormDataEntryValue | null) => String(v ?? "").trim() || null;

export async function addTask(form: FormData) {
  const { supabase } = await requireUser();
  const title = str(form.get("title"));
  if (!title) return;
  await supabase.from("tasks").insert({
    title,
    due_date: str(form.get("due_date")),
    priority: Number(form.get("priority") || 2),
    goal_id: str(form.get("goal_id")),
  });
  revalidatePath("/", "layout");
}

export async function toggleTask(form: FormData) {
  const { supabase } = await requireUser();
  const done = form.get("done") !== "1";
  await supabase
    .from("tasks")
    .update({ done, done_at: done ? new Date().toISOString() : null })
    .eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}

export async function deleteTask(form: FormData) {
  const { supabase } = await requireUser();
  await supabase.from("tasks").delete().eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}

export async function addGoal(form: FormData) {
  const { supabase } = await requireUser();
  const title = str(form.get("title"));
  if (!title) return;
  await supabase.from("goals").insert({
    title,
    area: str(form.get("area")) ?? "Pessoal",
    target: Number(String(form.get("target") || 100).replace(",", ".")),
    unit: str(form.get("unit")) ?? "%",
    deadline: str(form.get("deadline")),
  });
  revalidatePath("/", "layout");
}

export async function updateGoal(form: FormData) {
  const { supabase } = await requireUser();
  const current = Number(String(form.get("current")).replace(",", "."));
  if (Number.isNaN(current) || current < 0) return;
  await supabase.from("goals").update({ current }).eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}

export async function deleteGoal(form: FormData) {
  const { supabase } = await requireUser();
  await supabase.from("goals").delete().eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}
