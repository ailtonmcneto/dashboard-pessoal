-- =============================================================
-- V5 — saúde e hábitos
--
--   * métricas flexíveis: cada usuário define o que mede (sono, peso,
--     água…), a unidade, como os registros de um dia viram um valor
--     (soma, média ou último) e, se quiser, uma meta diária mínima ou
--     máxima
--   * hábitos: diário, em dias da semana escolhidos ou X vezes por
--     semana; cada dia cumprido é uma linha em habit_checks. A sequência
--     (streak) é sempre calculada, nunca guardada
--   * consultas médicas usam a agenda (events.kind = 'CONSULTA')
-- =============================================================

create table metric_types (
    id           uuid primary key default uuidv7(),
    user_id      uuid not null references users on delete cascade,
    name         text not null check (length(name) between 1 and 40),
    unit         text not null check (length(unit) between 1 and 12),
    aggregation  text not null default 'SOMA' check (aggregation in ('SOMA', 'MEDIA', 'ULTIMO')),
    target       numeric(10,2) check (target > 0),
    target_kind  text check (target_kind in ('MINIMO', 'MAXIMO')),
    color        text not null default '#0d9488' check (color ~ '^#[0-9a-f]{6}$'),
    archived     boolean not null default false,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    unique (id, user_id),
    check ((target is null) = (target_kind is null))
);
create unique index metric_types_unique_name on metric_types (user_id, lower(name));

create table measurements (
    id             uuid primary key default uuidv7(),
    user_id        uuid not null,
    metric_type_id uuid not null,
    measured_at    timestamptz not null,
    value          numeric(10,2) not null check (value >= 0),
    note           text,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (id, user_id),
    foreign key (metric_type_id, user_id) references metric_types (id, user_id) on delete cascade
);
create index measurements_metric_time on measurements (metric_type_id, measured_at desc);

-- weekdays_mask: bit (dia ISO − 1), ou seja seg = 1, ter = 2, qua = 4 … dom = 64
create table habits (
    id             uuid primary key default uuidv7(),
    user_id        uuid not null references users on delete cascade,
    name           text not null check (length(name) between 1 and 60),
    schedule       text not null default 'DIARIO' check (schedule in ('DIARIO', 'DIAS_SEMANA', 'X_POR_SEMANA')),
    weekdays_mask  smallint check (weekdays_mask between 1 and 127),
    times_per_week smallint check (times_per_week between 1 and 7),
    starts_on      date not null,
    color          text not null default '#0d9488' check (color ~ '^#[0-9a-f]{6}$'),
    archived       boolean not null default false,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (id, user_id),
    check (schedule <> 'DIAS_SEMANA' or weekdays_mask is not null),
    check (schedule <> 'X_POR_SEMANA' or times_per_week is not null)
);
create unique index habits_unique_name on habits (user_id, lower(name));

create table habit_checks (
    id         uuid primary key default uuidv7(),
    user_id    uuid not null,
    habit_id   uuid not null,
    date       date not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (habit_id, date),
    foreign key (habit_id, user_id) references habits (id, user_id) on delete cascade
);
create index habit_checks_user_date on habit_checks (user_id, date desc);
