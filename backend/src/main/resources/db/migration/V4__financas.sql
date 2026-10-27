-- =============================================================
-- V4 — finanças: contas, cartões, categorias, lançamentos (com
-- parcelas e transferências), recorrências e orçamento mensal.
--
-- Regras de modelagem:
--   * saldo nunca é guardado: sai da soma dos lançamentos
--   * compra no cartão é DESPESA na data da compra e cai numa fatura
--     (invoice_month = 1º dia do mês em que a fatura fecha)
--   * pagar a fatura é PAGAMENTO_FATURA: sai da conta, mas não conta
--     de novo como despesa
--   * TRANSFERENCIA move dinheiro entre contas e não é receita/despesa
-- =============================================================

create table accounts (
    id              uuid primary key default uuidv7(),
    user_id         uuid not null references users on delete cascade,
    name            text not null check (length(name) between 1 and 60),
    kind            text not null default 'CORRENTE'
                    check (kind in ('CORRENTE', 'POUPANCA', 'CARTEIRA', 'INVESTIMENTO')),
    opening_balance numeric(14,2) not null default 0,
    archived        boolean not null default false,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    unique (id, user_id),
    unique (user_id, name)
);

create table credit_cards (
    id                 uuid primary key default uuidv7(),
    user_id            uuid not null references users on delete cascade,
    name               text not null check (length(name) between 1 and 60),
    credit_limit       numeric(14,2) check (credit_limit > 0),
    closing_day        smallint not null check (closing_day between 1 and 28),
    due_day            smallint not null check (due_day between 1 and 28),
    payment_account_id uuid,
    archived           boolean not null default false,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    unique (id, user_id),
    unique (user_id, name),
    foreign key (payment_account_id, user_id) references accounts (id, user_id)
        on delete set null (payment_account_id)
);

-- Um nível de subcategoria (ex.: Moradia > Aluguel)
create table categories (
    id         uuid primary key default uuidv7(),
    user_id    uuid not null references users on delete cascade,
    name       text not null check (length(name) between 1 and 60),
    kind       text not null check (kind in ('RECEITA', 'DESPESA')),
    parent_id  uuid,
    color      text not null default '#78716c' check (color ~ '^#[0-9a-f]{6}$'),
    archived   boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (id, user_id),
    check (parent_id is null or parent_id <> id),
    foreign key (parent_id, user_id) references categories (id, user_id) on delete cascade
);
-- nome único entre irmãs (mesmo pai, ou ambas na raiz)
create unique index categories_unique_child_name on categories (parent_id, lower(name))
    where parent_id is not null;
create unique index categories_unique_root_name on categories (user_id, kind, lower(name))
    where parent_id is null;

create table recurrences (
    id            uuid primary key default uuidv7(),
    user_id       uuid not null references users on delete cascade,
    description   text not null check (length(description) between 1 and 120),
    amount        numeric(14,2) not null check (amount > 0),
    kind          text not null check (kind in ('RECEITA', 'DESPESA')),
    category_id   uuid,
    account_id    uuid,
    card_id       uuid,
    frequency     text not null default 'MENSAL' check (frequency in ('SEMANAL', 'MENSAL', 'ANUAL')),
    starts_on     date not null,
    ends_on       date,
    -- quantas ocorrências já viraram lançamento; a próxima é starts_on + n períodos
    generated     integer not null default 0 check (generated >= 0),
    next_on       date not null,
    active        boolean not null default true,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique (id, user_id),
    check (ends_on is null or ends_on >= starts_on),
    check (num_nonnulls(account_id, card_id) = 1),
    check (kind = 'DESPESA' or card_id is null),
    foreign key (category_id, user_id) references categories (id, user_id) on delete set null (category_id),
    foreign key (account_id, user_id) references accounts (id, user_id) on delete cascade,
    foreign key (card_id, user_id) references credit_cards (id, user_id) on delete cascade
);
create index recurrences_due on recurrences (next_on) where active;

create table transactions (
    id                   uuid primary key default uuidv7(),
    user_id              uuid not null references users on delete cascade,
    date                 date not null,
    description          text not null check (length(description) between 1 and 120),
    amount               numeric(14,2) not null check (amount > 0),
    kind                 text not null
                         check (kind in ('RECEITA', 'DESPESA', 'TRANSFERENCIA', 'PAGAMENTO_FATURA')),
    category_id          uuid,
    account_id           uuid,
    card_id              uuid,
    to_account_id        uuid,
    invoice_month        date check (invoice_month is null or extract(day from invoice_month) = 1),
    paid                 boolean not null default true,
    installment_group_id uuid,
    installment_no       smallint,
    installment_total    smallint,
    recurrence_id        uuid,
    notes                text,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now(),
    unique (id, user_id),
    -- forma de cada tipo
    check (kind <> 'RECEITA' or (account_id is not null and card_id is null and to_account_id is null)),
    check (kind <> 'DESPESA' or (num_nonnulls(account_id, card_id) = 1 and to_account_id is null
                                 and (card_id is null or invoice_month is not null))),
    check (kind <> 'TRANSFERENCIA' or (account_id is not null and to_account_id is not null
                                       and account_id <> to_account_id and card_id is null
                                       and category_id is null)),
    check (kind <> 'PAGAMENTO_FATURA' or (account_id is not null and card_id is not null
                                          and invoice_month is not null and category_id is null
                                          and to_account_id is null)),
    check ((installment_group_id is null) = (installment_no is null)
           and (installment_no is null) = (installment_total is null)),
    check (installment_no is null or installment_no between 1 and installment_total),
    foreign key (category_id, user_id) references categories (id, user_id) on delete set null (category_id),
    foreign key (account_id, user_id) references accounts (id, user_id),
    foreign key (to_account_id, user_id) references accounts (id, user_id),
    foreign key (card_id, user_id) references credit_cards (id, user_id),
    foreign key (recurrence_id, user_id) references recurrences (id, user_id) on delete set null (recurrence_id)
);
create index transactions_user_date on transactions (user_id, date desc);
create index transactions_account on transactions (account_id);
create index transactions_to_account on transactions (to_account_id);
create index transactions_card_invoice on transactions (card_id, invoice_month);
create index transactions_category on transactions (category_id);
create index transactions_installments on transactions (installment_group_id);

create table budgets (
    id           uuid primary key default uuidv7(),
    user_id      uuid not null,
    category_id  uuid not null,
    month        date not null check (extract(day from month) = 1),
    limit_amount numeric(14,2) not null check (limit_amount > 0),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    unique (id, user_id),
    unique (category_id, month),
    foreign key (category_id, user_id) references categories (id, user_id) on delete cascade
);
create index budgets_user_month on budgets (user_id, month);
