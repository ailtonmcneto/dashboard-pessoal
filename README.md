# Vida — dashboard de controle pessoal

Painel pessoal com cinco áreas: **Hoje** (resumo do dia), **Finanças**, **Hábitos e saúde**, **Tarefas e metas** e **Agenda e estudos**.

Stack: Next.js 16 (App Router, Server Actions) · Supabase (Postgres + Auth + RLS) · Tailwind CSS 4 · Recharts.

## Rodando pela primeira vez

1. **Crie um projeto no Supabase** em [supabase.com](https://supabase.com) (o plano grátis basta).
2. **Crie as tabelas**: no painel do Supabase, abra *SQL Editor*, cole o conteúdo de `supabase/schema.sql` e rode. O script pode ser rodado de novo sem quebrar nada.
3. **Configure as variáveis**:
   ```bash
   cp .env.example .env.local
   ```
   Preencha com a *Project URL* e a *Publishable key* (em *Project Settings → API Keys*).
4. **Instale e rode**:
   ```bash
   npm install
   npm run dev
   ```
   Abra http://localhost:3000, clique em **Criar conta** e entre.

> Por padrão o Supabase exige confirmação de e-mail. Para uso pessoal, você pode desligar em *Authentication → Sign In / Providers → Email → Confirm email*. Depois de criar sua conta, desligue também *Allow new users to sign up* para ninguém mais se cadastrar.

## Estrutura

```
supabase/schema.sql          tabelas, índices e políticas RLS
src/proxy.ts                 renova a sessão e protege as rotas (o antigo middleware)
src/lib/supabase/server.ts   cliente Supabase do servidor + requireUser()
src/lib/format.ts            datas no fuso de Brasília, moeda em R$
src/app/login/               login, cadastro e saída
src/app/(app)/page.tsx       Hoje: resumo do dia com o arco do sol
src/app/(app)/financas/      lançamentos, saldo, gráfico de 6 meses, categorias
src/app/(app)/habitos/       grade semanal de hábitos, sequências, registro de saúde, gráfico de sono
src/app/(app)/tarefas/       tarefas por prazo e metas com progresso
src/app/(app)/agenda/        compromissos dos próximos 30 dias e itens de estudo
src/components/              navegação, gráficos e peças reaproveitadas
```

Cada página é um Server Component que lê direto do Supabase; cada formulário chama uma Server Action no `actions.ts` da mesma pasta, que grava e chama `revalidatePath`. Não há API REST própria nem estado no cliente além dos gráficos.

## Segurança

Todas as tabelas têm Row Level Security com a política "dono lê e escreve": cada linha tem `user_id` preenchido automaticamente por `auth.uid()`, e o banco só devolve ou aceita linhas do usuário logado, mesmo que alguém chame a API diretamente.

## Deploy

Funciona na Vercel sem ajustes: importe o repositório e cadastre as duas variáveis do `.env.example`. Depois, em *Authentication → URL Configuration* no Supabase, adicione a URL de produção.

## Ideias para evoluir

- Orçamento mensal por categoria com alerta quando passar do limite
- Importar extrato do banco (CSV/OFX) para Finanças
- Sincronizar a Agenda com o Google Calendar
- Lançamentos recorrentes (aluguel, assinaturas)
- Heatmap anual de hábitos
- PWA para instalar no celular
