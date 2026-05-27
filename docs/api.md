# Contrato da API

> Mantido pelo agente **arquiteto**. Backend e frontend trabalham contra este contrato.
> Atualize ANTES de implementar cada fatia. Textos de erro ao usuário em pt-br.

## Convenções gerais

- Base: `/api`
- Autenticação: `Authorization: Bearer <jwt>` em todos os endpoints, exceto
  `POST /api/auth/register` e `POST /api/auth/login`.
- Formato: JSON. Datas em ISO 8601 (`yyyy-MM-dd`). Dinheiro como número com 2 casas.
- Erros: `{ "message": "mensagem em pt-br", "errors": { "campo": "detalhe" } }`.

## Health (Fatia 0)

### GET /api/health
Resposta 200:
```json
{ "status": "ok" }
```

---

## A definir por fatia

> O arquiteto preenche abaixo, fatia a fatia, antes do backend/frontend começarem.

### Fatia 1 — Autenticação
- `POST /api/auth/register`
- `POST /api/auth/login`
- (detalhar payloads quando a fatia iniciar)

### Fatia 2 — Lançamentos e categorias
- `GET/POST/PUT/DELETE /api/transactions`
- `GET/POST /api/categories`
- ...

### Fatia 3 — Cartões e parcelas
- `GET/POST /api/cards`
- (a geração de parcelas acontece ao criar uma transaction de crédito parcelada)
- ...

### Fatia 4 — Dashboard
- `GET /api/dashboard?month=yyyy-MM`
- ...

### Fatia 5 — Orçamento e recorrência
- `GET/POST /api/budgets`
- `GET/POST /api/recurring-rules`
- ...

### Fatia 6 — Acerto de contas
- `GET /api/settlement?month=yyyy-MM`
- ...
