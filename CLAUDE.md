# Gestor de Gastos — Contexto do Projeto

> Este arquivo é lido automaticamente pelo Claude Code. Mantenha-o atualizado.
> Ele é a fonte de verdade sobre stack, convenções e decisões do projeto.

## O que é

Aplicação web para controle de gastos e receitas entre duas pessoas (um casal),
com acerto de contas e suporte a compras parceladas no cartão de crédito.

- **Dois logins independentes**, sem vínculo formal entre as contas.
- **Base de dados compartilhada**: ambos os logins enxergam os mesmos dados.
- As duas pessoas aparecem como **participantes** (`Person`) nos lançamentos,
  usados para "quem pagou" e a regra de divisão.

## Stack

- **Backend:** Java 21 + Spring Boot 3 (Web, Data JPA, Security, Validation), Maven, JWT.
- **Banco:** PostgreSQL, migrations com Flyway.
- **Frontend:** Angular (versão recente) + Angular Material, responsivo.
- **Infra local:** Docker Compose (Postgres + backend + frontend) — sobe com um comando.
- **Ambiente de desenvolvimento:** Windows.

## REGRA DE IDIOMA (importante)

- **Código em INGLÊS:** classes, métodos, variáveis, tabelas, colunas, endpoints, branches.
  Ex.: `Transaction`, `findByMonth`, `paid_by_person_id`, `/api/transactions`.
- **Português (pt-br) em:** textos de interface (labels, botões, mensagens de erro
  ao usuário), documentação, comentários relevantes e mensagens de commit.
- Mensagens de validação/erro expostas na API devem vir em pt-br (são exibidas ao usuário).

## Convenções de código

- Backend em camadas: `controller` → `service` → `repository` → `domain`.
- DTOs para entrada/saída; nunca exponha entidades JPA direto no controller.
- Toda alteração de schema é uma **migration Flyway versionada** (nunca `ddl-auto: update` em produção).
- Validação com Bean Validation (`@NotNull`, `@Positive`, etc.), mensagens em pt-br.
- Valores monetários: `BigDecimal` (nunca `double`/`float`).
- Datas: `LocalDate` / `LocalDateTime`.
- Frontend: módulos por feature (`auth`, `transactions`, `dashboard`, `cards`,
  `budget`, `settlement`); serviços HTTP isolados; guards de rota para autenticação.

## Modelo de dados (resumo)

- **User**: id, name, email, password_hash, created_at. (login; dois usuários)
- **Person**: id, name, color. (participante/rótulo; sem login; app já vem com as duas)
  Nota: NÃO tem campo de renda. A proporção da divisão `PROPORTIONAL` é calculada a partir
  das receitas (Transaction type=INCOME) lançadas no mês, não de um valor fixo cadastrado.
- **Category**: id, name, type (EXPENSE/INCOME/BOTH), color.
- **Card**: id, owner_person_id, name, closing_day, due_day.
- **Transaction**: id, type (EXPENSE/INCOME), amount, date, category_id,
  paid_by_person_id, payment_method (CASH/DEBIT/CREDIT/PIX/TRANSFER), card_id,
  split_rule (PERSON_A/PERSON_B/FIFTY_FIFTY/PROPORTIONAL), installments_total, description.
  Nota: uma **receita individual** (que alimenta a proporção) é `type=INCOME` com
  `split_rule=PERSON_A` ou `PERSON_B`. Uma receita `FIFTY_FIFTY` é compartilhada e
  NÃO entra no cálculo da proporção.
- **Installment**: id, transaction_id, number, amount, reference_month.
  → Gerada automaticamente para compras parceladas. É o que entra no acerto mensal.
- **Budget**: category_id, month, limit_amount.
- **RecurringRule**: template do lançamento, frequência, próxima data de geração.

## Regras de negócio críticas (cobrir com testes)

1. **Parcelamento:** uma compra parcelada em N vezes gera N `Installment`, cada uma
   no `reference_month` correto, considerando fechamento/vencimento do cartão.
   O valor total pode não dividir exato — trate o arredondamento (última parcela ajusta).
2. **Acerto de contas:** o que entra no acerto de cada mês são as `Installment` que caem
   naquele mês, NÃO o valor cheio da compra no mês da compra.
3. **Divisão:** `split_rule` define quanto cada participante deve. O acerto soma quanto
   cada um pagou vs. quanto deveria pagar e calcula "quem deve quanto a quem".
   - `FIFTY_FIFTY`: 50% para cada.
   - `PERSON_A` / `PERSON_B`: 100% da despesa atribuída a uma das pessoas.
   - `PROPORTIONAL`: proporcional à **receita real lançada no mês**. Regras:
     a) Considera apenas as **receitas individuais** de cada pessoa no mês
        (transactions type=INCOME, split atribuído à própria pessoa). Receitas
        compartilhadas NÃO entram no cálculo da proporção.
     b) A proporção usada é a do **mês em que a despesa/parcela cai** (não o mês da compra),
        para manter coerência com a lógica de parcelas.
     c) Se o mês ainda não tem receita individual lançada, a proporção é **indefinida**:
        não assuma 50/50 — o acerto deve sinalizar pendência ("cadastre as receitas do mês
        para calcular a divisão proporcional") e não computar essas despesas até haver receita.
4. **Concorrência:** dois logins editam a mesma base. Trate edição/remoção concorrente
   (ex.: optimistic locking com `@Version`) para evitar sobrescrita silenciosa.

## Segurança

- Senhas com BCrypt. Autenticação via JWT em todos os endpoints (nada público além de login/registro).
- A base é compartilhada entre as duas contas — isso é intencional. Não há multi-tenant.

## Design System (APROVADO)

- **Cor principal:** azul (#4a7fc4) — confiança, ações, navegação.
- **Apoios pastéis com significado fixo:** verde-menta = receita/positivo;
  coral = despesa/atenção; areia = aviso; lilás = destaque.
- **Tipografia:** Fraunces (display — títulos e valores monetários) +
  Plus Jakarta Sans (corpo e interface).
- **Modos claro e escuro** via CSS variables (ver design-system.html).
- **Clima:** aconchegante e suave — cantos arredondados (10–24px), sombras leves, respiro.
- Valores monetários em fonte display, com sinal +/− sempre visível.
- Referência visual: `docs/design-system.html`.

## Roadmap (fatias verticais — uma de cada vez)

0. Esqueleto: Docker Compose + backend respondendo + Angular abrindo + banco conectado.
1. Cadastro e login (JWT) dos dois usuários.
2. CRUD de lançamentos + categorias.
3. Cartões + compras parceladas (geração de Installment).
4. Dashboard: total do mês, gráfico por categoria, comparativo.
5. Orçamento por categoria + lançamentos recorrentes.
6. Acerto de contas (quem deve quanto a quem, com parcelas).
7. Exportação Excel/CSV + ajustes finais (extra).

## Fluxo de trabalho por fatia

1. Arquiteto detalha a fatia em tarefas + atualiza o contrato da API.
2. Migrations preparadas/ajustadas.
3. Backend implementa os endpoints.
4. QA cobre a lógica crítica com testes.
5. Frontend constrói as telas que consomem os endpoints.
6. Revisor passa o pente fino (segurança + qualidade).
7. Você roda o roteiro de teste manual e aprova.
8. Commit/tag da fatia; garantir que sobe limpo no `docker compose up`.

## Comandos úteis

- Subir tudo: `docker compose up --build`
- Backend (dev): `cd backend && ./mvnw spring-boot:run`
- Frontend (dev): `cd frontend && npm start`
- Testes backend: `cd backend && ./mvnw test`
- Nova migration: criar `backend/src/main/resources/db/migration/V{n}__descricao.sql`

## Regras gerais para o Claude Code

- Trabalhe **uma fatia por vez**. No início de cada sessão, diga qual fatia e qual agente está ativo.
- Faça commits pequenos e descritivos (em pt-br) ao concluir cada tarefa aprovada.
- Ao terminar uma fatia, rode os testes e suba o `docker compose` para validar.
- Não pule a Fatia 0.
- Quando em dúvida sobre escopo, consulte `docs/plano.md` ou pergunte antes de implementar.

## Processo Scrum

- **Sprints de 2 semanas** (10 dias úteis). 7 sprints no total — ver `memory/epics-e-sprints.md`.
- **Agente Scrum Master** (`.claude/agents/scrum-master.md`) conduz todas as cerimônias.
- **Cerimônias obrigatórias por sprint:** Planning → Dailies → Review → Retro.
- **Diretório `/memory`** é a memória viva do projeto. Nunca deletar arquivos de lá.

### Rotina de início de sessão (todo agente, toda sessão)
1. Ler `CLAUDE.md`.
2. Ler `memory/sprints/sprint-NN.md` do sprint atual.
3. Ler o último `memory/dailies/YYYY-MM-DD.md` disponível.
4. Ler `memory/learnings/<sua-area>.md`.

### Rotina de fim de sessão (todo agente)
1. Atualizar status das tarefas em `memory/sprints/sprint-NN.md`.
2. Preencher seção FIM DO DIA no `memory/dailies/YYYY-MM-DD.md` do dia.
3. Se tomou decisão técnica ou de produto → criar `memory/decisions/YYYY-MM-DD-titulo.md`.

### Templates disponíveis em memory/templates/
- `planning.md` — ata de sprint planning
- `daily.md` — registro de daily (manhã + fim do dia)
- `review.md` — ata de sprint review
- `retro.md` — ata de retrospectiva
- `decision.md` — registro de decisão técnica ou de produto
- Ver também `memory/dailies/EXEMPLO-daily.md` para referência de preenchimento.
