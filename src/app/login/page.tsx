import { signIn, signUp } from "./actions";

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { erro, aviso } = await searchParams;

  return (
    <main className="grid min-h-screen place-items-center px-4">
      <div className="w-full max-w-sm">
        <SunMark />
        <h1 className="mt-6 text-4xl font-bold tracking-tight">Vida</h1>
        <p className="mt-2 text-muted">Seu dinheiro, seus hábitos, seu tempo. Tudo num lugar só.</p>

        <form className="panel mt-8 space-y-4">
          <label className="block space-y-1.5">
            <span className="text-sm font-medium">E-mail</span>
            <input name="email" type="email" required autoComplete="email" className="field" />
          </label>
          <label className="block space-y-1.5">
            <span className="text-sm font-medium">Senha</span>
            <input name="password" type="password" required minLength={6} autoComplete="current-password" className="field" />
          </label>

          {erro && <p role="alert" className="text-sm text-bad">{String(erro)}</p>}
          {aviso && <p className="text-sm text-good">{String(aviso)}</p>}

          <div className="flex gap-2 pt-1">
            <button formAction={signIn} className="btn flex-1">Entrar</button>
            <button formAction={signUp} className="flex-1 rounded-lg border border-line px-3.5 py-2 text-sm font-semibold hover:bg-bg">
              Criar conta
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}

function SunMark() {
  return (
    <svg width="56" height="32" viewBox="0 0 56 32" aria-hidden>
      <path d="M8 30a20 20 0 0 1 40 0" fill="var(--sun)" />
      <line x1="0" y1="30.5" x2="56" y2="30.5" stroke="var(--ink)" strokeWidth="2" />
    </svg>
  );
}
