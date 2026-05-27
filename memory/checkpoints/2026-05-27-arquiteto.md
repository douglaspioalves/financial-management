# Checkpoint Arquiteto — 2026-05-27

**Sprint:** 02
**Agente:** Arquiteto
**Status:** Sessão concluída — contrato e decisões entregues

---

## O que foi feito nesta sessão

### 1. Contrato da API — `docs/api.md`
Arquivo existente atualizado com os endpoints completos do Sprint 02:

- `GET /api/persons` — leitura das duas pessoas (seeds)
- `GET /api/categories` + filtro por `?type=`
- `POST /api/categories`
- `PUT /api/categories/{id}` — com `version` para optimistic locking
- `DELETE /api/categories/{id}` — soft delete (decisão tomada)
- `POST /api/transactions` — criação à vista (`installmentsTotal = 1` obrigatório)
- `GET /api/transactions?month=yyyy-MM` — listagem por mês
- `GET /api/transactions/{id}` — detalhe
- `PUT /api/transactions/{id}` — edição com `version`
- `DELETE /api/transactions/{id}` — remoção física (parcelados bloqueados)

Todas as mensagens de erro estão em pt-br.
Todos os campos obrigatórios, tipos e status codes documentados.
Seções de fatias futuras (3–6) preservadas com endpoints previstos.

### 2. Decisão de produto — `memory/decisions/2026-05-27-delete-category.md`
Optado por **soft delete** (Opção B) para `DELETE /api/categories/{id}`.
Motivo principal: preservar integridade do histórico financeiro sem quebrar FKs.
Impacto mapeado: migration V4, filtro no repository, comportamento do controller.

### 3. Pontos em aberto registrados
- Migration V4 para coluna `inactive` em `category` (DBA — antes do backend)
- Retornar `version` nos GETs de categorias e transações (backend)
- Status 422 vs 400 para categoria incompatível (backend decidir)

---

## Estado para retomada

### Próximos passos por agente (ordem de execução)

| Ordem | Agente | Tarefa | Pré-requisito |
|-------|--------|--------|---------------|
| 1 | DBA | Criar `V4__add_category_inactive.sql` | Nenhum |
| 2 | Backend | Entidade `Category` + `CategoryRepository` | V4 aplicada |
| 3 | Backend | `CategoryService` (CRUD com soft delete) | Category entity |
| 4 | Backend | `CategoryController` (GET, POST, PUT, DELETE) | CategoryService |
| 5 | Backend | Entidade `Transaction` + `TransactionRepository` | Category entity |
| 6 | Backend | `TransactionService` (CRUD + filtro por mês) | Transaction entity |
| 7 | Backend | `TransactionController` | TransactionService |
| 8 | Backend | Entidade `Person` + `PersonController` (GET) | Já existe no banco via seed |
| 9 | QA | Testes unitários de `CategoryService` | CategoryService |
| 10 | QA | Testes de integração dos endpoints de Transaction | TransactionController |
| 11 | Frontend | Módulo `transactions` + `TransactionService` HTTP | API de transactions pronta |
| 12 | Frontend | Tela de lançamentos (lista + formulário) | TransactionService FE |
| 13 | Revisor | Revisão de segurança e qualidade | Tudo implementado |

### Contratos acordados que Backend e Frontend devem seguir

1. **Optimistic locking explícito:** `version` vai no body do PUT (categories e transactions)
   e no response de todos os GETs. Frontend envia o `version` recebido na última leitura.
2. **Soft delete de categorias:** DELETE não verifica lançamentos; apenas marca `inactive = true`.
3. **Parcelamento bloqueado:** `installmentsTotal > 1` retorna 400 nesta fatia.
4. **Compatibilidade de tipo:** categoria EXPENSE só para transações EXPENSE; INCOME para INCOME; BOTH para ambos. Status 422 para incompatibilidade (ponto em aberto para backend confirmar).
5. **Filtro de mês obrigatório:** `GET /api/transactions` sem `?month=` retorna 400.

### Arquivos criados/modificados nesta sessão

- `docs/api.md` — atualizado com contrato completo do Sprint 02
- `memory/decisions/2026-05-27-delete-category.md` — decisão de soft delete
- `memory/checkpoints/2026-05-27-arquiteto.md` — este arquivo

### Arquivos que o DBA deve criar antes do backend começar

- `backend/src/main/resources/db/migration/V4__add_category_inactive.sql`

---

## Riscos identificados

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Backend e Frontend divergirem no formato de `version` | Média | Alto | Contratos explícitos no `docs/api.md`; revisor valida antes do merge |
| DBA esquecer o `NOT NULL DEFAULT false` na migration V4 | Baixa | Alto | Instrução explícita no `docs/api.md` e na decisão |
| Frontend enviar `installmentsTotal > 1` por engano | Baixa | Médio | Backend retorna 400 com mensagem clara |
| Lançamentos `PROPORTIONAL` sem receitas lançadas no mês (acerto indefinido) | Alta | Alto | Regra de negócio documentada no CLAUDE.md; afeta Sprint 06 — não Sprint 02 |
