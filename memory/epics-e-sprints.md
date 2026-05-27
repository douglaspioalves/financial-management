# Epics, Sprints e Backlog — Gestor de Gastos

> Fonte de verdade do roadmap de execução.
> O Scrum Master usa este arquivo no planning de cada sprint.
> Sprints de 2 semanas · 7 sprints · ~14 semanas no total.

---

## Mapa de sprints

| Sprint | Fatia | Epic | Período sugerido |
|---|---|---|---|
| 01 | 0 + 1 | Fundação + Autenticação | Semanas 1–2 |
| 02 | 2 | Lançamentos e Categorias | Semanas 3–4 |
| 03 | 3a | Cartões | Semanas 5–6 |
| 04 | 3b | Parcelamento | Semanas 7–8 |
| 05 | 4 + 5a | Dashboard + Orçamento | Semanas 9–10 |
| 06 | 5b + 6 | Recorrência + Acerto de Contas | Semanas 11–12 |
| 07 | 7 | Exportação + Qualidade Final | Semanas 13–14 |

---

## EPIC 01 — Fundação e Autenticação
**Sprint 01 | Fatias 0 e 1**
**Objetivo:** Ter o app rodando de ponta a ponta com login funcional.

### Stories

#### S-01-01 · Esqueleto do monorepo e Docker Compose
**Papel:** DevOps
**Pontos:** 3
**Critérios de aceite:**
- Estrutura `backend/`, `frontend/`, `docker-compose.yml` no repositório.
- `docker compose up --build` sobe Postgres, backend e frontend sem erro.
- Backend responde `GET /api/health` → `{ "status": "ok" }`.
- Frontend Angular abre em localhost:4200.
- README com instruções de instalação no Windows (PowerShell).

**Tarefas:**
- [ ] Criar projeto Spring Boot via Spring Initializr (Web, JPA, Security, Validation, Flyway, PostgreSQL) — DevOps
- [ ] Criar projeto Angular + Angular Material — DevOps
- [ ] Escrever `docker-compose.yml` (postgres, backend, frontend) — DevOps
- [ ] Criar `.env.example` com variáveis de ambiente — DevOps
- [ ] Implementar `GET /api/health` — Backend
- [ ] Criar componente Angular inicial que chama `/api/health` — Frontend
- [ ] Escrever README (instalação + comando de subida) — DevOps

---

#### S-01-02 · Migration inicial do banco
**Papel:** DBA
**Pontos:** 2
**Depende de:** S-01-01
**Critérios de aceite:**
- Migration `V1__initial_schema.sql` cria todas as tabelas do modelo v1.
- Migration `V2__seeds.sql` insere as duas `Person` e as categorias iniciais.
- Flyway aplica migrations automaticamente no start do backend.

**Tarefas:**
- [ ] Escrever `V1__initial_schema.sql` (user, person, category, card, transaction, installment, budget, recurring_rule) — DBA
- [ ] Definir índices, FKs e constraints — DBA
- [ ] Escrever `V2__seeds.sql` (2 persons + 12 categorias) — DBA
- [ ] Validar que `docker compose up` aplica tudo num banco zerado — DevOps

---

#### S-01-03 · Cadastro e login com JWT
**Papel:** Backend
**Pontos:** 5
**Depende de:** S-01-02
**Critérios de aceite:**
- `POST /api/auth/register` cria usuário com senha BCrypt; retorna 201.
- `POST /api/auth/login` valida credenciais; retorna JWT válido.
- Endpoints protegidos retornam 401 sem JWT.
- Mensagens de erro em pt-br.

**Tarefas:**
- [ ] Implementar entidade `User` e `UserRepository` — Backend
- [ ] Implementar `AuthService` (register + login + geração JWT) — Backend
- [ ] Implementar `AuthController` (`/api/auth/register`, `/api/auth/login`) — Backend
- [ ] Configurar Spring Security (filtro JWT, rotas públicas/protegidas) — Backend
- [ ] Escrever testes de integração para register e login — QA

---

#### S-01-04 · Telas de login e cadastro
**Papel:** Frontend
**Pontos:** 4
**Depende de:** S-01-03
**Critérios de aceite:**
- Tela de login com email/senha e botão entrar.
- Tela de cadastro com nome, email, senha e confirmação.
- JWT armazenado após login; guard redireciona para login se não autenticado.
- Modo claro e escuro funcionando conforme design system.
- Responsivo no celular.

**Tarefas:**
- [ ] Aplicar tema Angular Material com paleta do design system — Frontend
- [ ] Criar módulo `auth` com telas de login e cadastro — Frontend
- [ ] Implementar `AuthService` (HTTP + armazenamento do token) — Frontend
- [ ] Criar `AuthGuard` para proteger rotas — Frontend
- [ ] Implementar toggle de tema claro/escuro — Frontend

---

## EPIC 02 — Lançamentos e Categorias
**Sprint 02 | Fatia 2**
**Objetivo:** Registrar e listar despesas e receitas com categorias.

### Stories

#### S-02-01 · API de categorias
**Papel:** Backend + DBA
**Pontos:** 2
**Critérios de aceite:**
- `GET /api/categories` lista categorias (com seeds).
- `POST /api/categories` cria categoria personalizada.
- `PUT /api/categories/{id}` edita nome/cor.
- `DELETE /api/categories/{id}` — decide: impedir ou arquivar se há lançamentos. ⚠️ Decisão pendente.

**Tarefas:**
- [ ] Implementar `Category` entity, repository, service, controller — Backend
- [ ] Tratar a decisão de exclusão com lançamentos (registrar em `memory/decisions/`) — Arquiteto
- [ ] Testes unitários de CategoryService — QA

---

#### S-02-02 · API de lançamentos (CRUD)
**Papel:** Backend
**Pontos:** 8
**Depende de:** S-02-01
**Critérios de aceite:**
- `POST /api/transactions` cria lançamento; valida todos os campos.
- `GET /api/transactions?month=yyyy-MM` lista por mês.
- `PUT /api/transactions/{id}` edita; `DELETE /api/transactions/{id}` remove.
- Lançamento à vista (`installments_total=1`) não gera `Installment`.
- Todos os endpoints exigem autenticação.

**Tarefas:**
- [ ] Implementar `Transaction` entity + `TransactionRepository` — Backend
- [ ] Implementar `TransactionService` (CRUD) — Backend
- [ ] Implementar `TransactionController` — Backend
- [ ] Validação de campos (valor positivo, data obrigatória, categoria existente, etc.) — Backend
- [ ] Testes de integração dos endpoints — QA

---

#### S-02-03 · Tela de lançamentos
**Papel:** Frontend
**Pontos:** 6
**Depende de:** S-02-02
**Critérios de aceite:**
- Lista de lançamentos do mês com filtro de mês anterior/próximo.
- Formulário de novo lançamento com todos os campos (tipo, valor, data, categoria, quem pagou, divisão).
- Edição e exclusão com confirmação.
- Valores em R$ com sinal +/− em cores do design system.
- Responsivo.

**Tarefas:**
- [ ] Criar módulo `transactions` — Frontend
- [ ] Componente de lista com filtro de mês — Frontend
- [ ] Formulário reativo de novo/editar lançamento — Frontend
- [ ] Serviço HTTP `TransactionService` — Frontend
- [ ] Componente de confirmação de exclusão — Frontend

---

## EPIC 03 — Cartões e Parcelamento
**Sprints 03–04 | Fatia 3**
**Objetivo:** Registrar compras no cartão com geração automática de parcelas.

### Stories

#### S-03-01 · API de cartões
**Papel:** Backend
**Pontos:** 3
**Critérios de aceite:**
- `GET /api/cards` lista cartões.
- `POST /api/cards` cria cartão (nome, titular, fechamento, vencimento).
- `PUT /api/cards/{id}` edita; `DELETE /api/cards/{id}` remove se sem lançamentos.

**Tarefas:**
- [ ] Implementar `Card` entity, repository, service, controller — Backend
- [ ] Validar `closing_day` e `due_day` entre 1–31 — Backend
- [ ] Testes de CardService — QA

---

#### S-03-02 · Tela de cartões
**Papel:** Frontend
**Pontos:** 3
**Depende de:** S-03-01
**Critérios de aceite:**
- Lista de cartões com titular, fechamento e vencimento.
- Formulário de criação/edição.
- Cartão selecionável no formulário de lançamento quando método = crédito.

**Tarefas:**
- [ ] Módulo `cards` com lista e formulário — Frontend
- [ ] Integrar seleção de cartão no formulário de lançamento — Frontend

---

#### S-04-01 · Geração de parcelas
**Papel:** Backend + DBA
**Pontos:** 8
**Depende de:** S-03-01
**Critérios de aceite:**
- `POST /api/transactions` com `installments_total > 1` e `payment_method=CREDIT` gera N `Installment`.
- Cada `Installment.reference_month` calculado com base em `closing_day` do cartão.
- Soma das parcelas = valor total da compra (última parcela absorve diferença de centavos).
- Excluir `Transaction` cascateia exclusão das `Installment`.

**Tarefas:**
- [ ] Implementar `InstallmentService.generateInstallments()` — Backend
- [ ] Lógica de cálculo de `reference_month` por `closing_day` — Backend
- [ ] Lógica de arredondamento na última parcela — Backend
- [ ] Testes unitários: parcela em mês certo, arredondamento, virada de ano — QA
- [ ] Endpoint `GET /api/transactions/{id}/installments` — Backend

---

#### S-04-02 · Visualização de parcelas
**Papel:** Frontend
**Pontos:** 4
**Depende de:** S-04-01
**Critérios de aceite:**
- Lançamentos parcelados exibem badge "X/N" na lista.
- Expandir lançamento mostra todas as parcelas com mês e valor.
- Mês atual destaca a parcela correspondente.

**Tarefas:**
- [ ] Badge de parcela no componente de lista — Frontend
- [ ] Componente de detalhe/expansão de parcelas — Frontend

---

## EPIC 04 — Dashboard
**Sprint 05 | Fatia 4**
**Objetivo:** Visualização financeira do mês em tela inicial.

### Stories

#### S-05-01 · API de dashboard
**Papel:** Backend
**Pontos:** 5
**Critérios de aceite:**
- `GET /api/dashboard?month=yyyy-MM` retorna: total receitas, total despesas, saldo, gastos por categoria (com %) e lista dos últimos 10 lançamentos.
- Usa `Installment.reference_month` para parcelas.
- Comparativo com mês anterior incluído.

**Tarefas:**
- [ ] Implementar `DashboardService` com agregações — Backend
- [ ] Endpoint `GET /api/dashboard` — Backend
- [ ] Testes de agregação (mês com parcelas, mês sem receita) — QA

---

#### S-05-02 · Tela de dashboard
**Papel:** Frontend
**Pontos:** 6
**Depende de:** S-05-01
**Critérios de aceite:**
- Cards de receita, despesa e saldo com cores do design system.
- Gráfico de pizza/donut por categoria.
- Lista dos últimos lançamentos.
- Comparativo com mês anterior (% de variação).
- Responsivo; é a tela inicial após login.

**Tarefas:**
- [ ] Módulo `dashboard` com layout de cards — Frontend
- [ ] Integrar biblioteca de gráficos (Angular Material ou Chart.js) — Frontend
- [ ] Componente de comparativo mês a mês — Frontend
- [ ] Definir como tela inicial (rota padrão pós-login) — Frontend

---

## EPIC 05 — Orçamento e Recorrência
**Sprint 05–06 | Fatia 5**
**Objetivo:** Planejar gastos mensais e automatizar lançamentos fixos.

### Stories

#### S-05-03 · API de orçamento
**Papel:** Backend
**Pontos:** 4
**Critérios de aceite:**
- `GET /api/budgets?month=yyyy-MM` lista orçamentos com valor gasto vs. limite.
- `POST /api/budgets` define limite por categoria/mês.
- Cálculo de % consumido do orçamento por categoria.

**Tarefas:**
- [ ] Implementar `Budget` entity, service, controller — Backend
- [ ] Agregação de gasto real vs. limite no service — Backend
- [ ] Testes de estouro de orçamento — QA

---

#### S-06-01 · API de lançamentos recorrentes
**Papel:** Backend
**Pontos:** 5
**Critérios de aceite:**
- `POST /api/recurring-rules` cria regra de recorrência (frequência, próxima data, template do lançamento).
- Job diário gera lançamentos automaticamente na data correta.
- `GET /api/recurring-rules` lista regras ativas.
- `DELETE /api/recurring-rules/{id}` desativa.

**Tarefas:**
- [ ] Implementar `RecurringRule` entity e service — Backend
- [ ] Implementar job agendado (`@Scheduled`) para gerar lançamentos — Backend
- [ ] Testes do job (geração na data certa, sem duplicar) — QA

---

#### S-06-02 · Telas de orçamento e recorrência
**Papel:** Frontend
**Pontos:** 5
**Depende de:** S-05-03 + S-06-01
**Critérios de aceite:**
- Barras de progresso de orçamento por categoria (verde/amarelo/vermelho conforme % consumido).
- Alerta visual quando categoria ultrapassa o limite.
- Tela de gestão de recorrências (criar, listar, desativar).

**Tarefas:**
- [ ] Módulo `budget` com barras de progresso — Frontend
- [ ] Componente de alerta de estouro — Frontend
- [ ] Módulo de recorrências com formulário de criação — Frontend

---

## EPIC 06 — Acerto de Contas
**Sprint 06 | Fatia 6**
**Objetivo:** Calcular e exibir quem deve quanto a quem no mês.

### Stories

#### S-06-03 · API de acerto de contas
**Papel:** Backend
**Pontos:** 8
**Depende de:** S-04-01
**Critérios de aceite:**
- `GET /api/settlement?month=yyyy-MM` retorna: quanto cada pessoa pagou, quanto deveria pagar, e saldo ("X deve Y ao outro").
- Usa `Installment.reference_month` para parcelas parceladas.
- `PROPORTIONAL`: usa soma de receitas `PERSON_A/B` do mês; mês sem receita → `pending: true` na resposta.
- Resultado claro: `{ "debtor": "João", "creditor": "Maria", "amount": 312.45 }` ou `{ "settled": true }`.

**Tarefas:**
- [ ] Implementar `SettlementService.calculate(month)` — Backend
- [ ] Lógica para `PROPORTIONAL` com receitas do mês — Backend
- [ ] Lógica de `pending` quando sem receita individual — Backend
- [ ] Endpoint `GET /api/settlement` — Backend
- [ ] Testes unitários: 50/50, proporcional, proporcional sem receita, só de uma pessoa — QA
- [ ] Decidir se mês passado pode ser editado → registrar em `memory/decisions/` — Arquiteto

---

#### S-06-04 · Tela de acerto de contas
**Papel:** Frontend
**Pontos:** 5
**Depende de:** S-06-03
**Critérios de aceite:**
- Exibe quem deve quanto a quem com destaque visual.
- Se `pending: true`, exibe alerta "Cadastre as receitas de [mês] para calcular a proporção".
- Breakdown: quanto cada pessoa pagou e deveria pagar por item.
- Navegação por mês.

**Tarefas:**
- [ ] Módulo `settlement` com layout de acerto — Frontend
- [ ] Componente de alerta de proporção pendente — Frontend
- [ ] Breakdown por pessoa e por despesa proporcional — Frontend

---

## EPIC 07 — Exportação e Qualidade Final
**Sprint 07 | Fatia 7**
**Objetivo:** Exportação de dados e polimento geral para uso real.

### Stories

#### S-07-01 · Exportação Excel/CSV
**Papel:** Backend + Frontend
**Pontos:** 4
**Critérios de aceite:**
- `GET /api/export?month=yyyy-MM&format=csv` (ou xlsx) retorna arquivo.
- Colunas: data, descrição, categoria, quem pagou, valor, divisão, parcela.
- Botão de exportar na tela de lançamentos.

**Tarefas:**
- [ ] Implementar export service (Apache POI para xlsx) — Backend
- [ ] Endpoint de download — Backend
- [ ] Botão de exportar no frontend — Frontend

---

#### S-07-02 · Testes de ponta a ponta e revisão de segurança
**Papel:** QA + Revisor
**Pontos:** 5
**Critérios de aceite:**
- Todos os endpoints exigem autenticação (exceto login/registro).
- Nenhum dado sensível exposto em respostas.
- Testes de integração cobrindo o fluxo completo (login → lançamento → parcela → acerto).
- Performance aceitável com 12 meses de dados.

**Tarefas:**
- [ ] Revisar segurança de todos os endpoints — Revisor
- [ ] Testes de fluxo completo — QA
- [ ] Teste de carga leve (simulação de 1 ano de dados) — QA

---

#### S-07-03 · Polimento de UX e responsividade
**Papel:** Frontend
**Pontos:** 4
**Critérios de aceite:**
- App funciona bem no celular (testado em viewport mobile).
- Estados vazios (mês sem dados) com mensagens amigáveis.
- Loading states em todas as chamadas de API.
- Modo escuro revisado em todas as telas.

**Tarefas:**
- [ ] Auditar responsividade em todas as telas — Frontend
- [ ] Implementar estados vazios — Frontend
- [ ] Implementar loading/skeleton em listas e cards — Frontend
- [ ] Revisão final do modo escuro — Frontend

---

#### S-07-04 · Documentação final e tag de release
**Papel:** DevOps + Arquiteto
**Pontos:** 2
**Critérios de aceite:**
- README atualizado refletindo o estado final.
- `docs/api.md` completo e fiel à implementação.
- Tag `v1.0.0` criada no git.
- `memory/decisions/` e `memory/learnings/` atualizados.

**Tarefas:**
- [ ] Atualizar README e api.md — DevOps + Arquiteto
- [ ] Revisão final de memory/ — Scrum Master
- [ ] Criar tag `v1.0.0` — DevOps
