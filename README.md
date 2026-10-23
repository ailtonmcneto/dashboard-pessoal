# Vida — dashboard pessoal

Painel para organizar a vida: **estudos** (faculdade, cursos, projetos), **finanças**, **saúde e hábitos** e **metas e planos**, com uma tela **Hoje** que junta tudo.

| Camada | Stack |
|---|---|
| API | Java 25 · Spring Boot 4 (Web MVC, Data JPA, Validation, Security) · Flyway · springdoc-openapi |
| Banco | PostgreSQL 18 (ids `uuidv7()`) |
| Web | React · Vite · TypeScript · TanStack Query · React Router · Tailwind CSS |
| Testes | JUnit 5 · Testcontainers (Postgres real) |

## Pré-requisitos

- JDK 25
- Node 22+
- Docker (para o Postgres e para os testes com Testcontainers)

## Rodando

```bash
# 1. banco
docker compose up -d db

# 2. API — http://localhost:8080/api (Swagger em /api/docs)
cd backend
./mvnw spring-boot:run

# 3. front — http://localhost:5173 (repassa /api para a porta 8080)
cd frontend
npm install
npm run dev
```

Para subir a API também em container: `docker compose --profile api up -d --build`.

## Comandos úteis

| Onde | Comando | O que faz |
|---|---|---|
| `backend/` | `./mvnw verify` | compila e roda os testes (sobe um Postgres 18 via Testcontainers) |
| `frontend/` | `npm run lint` / `npm run build` | lint (oxlint) e checagem de tipos + build |
| `frontend/` | `npm run api:types` | gera `src/api/schema.d.ts` a partir do OpenAPI da API rodando |

## Estrutura

```
backend/                     API Spring Boot
  src/main/java/com/vida/    um pacote por módulo (auth, academico, financas, saude, metas, hoje…)
  src/main/resources/
    db/migration/            migrações Flyway (V1__..., V2__...)
frontend/                    SPA React + Vite + TS
docker-compose.yml           Postgres 18 (+ API no profile "api")
```

## Convenções do banco

- Chave primária `uuid` gerada com `uuidv7()` (ordenada no tempo, não adivinhável).
- `user_id` em todas as tabelas e chaves estrangeiras compostas `(x_id, user_id)`: o banco não aceita ligar dados de usuários diferentes.
- Dinheiro em `numeric(14,2)`, dias em `date`, instantes em `timestamptz`.
- Saldos, médias e progresso são calculados, nunca armazenados.

## Roadmap

0. ~~Esqueleto do projeto~~
1. Núcleo e autenticação (sessão + CSRF)
2. Acadêmico — semestres, disciplinas, horário, avaliações, faltas, assuntos, cursos, sessões de estudo
3. Finanças — contas, cartões, categorias, lançamentos, parcelas, recorrência, orçamento
4. Saúde e hábitos — métricas, medições, hábitos com sequência
5. Metas e planos — metas, marcos, projetos, tarefas
6. Hoje — visão agregada do dia

Depois da v1: controle de remédios, lembretes, importação de extrato, PWA.
