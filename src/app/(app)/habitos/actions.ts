"use server";

import { revalidatePath } from "next/cache";
import { requireUser } from "@/lib/supabase/server";
import { today } from "@/lib/format";

export async function addHabit(form: FormData) {
  const { supabase } = await requireUser();
  const name = String(form.get("name") ?? "").trim();
  if (!name) return;
  await supabase.from("habits").insert({ name });
  revalidatePath("/", "layout");
}

export async function deleteHabit(form: FormData) {
  const { supabase } = await requireUser();
  await supabase.from("habits").delete().eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}

/** Marca ou desmarca um hábito num dia. */
export async function toggleHabit(form: FormData) {
  const { supabase } = await requireUser();
  const habit_id = String(form.get("habit_id"));
  const date = String(form.get("date") || today());

  if (form.get("done") === "1") {
    await supabase.from("habit_logs").delete().eq("habit_id", habit_id).eq("date", date);
  } else {
    await supabase.from("habit_logs").upsert({ habit_id, date }, { onConflict: "habit_id,date", ignoreDuplicates: true });
  }
  revalidatePath("/", "layout");
}

const num = (v: FormDataEntryValue | null) => {
  const s = String(v ?? "").replace(",", ".").trim();
  return s === "" ? null : Number(s);
};

export async function saveHealth(form: FormData) {
  const { supabase, userId } = await requireUser();
  await supabase.from("health_logs").upsert(
    {
      user_id: userId,
      date: String(form.get("date") || today()),
      sleep_hours: num(form.get("sleep_hours")),
      water_ml: num(form.get("water_ml")),
      weight_kg: num(form.get("weight_kg")),
      workout_minutes: num(form.get("workout_minutes")),
      mood: num(form.get("mood")),
      notes: String(form.get("notes") ?? "").trim() || null,
    },
    { onConflict: "user_id,date" },
  );
  revalidatePath("/", "layout");
}
