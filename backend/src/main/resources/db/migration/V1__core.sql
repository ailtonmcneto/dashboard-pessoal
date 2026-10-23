-- =============================================================
-- V1 — núcleo: usuários e agenda
--
-- Convenções (valem para todas as migrações):
--   * PK uuid gerada com uuidv7() (ordenada no tempo, não adivinhável)
--   * toda tabela de dados tem user_id + unique (id, user_id), e as FKs
--     entre tabelas de usuário são compostas (x_id, user_id): o banco
--     recusa ligar dados de usuários diferentes
--   * enums como text + check, com os mesmos nomes do enum Java
--   * dias em date, instantes em timestamptz
-- =============================================================

create table users (
    id            uuid primary key default uuidv7(),
    email         text not null,
    name          text not null,
    password_hash text not null,
    timezone      text not null default 'America/Sao_Paulo',
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    constraint users_email_lowercase check (email = lower(email))
);
create unique index users_email_key on users (email);

create table events (
    id         uuid primary key default uuidv7(),
    user_id    uuid not null references users on delete cascade,
    title      text not null check (length(title) between 1 and 200),
    kind       text not null default 'COMPROMISSO'
               check (kind in ('COMPROMISSO', 'CONSULTA', 'OUTRO')),
    starts_at  timestamptz not null,
    ends_at    timestamptz,
    location   text,
    notes      text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (id, user_id),
    check (ends_at is null or ends_at >= starts_at)
);
create index events_user_starts on events (user_id, starts_at);
