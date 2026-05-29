# Checkpoint Arquiteto — 2026-05-28

**Sprint:** 03
**Agente:** Arquiteto
**Status:** Sessão concluída — planning e contrato entregues

---

## Feito nesta sessão

### 1. Contrato da API — `docs/api.md` (seção Fatia 3a)
Substituída a seção placeholder "Fatia 3 — a detalhar" pelo contrato completo:

- `GET /api/cards` — lista todos os cartões (ordenados por nome)
- `POST /api/cards` — cria cartão (validações: name, ownerPersonId, closingDay 1–31, dueDay 1–31)
- `GET /api/cards/{id}` — detalhe de cartão
- `PUT /api/cards/{id}` — edita cartão com optimistic locking (`version`)
- `DELETE /api/cards/{id}` — exclui cartão; bloqueia com 409 se houver transações vinculadas

Todos os payloads, status codes e mensagens de erro em pt-br documentados.

A seção de parcelamentos (Fatia 3b) foi separada como placeholder para depois desta fatia.

Tabela "Pontos em aberto" atualizada: itens 1–3 do Sprint 02 marcados como CONCLUÍDOS;
itens 5 e 6 adicionados para o Sprint 03.

### 2. Sprint 03 — `memory/sprints/sprint-03.md`
- Status alterado de NÃO INICIADO para EM ANDAMENTO
- Dia 1 preenchido: 2026-05-28 — planning concluído; contrato da API definido

### 3. Planning — `memory/plannings/planning-sprint-03.md`
Criado com:
- Objetivo do sprint
- Contexto técnico (schema já existente, nenhuma migration nova)
- 5 stories: S-03-00 (DBA), S-03-01 (Backend), S-03-02 (Frontend), S-03-03 (QA), S-03-04 (Revisor+DevOps)
- Detalhamento de tarefas por story com ordem de execução
- 11 casos de teste (TC-01 a TC-11) para QA
- Diagrama de paralelismo por dia
- Tabela de riscos
- DoD completo

### 4. Daily — `memory/dailies/2026-05-28.md`
Criado com seção MANHÃ para todos os agentes (Arquiteto, DBA, Backend, Frontend, QA).
Seção FIM DO DIA preenchida para Arquiteto; demais agentes com pendências para amanhã.

### 5. Decisão — `memory/decisions/2026-05-28-card-delete-policy.md`
Registrada a decisão: cartão com transações vinculadas NÃO pode ser excluído (409).
Três opções avaliadas (cascade, bloquear, soft delete). Escolha: bloquear com 409.
Justificativa: integridade do histórico financeiro; FK `fk_transaction_card` já garante
a restrição no banco, mas o service deve verificar antes para retornar mensagem amigável.

---

## Pendente

- S-03-00: DBA precisa confirmar schema e índices de `card` e `transaction(card_id)`
- S-03-01: Backend — Card entity + service + controller (branch: `feature/s03-backend`)
- S-03-02: Frontend — módulo cards + integração formulário (branch: `feature/s03-frontend`)
- S-03-03: QA — testes CardService (branch: `feature/s03-tests`)
- S-03-04: Revisor + DevOps — após demais stories concluídas

---

## Próximo passo imediato

DBA deve executar S-03-00: **o índice `transaction(card_id)` está AUSENTE em V2.**
Verificado via grep em V2__initial_schema.sql — não existe `idx_transaction_card_id`.
DBA DEVE criar `V6__add_transaction_card_index.sql` com:
```sql
CREATE INDEX idx_transaction_card_id ON transaction (card_id);
```
Essa migration é necessária pois o backend fará `transactionRepository.existsByCardId(id)`
para verificar se o cartão pode ser excluído. Sem índice, a query faz full scan na tabela
`transaction` — aceitável em desenvolvimento, problemático em produção.

Após `V6` criada e aplicada, Backend inicia S-03-01 na branch `feature/s03-backend`.

Após S-03-00, Backend inicia S-03-01 na branch `feature/s03-backend`.

---

## Contratos fechados (Backend e Frontend devem seguir)

1. Response de cartão sempre inclui `ownerPersonName` (join com `person`).
2. Response de cartão inclui `version` para que o PUT possa ser feito com optimistic locking.
3. `GET /api/cards` ordenado por `name` ASC.
4. `DELETE /api/cards/{id}` retorna 409 (não 400) quando há transações vinculadas.
5. Mensagem de erro 409 do delete: "Este cartão possui lançamentos vinculados e não pode
   ser excluído. Remova os lançamentos antes de excluir o cartão."
6. `closingDay` e `dueDay`: inteiros 1–31 inclusive; validação no backend com Bean Validation.

---

## Arquivos criados/modificados nesta sessão

- `docs/api.md` — seção Fatia 3a completa + pontos em aberto atualizados
- `memory/sprints/sprint-03.md` — status EM ANDAMENTO + Dia 1 preenchido
- `memory/plannings/planning-sprint-03.md` — planning completo
- `memory/dailies/2026-05-28.md` — daily do dia 1
- `memory/decisions/2026-05-28-card-delete-policy.md` — decisão de exclusão de cartão
- `memory/checkpoints/2026-05-28-arquiteto.md` — este arquivo
