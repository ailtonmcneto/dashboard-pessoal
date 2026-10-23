"use server";

import { revalidatePath } from "next/cache";
import { requireUser } from "@/lib/supabase/server";

export async function addTransaction(form: FormData) {
  const { supabase } = await requireUser();
  const amount = Number(String(form.get("amount")).replace(",", "."));
  if (!amount || amount <= 0) return;

  await supabase.from("transactions").insert({
    description: String(form.get("description")).trim(),
    amount,
    type: form.get("type") === "receita" ? "receita" : "despesa",
    category: String(form.get("category") || "Outros").trim(),
    date: String(form.get("date")),
  });
  revalidatePath("/", "layout");
}

export async function deleteTransaction(form: FormData) {
  const { supabase } = await requireUser();
  await supabase.from("transactions").delete().eq("id", String(form.get("id")));
  revalidatePath("/", "layout");
}
