# Contexto do projeto — Vida (dashboard pessoal)

Documento de passagem de contexto: o que foi decidido, o que já existe, como o código está organizado e o que falta. Atualizado ao fim da **fase 5**.

---

## 1. Objetivo

Dashboard para organizar a vida pessoal do autor: **estudos** (faculdade, cursos/livros, projetos), **finanças**, **saúde e hábitos** e **metas e planos**, com uma tela **Hoje** que reúne o que importa no dia.

- **Usuário:** só o autor por enquanto, mas modelado para vários usuários (`user_id` em tudo).
- **Onde roda:** local primeiro (Docker Compose). Deploy fica para depois.
- **Idioma:** interface, mensagens de erro, comentários e commits em português.
- **Forma de trabalho:** o autor aprova cada fase antes de começar. Cada fase termina com testes passando, teste no navegador, commit e push.
- **Repositório:** `ailtonmcneto/dashboard-pessoal`, branch de trabalho `claude/blissful-heisenberg-ucc55d`. Nenhum PR aberto.

---

## 2. Decisões tomadas (e por quê)

| Tema | Decisão | Motivo |
|---|---|---|
| Recomeço | Protótipo anterior (Next.js + Supabase) foi apagado | Pedido do autor: projeto novo em Java/Spring + JS |
| Backend | Java 25 + Spring Boot 4.1 (Maven) | LTS atual; Maven é o padrão mais documentado |
| Banco | PostgreSQL 18 | `numeric` para dinheiro, CHECKs fortes, `uuidv7()` nativo |
| IDs | UUID v7 (`default uuidv7()` no banco, `@UuidGenerator(VERSION_7)` no JPA) | Não adivinhável e ordenado no tempo (índice não fragmenta como o v4) |
| Multiusuário | `user_id` em toda tabela + FKs compostas `(x_id, user_id)` | O próprio banco recusa ligar dados de usuários diferentes |
| Autenticação | Sessão no servidor + cookie `httpOnly` + CSRF (padrão SPA); **sem JWT** | Front e API no mesmo domínio; mais simples e seguro que token no localStorage |
| Sessões | Spring Session JDBC (no próprio Postgres) | Reiniciar a API não desloga; pronto para mais de uma instância |
| Frontend | React + Vite + **TypeScript** (o autor perguntou por que não Angular/JS e aceitou TS) | Tipos gerados do OpenAPI da API |
| Remédios | Ficaram **fora da v1** | Decisão do autor |
| Gráficos | HTML/CSS próprio, seguindo a skill de dataviz | Cores validadas, acessível, sem dependência extra |

---

## 3. Stack e versões

- **API:** Java 25, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation, Security 7, Session JDBC, Actuator), Flyway, springdoc-openapi 3.1, Hibernate 7.
- **Banco:** PostgreSQL 18 (`postgres:18-alpine`).
- **Web:** React 19, Vite 8, TypeScript 6, React Router 8, TanStack Query 5, Tailwind CSS 4, lucide-react, oxlint.
- **Testes:** JUnit 5, Spring Boot Test/MockMvc, Testcontainers 2 (Postgres real), Playwright (só para testes manuais no sandbox, fora do repositório).

---

## 4. Estrutura do repositório

```
backend/
  pom.xml, mvnw, Dockerfile
  src/main/java/com/vida/
    common/      BaseEntity, OwnedEntity, CurrentUser, ApiExceptionHandler, BusinessException,
                 NotFoundException, Strings, RecordNullabilityConverter, SchedulingConfig
    auth/        User, AuthUser, SecurityConfig, AuthService, AuthController
    agenda/      Event (compromissos/consultas)
    academico/   Term, Course, ClassSlot, Absence, Assessment, Resource, Topic, StudySession,
                 Grades (média), Reviews (revisão espaçada), services e controllers
    financas/    Account, CreditCard, Category, Transaction, Recurrence, Budget, Schedules
                 (fatura/parcelas/ocorrências), FinanceQueries (SQL de agregação), RecurrenceJob,
                 services e FinanceController
    saude/       MetricType, Measurement, Habit, HabitCheck, DailyValues (valor do dia),
                 HabitStats (sequência/semana/taxa), MetricService, HabitService, HealthController
    metas/       Goal, Milestone, Project, Task, GoalProgress (percentual e ritmo),
                 GoalService, ProjectService, TaskService, GoalController
  src/main/resources/
    application.yml
    db/migration/  V1__core.sql, V2__spring_session.sql, V3__academico.sql, V4__financas.sql, V5__saude.sql, V6__metas.sql
  src/test/      IntegrationTest (base), TestcontainersConfig, testes por módulo
                 resources/application.properties (vida.jobs.enabled=false)
frontend/
  src/api/       client.ts (fetch + CSRF), hooks.ts (useGet/useSend), schema.d.ts (gerado), types.ts
  src/auth/      hooks de login/cadastro/sessão
  src/components AppLayout (navegação), ui.tsx, charts.tsx, StatusBadge
  src/agenda/    lista de eventos
  src/estudos/   páginas e hooks do acadêmico
  src/financas/  páginas e hooks de finanças
  src/saude/     hábitos (com calendário) e métricas (com gráfico)
  src/metas/     tarefas (TaskList reutilizável), metas com marcos e projetos
  src/pages/     Hoje, Agenda, Login
  src/router.tsx
docker-compose.yml   Postgres 18 (+ API no profile "api")
README.md            como rodar, regras de negócio, roadmap
contexto.md          este arquivo
```

---

## 5. Convenções de código

### Backend
- **Pacote por módulo.** Cada módulo tem entidades, repositórios (um arquivo cada), services, controller(s) e um arquivo `XxxDtos` com todos os records de request/response.
- **Posse dos dados:**
  - Entidades de usuário estendem `OwnedEntity` (que guarda `userId`).
  - Repositórios estendem `Repository` (não `JpaRepository`) e só expõem buscas com dono: `findByIdAndUserId`, nunca `findById` puro.
  - O usuário vem sempre de `CurrentUser` (`id()`, `today()`, `zone()`), nunca do request.
  - Recurso de outro usuário responde **404**.
- **FKs entre entidades:** guardadas como `UUID` simples (sem `@ManyToOne`). O service valida a posse e o banco garante pela FK composta.
- **Enums:** `text` + `CHECK` no banco, com os mesmos nomes do enum Java em MAIÚSCULAS (`@Enumerated(STRING)`).
- **Tipos:** dinheiro em `numeric(14,2)` ↔ `BigDecimal`; dias em `date` ↔ `LocalDate`; instantes em `timestamptz` ↔ `Instant`; mês na API como `AAAA-MM` (`YearMonth`).
- **Valores derivados** (saldo, média, progresso, gasto do orçamento) são **calculados, nunca armazenados**.
- **Erros:**
  - Todo erro sai como `application/problem+json`.
  - Erros de validação trazem `errors: {campo: mensagem}`.
  - Regra de negócio violada usa `BusinessException` (400/403/409).
  - Violação de constraint que escapar das validações vira 409.
- **Contrato OpenAPI:** campo de record sem `@Nullable` (JSpecify) é obrigatório; com `@Nullable`, é opcional e aceita `null`. Quem aplica isso é o `RecordNullabilityConverter`, e é o que mantém os tipos do front corretos.
- **Migrações:** uma por fase, SQL puro, comentadas. Sempre `unique (id, user_id)` e FKs compostas. Em FK opcional, `on delete set null (coluna)` zera só o vínculo.
- **Entre módulos:** um módulo não acessa repositórios de outro. Usa métodos públicos dos services, como `AccountService.requireOwned`/`paidBalances` e `CourseService.requireOwned`/`names`.
- **Jobs:** `@Scheduled`, ligados por `vida.jobs.enabled` (desligado nos testes).

### Frontend
- **Tipos:** gerados do OpenAPI (`npm run api:types`, com a API rodando) e reexportados com nomes amigáveis em `api/types.ts`.
- **Leitura e escrita:**
  - `useGet(path)` usa o próprio caminho como chave de cache: `['api', ...segmentos, query]`.
  - `useSend(method, path, prefixos)` invalida por prefixo. Cada módulo recarrega o módulo inteiro ao gravar, porque tudo nele se relaciona.
- **Formulários:** não controlados, lidos com `formData(form)` (`lib/forms.ts`).
- **Campos:** `Field` liga rótulo e campo por `id` (não envolve o campo) e associa o erro com `aria-describedby`.
- **Datas:** `parseLocalDate` evita deslocar o dia pelo fuso; `todayIso()`; meses em `AAAA-MM`.
- **Tema:** tokens em `index.css`:
  - interface: `--color-accent: #0f766e`;
  - dado de série única: `--color-chart: #0d9488`;
  - receitas × despesas: `--color-income: #2a78d6` e `--color-expense: #eb6834`;
  - status (sempre com ícone + rótulo): `good`, `warning` e `critical`.
- **Gráficos** (`components/charts.tsx`): `ColumnChart` (com linha de referência opcional), `BarList`, `GroupedColumns` (com legenda), `LineChart` (liga os dias com registro, ponto em cada registro, tooltip com cruzamento) e `Meter`. Todos têm tooltip e uma tabela `sr-only` para leitor de tela.
- **Tema escuro:** só o tema claro por enquanto.

---

## 6. O que já está pronto

### Fase 0 — esqueleto
Repositório limpo, `backend/`, `frontend/` e `docker-compose.yml`.

### Fase 1 — núcleo, login e agenda
- **Login:**
  - O primeiro cadastro é aceito e os seguintes são bloqueados (`vida.auth.signup-open` reabre).
  - O e-mail é normalizado para minúsculas.
  - O fuso do usuário é gravado no cadastro.
- **Endpoints de auth:** `/auth/csrf`, `/auth/status`, `/auth/register`, `/auth/login`, `/auth/logout`, `/auth/me`.
- **Cookies:**
  - `XSRF-TOKEN` fica no path `/`, para o JS das páginas conseguir ler.
  - A sessão usa o cookie `SESSION`: 14 dias, `HttpOnly`, `SameSite=Lax`.
- **Agenda:** CRUD de `/events` (compromisso, consulta, outro), por padrão os próximos 30 dias.
- **Front:**
  - login e cadastro;
  - layout com menu lateral e barra inferior no celular;
  - tela Hoje;
  - Agenda.

### Fase 1b — sessões no banco
Spring Session JDBC com a migração V2. Reiniciar a API não desloga.

### Fase 2 — Acadêmico (V3)
- **Tabelas:** `terms`, `courses`, `class_slots`, `absences`, `assessments` (+ `assessment_topics`), `resources`, `topics`, `study_sessions`.
- **Regras:**
  - **Média:** ponderada, com cada nota normalizada para 0–10.
  - **Nota necessária:** quanto falta tirar no que ainda não foi corrigido.
  - **Situação:** SEM_NOTAS, EM_DIA, EM_RISCO, APROVADO ou REPROVADO.
  - **Faltas:** alerta a partir de 75% do limite.
  - **Revisão espaçada:** domínio 0–5 define a próxima revisão em 1, 2, 4, 7, 15 ou 30 dias.
  - **Sessões de estudo:** apagar a disciplina mantém as horas, que passam a contar como "Sem vínculo".
- **Endpoints:**
  - **Semestres e disciplinas:** `/terms`, `/terms/{id}/courses`, `/courses`.
  - **Horário, faltas e avaliações:** `/courses/{id}/{slots|absences|assessments}`, `/slots/{id}`, `/absences/{id}`, `/assessments/{id}`, `/assessments/upcoming?days=`, `/schedule`.
  - **Cursos, assuntos e sessões:** `/resources`, `/topics?courseId|resourceId|due`, `/topics/{id}/review`, `/study-sessions`, `/study-sessions/summary`.
- **Telas:**
  - Estudos com abas: Semestre, Horário, Revisões, Cursos e livros, Sessões.
  - Página da disciplina, com a nota editável na própria linha.
  - Tela Hoje com aulas do dia, provas dos próximos 14 dias e revisões.

### Fase 3 — Finanças (V4)
- **Tabelas:** `accounts`, `credit_cards`, `categories` (um nível de subcategoria), `transactions`, `recurrences`, `budgets`.
- **Regras:**
  - **Saldo:** "pago" conta só o que já foi pago ou recebido; "previsto" inclui os pendentes.
  - **Transferência:** não é receita nem despesa.
  - **Cartão:** a compra é DESPESA na data e cai na fatura pelo dia de fechamento. `PAGAMENTO_FATURA` sai da conta sem contar como despesa de novo.
  - **Parcelado:** vira um lançamento por mês, com os centavos que sobram na 1ª parcela.
  - **Recorrências:**
    - São geradas ao criar (inclusive datas passadas) e depois de hora em hora, no fuso de cada usuário.
    - Editar a data ou a frequência recomeça depois de hoje, sem duplicar.
    - As que caem em conta nascem pendentes.
    - O dia 31 é preservado nos meses que têm 31 dias.
  - **Orçamento:** mensal por categoria, incluindo as subcategorias, com alerta a partir de 80% e cópia de um mês para outro.
  - **Conta ou cartão com histórico:** não pode ser apagado, só arquivado.
- **Endpoints:**
  - **Contas e cartões:** `/accounts`, `/cards`, `/cards/{id}/invoices/{AAAA-MM}` (+ `/pay`).
  - **Categorias:** `/categories` (+ `/defaults`).
  - **Lançamentos:** `/transactions?month|from&to&accountId&cardId&categoryId` (+ `?allInstallments=true` no DELETE).
  - **Recorrências:** `/recurrences` (+ `/pause`, `/resume`).
  - **Orçamento:** `/budgets` (PUT faz upsert; `/budgets/copy`).
  - **Resumos:** `/finance/summary?month=`, `/finance/history?until=&months=`.
- **Telas:**
  - Finanças com abas e um mês compartilhado entre elas: Visão geral, Lançamentos, Contas e cartões (com fatura e pagamento), Orçamento, Recorrências, Categorias.
  - "Primeiros passos" para quem está começando.
  - Cartão "Dinheiro" na tela Hoje.

### Fase 4 — Saúde e hábitos (V5)
- **Tabelas:** `metric_types`, `measurements`, `habits`, `habit_checks`.
- **Regras:**
  - **Valor do dia** (no fuso do usuário): soma, média ou último registro.
  - **Meta:** mínima ou máxima. Status do dia: SEM_DADOS, SEM_META, ATINGIDA ou NAO_ATINGIDA. Também a média dos últimos 7 dias.
  - **Hábitos:** DIARIO, DIAS_SEMANA (`weekdays_mask`, bit dia ISO − 1) ou X_POR_SEMANA.
  - **Sequência:** hoje em aberto não quebra; dias não devidos são pulados; no semanal conta semanas. Também recorde, progresso da semana, taxa de 30 dias e os últimos 7 dias.
  - **Marcar o dia:** idempotente. Não aceita dia futuro nem dia antes do início.
- **Endpoints:**
  - **Métricas:** `/metrics` (+ `/defaults`: sono, água, treino, peso, humor), `/metrics/{id}/series?from&to`, `/metrics/{id}/measurements` (GET/POST), `/measurements/{id}`.
  - **Hábitos:** `/habits`, `PUT|DELETE /habits/{id}/checks/{data}`, `/habits/{id}/history?from&to`.
- **Telas:**
  - Saúde com abas Hábitos (últimos 7 dias clicáveis, sequência, calendário de 12 semanas) e Métricas (cards com valor de hoje, meta, registro rápido).
  - Detalhe da métrica com gráfico de 7, 30 ou 90 dias: colunas para soma, linha para média ou último.
  - Tela Hoje com "Hábitos de hoje" (marcar ali mesmo) e "Saúde hoje" (métricas com meta e registro rápido).

### Fase 5 — Metas e planos (V6)
- **Tabelas:** `goals`, `milestones`, `projects`, `tasks`. Os vínculos de tarefa e projeto usam `on delete set null (coluna)`.
- **Regras:**
  - **Progresso** por `progress_mode`: MANUAL, MARCOS, TAREFAS ou FINANCEIRO (saldo pago da conta ÷ alvo), limitado a 0–100.
  - **Esperado:** dias decorridos ÷ dias entre início e prazo. O início vale hoje se não for informado.
  - **Ritmo:** ADIANTADA (≥ esperado + 10), NO_RITMO, ATRASADA (< esperado − 10, ou prazo vencido). Também CONCLUIDA, ABANDONADA e SEM_PRAZO.
  - **Status:** concluir guarda `completed_on`; reabrir limpa.
  - **Tarefas:** tarefa de um marco recebe a meta do marco; marco de outra meta dá 400. As visões são HOJE, SEMANA, ABERTAS e FEITAS (30 dias). Ordem: prazo (vazio no fim), depois prioridade.
- **Endpoints:**
  - **Metas e marcos:** `/goals`, `/goals/{id}`, `/goals/{id}/status`, `/goals/{id}/milestones`, `/milestones/{id}` (+ `/done`, `/undone`).
  - **Projetos:** `/projects`.
  - **Tarefas:** `/tasks?view&goalId&projectId&courseId`, `/tasks/{id}/done|undone`.
- **Telas:**
  - Metas e planos com abas Tarefas (visões), Metas (cards com barra, marca do esperado e ritmo) e Projetos (status, progresso, tarefas expansíveis).
  - Detalhe da meta: concluir, abandonar ou reabrir; atualizar o valor manual; marcos; tarefas.
  - Tarefas também na página da disciplina e no cartão "Tarefas de hoje" da tela Hoje.
  - As caixas de concluir tarefa e marco são otimistas: marcam na hora e voltam se a API falhar.

### Testes (62 no backend, todos passando)

| Classe | Testes | O que cobre |
|---|---|---|
| `AuthApiTest` | 8 | login, cadastro, sessão |
| `SpaAuthFlowTest` | 1 | fluxo HTTP real, como o navegador |
| `EventApiTest` | 4 | agenda |
| `GradesTest` | 6 | cálculo de média, faltas e revisão |
| `AcademicApiTest` | 9 | API do acadêmico |
| `SchedulesTest` | 4 | fatura, vencimento, parcelas, dia 31 |
| `FinanceApiTest` | 7 | API de finanças |
| `RecurrenceJobTest` | 1 | job das recorrências |
| `HabitStatsTest` | 5 | sequências: diário, dias fixos, X por semana, início |
| `DailyValuesTest` | 3 | valor do dia no fuso; metas mínima e máxima |
| `HealthApiTest` | 5 | API de saúde e hábitos |
| `GoalProgressTest` | 3 | percentual, esperado e ritmo |
| `GoalApiTest` | 5 | API de metas, marcos, projetos e tarefas |
| `VidaApplicationTests` | 1 | contexto sobe com Postgres 18 |

Todo módulo tem teste de **isolamento entre usuários** (o outro usuário recebe 404) e pelo menos um teste mostrando que a **FK composta barra o vínculo cruzado direto no banco**.

O front passa em `npm run lint` e `npm run build` sem avisos. Os fluxos das cinco fases foram testados no Chromium com Playwright, incluindo o layout no celular.

---

## 7. Como rodar

```bash
docker compose up -d db                     # Postgres 18
cd backend && ./mvnw spring-boot:run        # API em http://localhost:8080/api (Swagger: /api/docs)
cd frontend && npm install && npm run dev   # http://localhost:5173 (proxy de /api)
cd backend && ./mvnw verify                 # testes (precisa de Docker)
cd frontend && npm run api:types            # regenera os tipos (com a API rodando)
```

O autor já tem JDK 25, Node 22+ e Docker instalados na máquina.

### Particularidades do sandbox (só para quem continuar numa sessão na nuvem)
- **JDK 25:** não vem instalado. Foi extraído da imagem `eclipse-temurin:25-jdk` para o scratchpad e usado via `JAVA_HOME`.
- **Docker:** o daemon não sobe sozinho. É preciso iniciar com `dockerd` em segundo plano, e ele cai quando a sessão reinicia.
- **Build da imagem da API:** dentro do sandbox, falha por causa do certificado do proxy. Na máquina do autor o `Dockerfile` funciona normalmente.
- **`openapi-typescript`:** exige TypeScript 5, por isso o script `api:types` roda via `npx`, isolado do projeto (que usa TS 6).

---

## 8. Lacunas conhecidas (já combinadas ou anotadas)

- **Acadêmico:**
  - Escolher o assunto ao registrar uma sessão de estudo (a API aceita, a tela não).
  - Ligar assuntos a uma prova pela tela.
  - Editar nome ou cor de uma disciplina já criada.
- **Finanças:**
  - Editar um lançamento já criado pela tela (a API aceita; a tela só marca como pago ou apaga).
  - Editar uma recorrência pela tela.
  - Estorno no cartão.
  - Importar extrato.
- **Saúde:**
  - Editar nome, frequência ou meta de um hábito ou métrica já criados (só arquivar e apagar pela tela).
- **Metas:**
  - Editar título, prazo ou modo de uma meta pela tela (a API aceita; a tela atualiza só o valor manual e o status).
  - Editar uma tarefa já criada (título, prazo, vínculos).
  - Metas dentro de metas (deixadas de fora; os marcos cobrem as etapas).
- **Geral:**
  - Tema escuro.
  - Remédios, lembretes e PWA (depois da v1).

---

## 9. Próximas fases (plano aprovado)

### Fase 6 — Hoje
Endpoint agregado `GET /api/hoje` e a tela inicial definitiva, reunindo:
- aulas do dia e avaliações próximas;
- tarefas vencendo ou atrasadas;
- hábitos a marcar;
- faltas em risco;
- orçamento do mês e contas a pagar;
- minutos estudados na semana;
- eventos.

Hoje a tela Hoje já monta esses cartões chamando vários endpoints.
