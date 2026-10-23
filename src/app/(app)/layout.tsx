import { LogOut } from "lucide-react";
import { BottomNav, SideNav } from "@/components/nav";
import { signOut } from "@/app/login/actions";
import { requireUser } from "@/lib/supabase/server";

export default async function AppLayout({ children }: LayoutProps<"/">) {
  const { email } = await requireUser();

  return (
    <div className="mx-auto flex max-w-7xl gap-8 px-4 md:px-6">
      <aside className="sticky top-0 hidden h-screen w-56 shrink-0 flex-col py-8 md:flex">
        <p className="mb-8 px-3 text-2xl font-bold tracking-tight">Vida</p>
        <SideNav />
        <div className="mt-auto space-y-2 px-3">
          <p className="truncate text-xs text-muted" title={email}>{email}</p>
          <form action={signOut}>
            <button className="flex items-center gap-2 text-sm text-muted hover:text-ink">
              <LogOut size={15} /> Sair
            </button>
          </form>
        </div>
      </aside>
      <main className="min-w-0 flex-1 pb-28 pt-6 md:pb-12 md:pt-8">
        <div className="mb-6 flex items-center justify-between md:hidden">
          <p className="text-xl font-bold tracking-tight">Vida</p>
          <form action={signOut}>
            <button className="flex items-center gap-1.5 text-sm text-muted" aria-label="Sair">
              <LogOut size={15} /> Sair
            </button>
          </form>
        </div>
        {children}
      </main>
      <BottomNav />
    </div>
  );
}
