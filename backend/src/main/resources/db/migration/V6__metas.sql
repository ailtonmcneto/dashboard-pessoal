-- =============================================================
-- V6 — metas, marcos, projetos e tarefas
--
--   * meta: o progresso é calculado conforme progress_mode — MANUAL
--     (current_value / target_value), MARCOS (marcos concluídos),
--     TAREFAS (tarefas ligadas concluídas) ou FINANCEIRO (saldo de uma
--     conta / target_value). "Plano" = meta com marcos (e conta, se
--     envolver dinheiro)
--   * tarefas formam uma lista única; cada uma pode apontar para meta,
--     marco, projeto e/ou disciplina. Apagar o "pai" só desfaz o vínculo
--     (on delete set null (coluna)), a tarefa continua existindo
-- =============================================================

create table goals (
    id            uuid primary key default uuidv7(),
    user_id       uuid not null references users on delete cascade,
    title         text not null check (length(title) between 1 and 120),
    description   text,
    area          text not null default 'PESSOAL'
                  check (area in ('ESTUDOS', 'FINANCAS', 'SAUDE', 'CARREIRA', 'PESSOAL')),
    status        text not null default 'ATIVA' check (status in ('ATIVA', 'CONCLUIDA', 'ABANDONADA')),
    starts_on     date,
    due_on        date,
    progress_mode text not null default 'MARCOS'
                  check (progress_mode in ('MANUAL', 'MARCOS', 'TAREFAS', 'FINANCEIRO')),
    target_value  numeric(14,2) check (target_value > 0),
    current_value numeric(14,2) check (current_value >= 0),
    unit          text,
    account_id    uuid,
    completed_on  date,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique (id, user_id),
    check (due_on is null or starts_on is null or due_on >= starts_on),
    check (progress_mode not in ('MANUAL', 'FINANCEIRO') or target_value is not null),
    -- conta exigida pelo service ao salvar; se a conta for apagada, a meta fica sem (progresso 0)
    foreign key (account_id, user_id) references accounts (id, user_id) on delete set null (account_id)
);
create index goals_user_status on goals (user_id, status);

create table milestones (
    id         uuid primary key default uuidv7(),
    user_id    uuid not null,
    goal_id    uuid not null,
    title      text not null check (length(title) between 1 and 120),
    due_on     date,
    done_on    date,
    position   integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (id, user_id),
    foreign key (goal_id, user_id) references goals (id, user_id) on delete cascade
);
create index milestones_goal on milestones (goal_id, position);

create table projects (
    id          uuid primary key default uuidv7(),
    user_id     uuid not null references users on delete cascade,
    title       text not null check (length(title) between 1 and 120),
    description text,
    kind        text not null default 'PESSOAL' check (kind in ('ESTUDO', 'PESSOAL', 'TRABALHO')),
    status      text not null default 'ATIVO' check (status in ('PLANEJADO', 'ATIVO', 'PAUSADO', 'CONCLUIDO')),
    starts_on   date,
    due_on      date,
    url         text,
    goal_id     uuid,
    course_id   uuid,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    unique (id, user_id),
    check (due_on is null or starts_on is null or due_on >= starts_on),
    foreign key (goal_id, user_id) references goals (id, user_id) on delete set null (goal_id),
    foreign key (course_id, user_id) references courses (id, user_id) on delete set null (course_id)
);
create index projects_user_status on projects (user_id, status);

-- priority: 1 alta, 2 média, 3 baixa
create table tasks (
    id           uuid primary key default uuidv7(),
    user_id      uuid not null references users on delete cascade,
    title        text not null check (length(title) between 1 and 200),
    notes        text,
    priority     smallint not null default 2 check (priority between 1 and 3),
    due_on       date,
    done_at      timestamptz,
    goal_id      uuid,
    milestone_id uuid,
    project_id   uuid,
    course_id    uuid,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    unique (id, user_id),
    foreign key (goal_id, user_id) references goals (id, user_id) on delete set null (goal_id),
    foreign key (milestone_id, user_id) references milestones (id, user_id) on delete set null (milestone_id),
    foreign key (project_id, user_id) references projects (id, user_id) on delete set null (project_id),
    foreign key (course_id, user_id) references courses (id, user_id) on delete set null (course_id)
);
create index tasks_user_open on tasks (user_id, due_on) where done_at is null;
create index tasks_goal on tasks (goal_id);
create index tasks_project on tasks (project_id);
create index tasks_course on tasks (course_id);
