-- =============================================================
-- Vida Dashboard — schema completo
-- Rode este arquivo inteiro no SQL Editor do Supabase.
-- Toda tabela tem user_id + Row Level Security: cada pessoa
-- só enxerga e altera os próprios dados.
-- =============================================================

-- ---------- Finanças ----------
create table if not exists public.transactions (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null default auth.uid() references auth.users on delete cascade,
  date        date not null default current_date,
  description text not null,
  amount      numeric(12,2) not null check (amount > 0),
  type        text not null check (type in ('receita','despesa')),
  category    text not null default 'Outros',
  created_at  timestamptz not null default now()
);
create index if not exists transactions_user_date on public.transactions (user_id, date desc);

-- ---------- Hábitos ----------
create table if not exists public.habits (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  name       text not null,
  archived   boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.habit_logs (
  id       uuid primary key default gen_random_uuid(),
  user_id  uuid not null default auth.uid() references auth.users on delete cascade,
  habit_id uuid not null references public.habits on delete cascade,
  date     date not null default current_date,
  unique (habit_id, date)
);
create index if not exists habit_logs_user_date on public.habit_logs (user_id, date desc);

-- ---------- Saúde (um registro por dia) ----------
create table if not exists public.health_logs (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null default auth.uid() references auth.users on delete cascade,
  date            date not null default current_date,
  sleep_hours     numeric(4,1) check (sleep_hours between 0 and 24),
  water_ml        integer check (water_ml >= 0),
  weight_kg       numeric(5,1) check (weight_kg > 0),
  workout_minutes integer check (workout_minutes >= 0),
  mood            smallint check (mood between 1 and 5),
  notes           text,
  unique (user_id, date)
);

-- ---------- Metas e tarefas ----------
create table if not exists public.goals (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  title      text not null,
  area       text not null default 'Pessoal',
  target     numeric(12,2) not null default 100 check (target > 0),
  current    numeric(12,2) not null default 0 check (current >= 0),
  unit       text not null default '%',
  deadline   date,
  created_at timestamptz not null default now()
);

create table if not exists public.tasks (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  title      text not null,
  done       boolean not null default false,
  priority   smallint not null default 2 check (priority between 1 and 3), -- 1 alta, 2 média, 3 baixa
  due_date   date,
  goal_id    uuid references public.goals on delete set null,
  created_at timestamptz not null default now(),
  done_at    timestamptz
);
create index if not exists tasks_user_done on public.tasks (user_id, done, due_date);

-- ---------- Agenda ----------
create table if not exists public.events (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  title      text not null,
  starts_at  timestamptz not null,
  ends_at    timestamptz,
  location   text,
  created_at timestamptz not null default now(),
  check (ends_at is null or ends_at >= starts_at)
);
create index if not exists events_user_start on public.events (user_id, starts_at);

-- ---------- Estudos ----------
create table if not exists public.study_items (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  title      text not null,
  kind       text not null default 'curso' check (kind in ('curso','livro','disciplina','outro')),
  progress   smallint not null default 0 check (progress between 0 and 100),
  status     text not null default 'ativo' check (status in ('ativo','pausado','concluido')),
  created_at timestamptz not null default now()
);

create table if not exists public.study_sessions (
  id       uuid primary key default gen_random_uuid(),
  user_id  uuid not null default auth.uid() references auth.users on delete cascade,
  item_id  uuid not null references public.study_items on delete cascade,
  date     date not null default current_date,
  minutes  integer not null check (minutes > 0)
);
create index if not exists study_sessions_user_date on public.study_sessions (user_id, date desc);

-- ---------- Row Level Security ----------
do $$
declare t text;
begin
  foreach t in array array[
    'transactions','habits','habit_logs','health_logs',
    'goals','tasks','events','study_items','study_sessions'
  ] loop
    execute format('alter table public.%I enable row level security', t);
    execute format('drop policy if exists "dono le e escreve" on public.%I', t);
    execute format(
      'create policy "dono le e escreve" on public.%I for all to authenticated
         using ((select auth.uid()) = user_id)
         with check ((select auth.uid()) = user_id)', t);
  end loop;
end $$;
