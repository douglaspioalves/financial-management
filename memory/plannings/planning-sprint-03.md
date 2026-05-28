# Planning — Sprint 03
**Data:** 2026-05-28
**Sprint:** 03 de 07
**Período:** 2026-05-28 → 2026-06-10 (10 dias úteis)
**Fatia do roadmap:** 3a — Cartões de Crédito (CRUD)
**Conduzido por:** Scrum Master / Arquiteto

---

## Objetivo do sprint

Ao fim do sprint, o usuário poderá cadastrar, visualizar, editar e excluir
cartões de crédito na interface; e o formulário de lançamento exibirá o
seletor de cartão quando o método de pagamento for "Crédito".

---

## Contexto técnico

- **Schema:** tabela `card` já existe em V2__initial_schema.sql com todos os campos
  e constraints necessários (`closing_day`, `due_day` CHECK 1–31, FK para `person`,
  `version` para optimistic locking).
- **Migration necessária:** índice `transaction(card_id)` está **ausente** em V2.
  DBA deve criar `V6__add_transaction_card_index.sql` na branch `feature/s03-schema`.
- **Tabela `transaction`:** coluna `card_id` (FK nullable) já existe, com
  constraint que obriga `card_id` quando `payment_method = 'CREDIT'`.
- **Entidades existentes:** User, Person, Category, Transaction + seus repositórios
  e serviços estão implementados e mergeados em master.
- **Frontend:** módulo `transactions` existe; campo `card_id` ainda não integrado.

---

## Stories selecionadas

| ID | Story | Papel | Pontos | Depende de |
|---|---|---|---|---|
| S-03-00 | Validar schema e índices da tabela `card` | DBA | 1 | — |
| S-03-01 | API de cartões (backend completo) | Backend | 3 | S-03-00 |
| S-03-02 | Tela de cartões + integração no formulário | Frontend | 3 | S-03-01 |
| S-03-03 | Testes de CardService (unitários) | QA | 2 | S-03-01 |
| S-03-04 | Revisão e merge | Revisor + DevOps | 1 | S-03-02, S-03-03 |

**Total de pontos:** 10

---

## Detalhamento das stories

### S-03-00 · Validar schema (DBA)
**Branch:** `feature/s03-schema` (necessária — ver resultado da análise abaixo)

Responsabilidades:
- Confirmar que `V2__initial_schema.sql` tem `card` com todos os campos corretos. (**OK**)
- Confirmar índices em `transaction(card_id)` — **AUSENTE**: não existe `idx_transaction_card_id` em V2.
  DBA DEVE criar `V6__add_transaction_card_index.sql`.
- Confirmar que constraint `chk_transaction_card_credit` está ativa. (**OK** — verificado em V2)
- Documentar resultado em `memory/sprints/sprint-03.md` (linha do dia 1).

Resultado esperado: migration `V6__add_transaction_card_index.sql` com:
```sql
-- Índice para busca de transações por cartão
-- Necessário para CardService.delete verificar transações vinculadas eficientemente
CREATE INDEX idx_transaction_card_id ON transaction (card_id);
```

---

### S-03-01 · API de cartões (Backend)
**Branch:** `feature/s03-backend`

Tarefas em ordem:

| # | Tarefa | Detalhe |
|---|--------|---------|
| 1 | `Card.java` (entidade JPA) | `@Entity @Table("card")`, campos: id (UUID), ownerPerson (ManyToOne), name, closingDay, dueDay, version (`@Version`) |
| 2 | `CardRepository.java` | `JpaRepository<Card, UUID>` + `existsByIdAndTransactionsIsNotEmpty` (ou query nativa para checar FK em `transaction`) |
| 3 | `CardRequest.java` (DTO entrada) | name, ownerPersonId, closingDay (1–31), dueDay (1–31) — Bean Validation pt-br |
| 4 | `CardResponse.java` (DTO saída) | id, name, ownerPersonId, ownerPersonName, closingDay, dueDay, version |
| 5 | `CardService.java` | findAll, findById, create, update, delete (com check de transações vinculadas → 409) |
| 6 | `CardController.java` | GET /api/cards, POST /api/cards, GET /api/cards/{id}, PUT /api/cards/{id}, DELETE /api/cards/{id} |
| 7 | Tratamento de erros | 404 cartão não encontrado, 404 person não encontrada, 409 conflito de versão, 409 cartão com transações |

Regras de implementação:
- `@Version` na entidade `Card` para optimistic locking (já existe na coluna `version`).
- No `delete`: verificar `transactionRepository.existsByCardId(cardId)` antes de excluir.
  Se existir → lançar exceção de negócio mapeada para 409.
- `closingDay` e `dueDay`: validação com `@Min(1) @Max(31)`, mensagem em pt-br.
- Resposta do GET /api/cards ordenada por `name` ASC.

---

### S-03-02 · Tela de cartões + integração no formulário (Frontend)
**Branch:** `feature/s03-frontend`

Tarefas em ordem (S-03-01 deve estar mergeada ou contratos mockados):

| # | Tarefa | Detalhe |
|---|--------|---------|
| 1 | `CardService` (HTTP) | Serviço Angular — getAll(), getById(id), create(req), update(id, req), delete(id) |
| 2 | Módulo `cards` | Lazy-loaded; rota `/cards` protegida por `authGuard` |
| 3 | `CardsListComponent` | Tabela/lista de cartões; botão "Novo Cartão"; ações editar/excluir |
| 4 | `CardFormComponent` | Formulário reativo; campos: nome, titular (dropdown de persons), dia fechamento, dia vencimento; validações inline em pt-br |
| 5 | Integrar seleção no formulário de lançamento | Em `TransactionFormComponent`: quando `paymentMethod = CREDIT`, exibir `<mat-select>` populado por `CardService.getAll()`; ocultar/limpar quando outro método |
| 6 | Navegação | Adicionar link "Cartões" no menu lateral/navbar |

Design system:
- Seguir tokens do design system aprovado (azul #4a7fc4, tipografia Fraunces + Plus Jakarta Sans).
- Chips de cores por titular (usar `ownerPersonName` + cor da person).
- Modo claro e escuro devem funcionar.

---

### S-03-03 · Testes de CardService (QA)
**Branch:** `feature/s03-tests`

Casos de teste obrigatórios:

| Caso | Cenário | Resultado esperado |
|------|---------|-------------------|
| TC-01 | Criar cartão com dados válidos | Cartão salvo; response 201 com todos os campos |
| TC-02 | Criar com `closingDay = 0` | 400 com mensagem "Dia de fechamento deve estar entre 1 e 31." |
| TC-03 | Criar com `closingDay = 32` | 400 com mensagem "Dia de fechamento deve estar entre 1 e 31." |
| TC-04 | Criar com `dueDay = 0` | 400 com mensagem "Dia de vencimento deve estar entre 1 e 31." |
| TC-05 | Criar com `ownerPersonId` inexistente | 404 com mensagem "Participante não encontrado." |
| TC-06 | Criar com `name` vazio | 400 com mensagem "Nome do cartão é obrigatório." |
| TC-07 | Buscar cartão inexistente | 404 com mensagem "Cartão não encontrado." |
| TC-08 | Editar com `version` desatualizada | 409 com mensagem de conflito |
| TC-09 | Excluir cartão sem transações vinculadas | 204; cartão não existe mais |
| TC-10 | Excluir cartão com transações vinculadas | 409 com mensagem "Este cartão possui lançamentos vinculados..." |
| TC-11 | Listar cartões | 200 com array; ordenado por nome |

---

### S-03-04 · Revisão e merge (Revisor + DevOps)
**Branch:** — (age sobre as branches abertas)

Checklist do revisor:
- [ ] Nenhuma entidade JPA exposta diretamente em controller (somente DTOs)
- [ ] Bean Validation com mensagens em pt-br
- [ ] `@Version` presente na entidade Card
- [ ] `delete` verifica transações vinculadas antes de excluir
- [ ] Sem hardcode de IDs ou strings fora de constante/enum
- [ ] Frontend: campo cartão visível **apenas** quando `paymentMethod = CREDIT`
- [ ] Frontend: formulário não permite submissão com cartão vazio quando crédito
- [ ] Testes passando: `./mvnw test`

Checklist DevOps:
- [ ] Branches mergeadas para master via `git merge --no-ff`
- [ ] `docker compose up --build` sem erro
- [ ] Tag `sprint-03` criada em master

---

## Ordem de execução e paralelismo

```
DIA 1 (hoje — paralelo):
  ├── DBA:      S-03-00 — validar schema (1h)
  └── Arquiteto: planning + contrato API em docs/api.md (concluído)

DIA 1–4 (após validação DBA):
  └── Backend:  S-03-01 — Card entity + service + controller (branch: feature/s03-backend)

DIA 3–7 (paralelo após API em rascunho ou mergeada):
  ├── Frontend: S-03-02 — módulo cards + integração formulário (branch: feature/s03-frontend)
  └── QA:       S-03-03 — testes CardService (branch: feature/s03-tests)

DIA 8–9:
  └── Revisor:  S-03-04 — revisão de todos os branches abertos

DIA 9–10:
  └── DevOps:   merges + docker compose up --build + tag sprint-03
```

**Dependências críticas:**
- S-03-01 depende de S-03-00 (confirmação do schema)
- S-03-02 pode iniciar com mock se S-03-01 ainda não estiver mergeada (contrato em docs/api.md)
- S-03-03 requer S-03-01 para testar o service real
- S-03-04 requer S-03-01, S-03-02 e S-03-03 concluídos

---

## Riscos e pontos de atenção

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Verificação de transações vinculadas no delete (query correta) | Média | Alto | QA tem TC-09 e TC-10; DBA confirma FK existente |
| Optimistic locking: `@Version` em Card gera conflito na atualização do JPA | Baixa | Médio | Seguir mesmo padrão de Category/Transaction já implementados |
| Frontend exibe seletor de cartão fora do contexto CREDIT | Média | Médio | Lógica de show/hide com reactive forms `valueChanges`; QA valida no browser |
| `closingDay`/`dueDay` = 31 em meses com 28–30 dias (lógica de parcelas) | Alta | Alto | Afeta Sprint 03b (parcelamentos) — não esta fatia; registrar como risco futuro |
| Remoção de cartão em produção apagaria histórico se não houvesse a verificação | Alta | Alto | Decisão tomada: bloquear com 409; ver memory/decisions/2026-05-28-card-delete-policy.md |

---

## Definição de pronto (DoD) do sprint

- [ ] S-03-00: DBA confirma schema e índices (ou cria migration)
- [ ] S-03-01: CRUD de cartões funcionando; validações de dia corretas; testes unitários passando
- [ ] S-03-02: Tela de cartões funcional; seleção integrada no formulário de lançamento; campo oculto quando não é crédito
- [ ] S-03-03: Todos os 11 casos de teste (TC-01 a TC-11) passando
- [ ] S-03-04: Revisão sem itens bloqueantes; `./mvnw test` BUILD SUCCESS; `docker compose up --build` sem erro
- [ ] Review registrada em `memory/reviews/review-sprint-03.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-03.md`
- [ ] Learnings atualizados em `memory/learnings/`
- [ ] Tag `sprint-03` criada em master
