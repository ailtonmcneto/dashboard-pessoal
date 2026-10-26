# Vida — dashboard pessoal

Painel para organizar a vida: **estudos** (faculdade, cursos, projetos), **finanças**, **saúde e hábitos** e **metas e planos**, com uma tela **Hoje** que junta tudo.

| Camada | Stack |
|---|---|
| API | Java 25 · Spring Boot 4 (Web MVC, Data JPA, Validation, Security) · Flyway · springdoc-openapi |
| Banco | PostgreSQL 18 (ids `uuidv7()`) |
| Sessão | Spring Session JDBC (no próprio Postgres) |
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

**Primeiro acesso:** enquanto não existe nenhum usuário, a tela de login vira "Criar conta". Depois do primeiro cadastro ela fica fechada (uso pessoal). Para aceitar novos cadastros, rode a API com `VIDA_AUTH_SIGNUP_OPEN=true`.

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
    db/migration/            migrações Flyway (V1 núcleo, V2 sessões, V3 acadêmico, V4 finanças, V5 saúde, V6 metas)
frontend/                    SPA React + Vite + TS
docker-compose.yml           Postgres 18 (+ API no profile "api")
```

## Autenticação

Sessão no servidor com cookie `httpOnly` (sem JWT), guardada no Postgres pelo Spring Session JDBC (tabelas `spring_session*`, migração V2): reiniciar a API não desloga, e a sessão dura 14 dias. Como front e API ficam no mesmo domínio (proxy do Vite em dev), o navegador manda o cookie sozinho. Requisições que alteram dados levam o token CSRF: a API grava o cookie `XSRF-TOKEN` e o front devolve o valor no header `X-XSRF-TOKEN` (`frontend/src/api/client.ts`).

Erros da API seguem o formato `application/problem+json`; erros de validação trazem `errors: {campo: mensagem}`.

## Convenções do banco

- Chave primária `uuid` gerada com `uuidv7()` (ordenada no tempo, não adivinhável).
- `user_id` em todas as tabelas e chaves estrangeiras compostas `(x_id, user_id)`: o banco não aceita ligar dados de usuários diferentes.
- Dinheiro em `numeric(14,2)`, dias em `date`, instantes em `timestamptz`.
- Saldos, médias e progresso são calculados, nunca armazenados.
- Nos records de request/response, campo sem `@Nullable` é obrigatório no OpenAPI; com `@Nullable`, opcional e pode ser `null`. É isso que define os tipos gerados no front.

## Estudos: regras de cálculo

- **Média:** ponderada, com cada nota normalizada para 0–10 (`nota / nota máxima × 10`). A disciplina mostra quanto falta tirar, em média, nas avaliações ainda sem nota para chegar na média de aprovação do semestre, e a situação: *sem notas*, *em dia*, *em risco* (precisa de mais que a média), *aprovado* ou *reprovado* por nota.
- **Faltas:** alerta a partir de 75% do limite cadastrado na disciplina.
- **Revisão espaçada:** ao revisar um assunto você informa o domínio de 0 a 5, que define a próxima revisão: 1, 2, 4, 7, 15 ou 30 dias.
- **Sessões de estudo:** apagar uma disciplina ou curso mantém as horas estudadas, que passam a contar como "Sem vínculo".

## Finanças: regras

- **Saldo** nunca é guardado: é o saldo inicial mais a soma dos lançamentos. *Saldo* conta só o que já foi pago/recebido; *previsto* inclui os pendentes.
- **Transferência** move dinheiro entre contas e não entra como receita nem despesa.
- **Cartão:** a compra é despesa na data em que foi feita e entra na fatura pelo dia de fechamento (do dia do fechamento em diante, vai para a fatura seguinte). **Pagar a fatura** tira o dinheiro da conta, mas não conta de novo como despesa.
- **Parcelado:** vira um lançamento por mês; os centavos que sobram ficam na 1ª parcela.
- **Recorrências** geram os lançamentos sozinhas (ao criar, e depois de hora em hora). Em conta, nascem *pendentes* para você confirmar. Mensal no dia 31 cai no último dia dos meses curtos, e volta para 31 quando o mês tem.
- **Orçamento** é por categoria e mês, conta as subcategorias e avisa a partir de 80%.

## Saúde e hábitos: regras

- **Métricas** são suas (sono, água, peso…): cada uma tem unidade, uma regra para o valor do dia — *soma* (água, treino), *média* (humor) ou *último registro* (peso) — e, se quiser, uma meta diária *mínima* ("pelo menos 2 L") ou *máxima* ("no máximo 2 h de tela"). O dia é contado no seu fuso.
- **Gráficos:** métricas de soma aparecem em colunas por dia; média e último, em linha ligando os dias com registro (o que importa é a tendência). A meta aparece como linha de referência.
- **Hábitos:** *todo dia*, *dias fixos* da semana ou *X vezes por semana* (qualquer dia). A sequência é calculada: hoje ainda não feito não quebra a sequência; dias fora da frequência são pulados; no "X por semana" a sequência conta semanas que bateram a meta. Também há recorde, progresso da semana e taxa dos últimos 30 dias.

## Metas e planos: regras

- **Progresso da meta**, sempre calculado, de um de quatro jeitos: *por marcos* (concluídos ÷ total), *por tarefas* (tarefas ligadas feitas ÷ total), *valor manual* (ex.: 6 de 12 livros) ou *saldo de uma conta* até o valor-alvo (ex.: juntar R$ 10 mil para a viagem).
- **Ritmo:** com início e prazo, o esperado é a fração do tempo que já passou. A meta fica *adiantada*, *no ritmo* (até 10 pontos de diferença) ou *atrasada*; passado o prazo sem chegar a 100%, *atrasada*. Na barra, um traço marca onde você deveria estar hoje.
- **Tarefas** formam uma lista só, com visões *Hoje* (vencidas e de hoje), *Próximos 7 dias*, *Todas abertas* e *Feitas*. Cada tarefa pode ser de uma meta, de um marco (aí conta para a meta dele), de um projeto e/ou de uma disciplina. Apagar meta, projeto ou disciplina não apaga as tarefas.
- **Projetos** mostram o progresso pelas tarefas e avisam quando passam do prazo.

## Roadmap

0. ~~Esqueleto do projeto~~
1. ~~Núcleo e autenticação (sessão + CSRF), agenda~~
2. ~~Acadêmico — semestres, disciplinas, horário, avaliações, faltas, assuntos, cursos, sessões de estudo~~
3. ~~Finanças — contas, cartões, categorias, lançamentos, parcelas, recorrência, orçamento~~
4. ~~Saúde e hábitos — métricas, medições, hábitos com sequência~~
5. ~~Metas e planos — metas, marcos, projetos, tarefas~~
6. Hoje — visão agregada do dia

Depois da v1: controle de remédios, lembretes, importação de extrato, PWA.
