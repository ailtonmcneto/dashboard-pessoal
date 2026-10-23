"use server";

import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

function creds(form: FormData) {
  return {
    email: String(form.get("email") ?? "").trim(),
    password: String(form.get("password") ?? ""),
  };
}

export async function signIn(form: FormData) {
  const supabase = await createClient();
  const { error } = await supabase.auth.signInWithPassword(creds(form));
  if (error) redirect(`/login?erro=${encodeURIComponent("E-mail ou senha incorretos.")}`);
  redirect("/");
}

export async function signUp(form: FormData) {
  const supabase = await createClient();
  const { data, error } = await supabase.auth.signUp(creds(form));
  if (error) redirect(`/login?erro=${encodeURIComponent(error.message)}`);
  if (!data.session) {
    redirect(`/login?aviso=${encodeURIComponent("Conta criada. Confirme pelo link enviado ao seu e-mail e depois entre.")}`);
  }
  redirect("/");
}

export async function signOut() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/login");
}
