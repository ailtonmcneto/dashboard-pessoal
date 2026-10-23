-- =============================================================
-- V3 — acadêmico: faculdade (semestres, disciplinas, horário, faltas,
-- avaliações), autodidata (cursos/livros), assuntos com revisão e
-- sessões de estudo.
--
-- FKs entre tabelas de usuário são compostas (x_id, user_id): o banco
-- recusa, por exemplo, uma prova numa disciplina de outro usuário.
-- Em FKs opcionais, "on delete set null (x_id)" zera só a coluna do
-- vínculo e preserva o user_id.
-- =============================================================

-- ---------- Faculdade ----------
create table terms (
    id            uuid primary key default uuidv7(),
    user_id       uuid not null references users on delete cascade,
    name          text not null check (length(name) between 1 and 60),
    starts_on     date not null,
    ends_on       date not null,
    passing_grade numeric(4,2) not null default 6.0 check (passing_grade between 0 and 10),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique (id, user_id),
    unique (user_id, name),
    check (ends_on >= starts_on)
);

create table courses (
    id             uuid primary key default uuidv7(),
    user_id        uuid not null,
    term_id        uuid not null,
    name           text not null check (length(name) between 1 and 120),
    code           text,
    professor      text,
    workload_hours integer check (workload_hours > 0),
    max_absences   integer check (max_absences >= 0),
    color          text not null default '#0f766e' check (color ~ '^#[0-9a-fA-F]{6}$'),
    status         text not null default 'CURSANDO'
                   check (status in ('CURSANDO', 'APROVADO', 'REPROVADO', 'TRANCADO')),
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (id, user_id),
    unique (term_id, name),
    foreign key (term_id, user_id) references terms (id, user_id) on delete cascade
);
create index courses_user_term on courses (user_id, term_id);

-- Horário semanal: weekday ISO (1 = segunda … 7 = domingo)
create table class_slots (
    id         uuid primary key default uuidv7(),
    user_id    uuid not null,
    course_id  uuid not null,
    weekday    smallint not null check (weekday between 1 and 7),
    start_time time not null,
    end_time   time not null,
    room       text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (id, user_id),
    check (end_time > start_time),
    foreign key (course_id, user_id) references courses (id, user_id) on delete cascade
);
create index class_slots_user_weekday on class_slots (user_id, weekday, start_time);

create table absences (
    id             uuid primary key default uuidv7(),
    user_id        uuid not null,
    course_id      uuid not null,
    date           date not null,
    classes_missed smallint not null default 1 check (classes_missed between 1 and 20),
    note           text,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    unique (id, user_id),
    foreign key (course_id, user_id) references courses (id, user_id) on delete cascade
);
create index absences_course on absences (course_id, date);

-- Provas, trabalhos etc. Nota normalizada para 0–10 = score / max_score * 10.
create table assessments (
    id         uuid primary key default uuidv7(),
    user_id    uuid not null,
    course_id  uuid not null,
    title      text not null check (length(title) between 1 and 120),
    kind       text not null default 'PROVA'
               check (kind in ('PROVA', 'TRABALHO', 'SEMINARIO', 'LISTA', 'PROJETO', 'OUTRO')),
    due_on     date,
    weight     numeric(5,2) not null default 1 check (weight > 0),
    max_score  numeric(6,2) not null default 10 check (max_score > 0),
    score      numeric(6,2) check (score >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (id, user_id),
    check (score is null or score <= max_score),
    foreign key (course_id, user_id) references courses (id, user_id) on delete cascade
);
create index assessments_user_due on assessments (user_id, due_on);
create index assessments_course on assessments (course_id);

-- ---------- Autodidata ----------
create table resources (
    id          uuid primary key default uuidv7(),
    user_id     uuid not null references users on delete cascade,
    title       text not null check (length(title) between 1 and 200),
    kind        text not null default 'CURSO'
                check (kind in ('CURSO', 'LIVRO', 'VIDEO', 'ARTIGO', 'OUTRO')),
    provider    text,
    url         text,
    total_units integer check (total_units > 0),
    done_units  integer not null default 0 check (done_units >= 0),
    status      text not null default 'ATIVO'
                check (status in ('PLANEJADO', 'ATIVO', 'PAUSADO', 'CONCLUIDO')),
    started_on  date,
    finished_on date,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    unique (id, user_id),
    check (total_units is null or done_units <= total_units),
    check (finished_on is null or started_on is null or finished_on >= started_on)
);
create index resources_user_status on resources (user_id, status);

-- ---------- Assuntos (de uma disciplina, de um curso/livro, ou soltos) ----------
create table topics (
    id               uuid primary key default uuidv7(),
    user_id          uuid not null references users on delete cascade,
    course_id        uuid,
    resource_id      uuid,
    parent_id        uuid,
    title            text not null check (length(title) between 1 and 200),
    mastery          smallint not null default 0 check (mastery between 0 and 5),
    last_reviewed_on date,
    next_review_on   date,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    unique (id, user_id),
    check (num_nonnulls(course_id, resource_id) <= 1),
    check (parent_id is null or parent_id <> id),
    foreign key (course_id, user_id) references courses (id, user_id) on delete cascade,
    foreign key (resource_id, user_id) references resources (id, user_id) on delete cascade,
    foreign key (parent_id, user_id) references topics (id, user_id) on delete cascade
);
create index topics_user_review on topics (user_id, next_review_on);
create index topics_course on topics (course_id);
create index topics_resource on topics (resource_id);

-- O que cai em cada avaliação
create table assessment_topics (
    user_id       uuid not null,
    assessment_id uuid not null,
    topic_id      uuid not null,
    primary key (assessment_id, topic_id),
    foreign key (assessment_id, user_id) references assessments (id, user_id) on delete cascade,
    foreign key (topic_id, user_id) references topics (id, user_id) on delete cascade
);
create index assessment_topics_topic on assessment_topics (topic_id);

-- ---------- Sessões de estudo ----------
create table study_sessions (
    id          uuid primary key default uuidv7(),
    user_id     uuid not null references users on delete cascade,
    started_at  timestamptz not null,
    minutes     integer not null check (minutes between 1 and 1440),
    course_id   uuid,
    resource_id uuid,
    topic_id    uuid,
    notes       text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    unique (id, user_id),
    check (num_nonnulls(course_id, resource_id) <= 1),
    -- apagar a disciplina/curso/assunto mantém o histórico de horas estudadas
    foreign key (course_id, user_id) references courses (id, user_id) on delete set null (course_id),
    foreign key (resource_id, user_id) references resources (id, user_id) on delete set null (resource_id),
    foreign key (topic_id, user_id) references topics (id, user_id) on delete set null (topic_id)
);
create index study_sessions_user_started on study_sessions (user_id, started_at desc);
