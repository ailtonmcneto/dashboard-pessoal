"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CalendarDays, CheckSquare, HeartPulse, Sun, Wallet } from "lucide-react";

const LINKS = [
  { href: "/", label: "Hoje", icon: Sun },
  { href: "/financas", label: "Finanças", icon: Wallet },
  { href: "/habitos", label: "Hábitos e saúde", short: "Hábitos", icon: HeartPulse },
  { href: "/tarefas", label: "Tarefas e metas", short: "Tarefas", icon: CheckSquare },
  { href: "/agenda", label: "Agenda e estudos", short: "Agenda", icon: CalendarDays },
];

export function SideNav() {
  const path = usePathname();
  return (
    <nav className="space-y-1">
      {LINKS.map(({ href, label, icon: Icon }) => {
        const active = href === "/" ? path === "/" : path.startsWith(href);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
              active ? "bg-surface text-ink shadow-[inset_3px_0_0_var(--sun)]" : "text-muted hover:text-ink"
            }`}
          >
            <Icon size={18} strokeWidth={1.75} />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}

export function BottomNav() {
  const path = usePathname();
  return (
    <nav className="fixed inset-x-0 bottom-0 z-10 grid grid-cols-5 border-t border-line bg-surface md:hidden">
      {LINKS.map(({ href, label, short, icon: Icon }) => {
        const active = href === "/" ? path === "/" : path.startsWith(href);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={`flex flex-col items-center gap-0.5 py-2 text-[11px] ${active ? "text-ink" : "text-muted"}`}
          >
            <Icon size={20} strokeWidth={active ? 2.25 : 1.75} />
            {short ?? label}
          </Link>
        );
      })}
    </nav>
  );
}
