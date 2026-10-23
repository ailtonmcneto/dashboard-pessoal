"use server";

import { revalidatePath } from "next/cache";
import { requireUser } from "@/lib/supabase/server";
import { localToIso, today } from "@/lib/format";

const str = (v: FormDataEntryValue | null) => String(v ?? "").trim() || null;

export async function addEvent(form: FormData) {
  const { supabase } = await requireUser();
  const title = str(form.get("title"));
  const start = str(form.get("starts_at"));
  if (!title || !start) return;
  const endTime = str(form.get("end_time")); // HH:MM no mesmo dia
  await supabase.from("events").insert({
    title,
    starts_at: localToIso(start),
    ends_at: endTime ? localToIso(`${start.slice(0, 10)}T${endTime}`) : null,
    location: str(form.get("location")),
  });
  revalidatePath("/", "layout");
}

export async function deleteEvent(form: FormData) {
  const { supabase } = await requireUser();
  await supabase.from("events").delete().eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}

export async function addStudyItem(form: FormData) {
  const { supabase } = await requireUser();
  const title = str(form.get("title"));
  if (!title) return;
  await supabase.from("study_items").insert({ title, kind: str(form.get("kind")) ?? "curso" });
  revalidatePath("/", "layout");
}

export async function updateStudyItem(form: FormData) {
  const { supabase } = await requireUser();
  const progress = Math.max(0, Math.min(100, Math.round(Number(form.get("progress")) || 0)));
  await supabase
    .from("study_items")
    .update({ progress, status: progress === 100 ? "concluido" : str(form.get("status")) ?? "ativo" })
    .eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}

export async function deleteStudyItem(form: FormData) {
  const { supabase } = await requireUser();
  await supabase.from("study_items").delete().eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}

export async function logStudy(form: FormData) {
  const { supabase } = await requireUser();
  const minutes = Math.round(Number(form.get("minutes")));
  const item_id = str(form.get("item_id"));
  if (!item_id || !minutes || minutes <= 0) return;
  await supabase.from("study_sessions").insert({ item_id, minutes, date: str(form.get("date")) ?? today() });
  revalidatePath("/", "layout");
}
