# Contrato da API — Gestor de Gastos

> Fonte de verdade para Backend e Frontend trabalharem contra o mesmo contrato.
> Mantido pelo agente **arquiteto**. Atualize ANTES de implementar cada fatia.
> Textos de erro ao usuário em pt-br.

**Base URL (desenvolvimento):** `http://localhost:8080`

**Autenticação:** todos os endpoints (exceto os marcados como "público") exigem:
```
Authorization: Bearer <token_jwt>
```

**Formato de datas:**
- Dia específico: `"yyyy-MM-dd"` — ex.: `"2026-05-27"`
- Mês de referência (query param): `"yyyy-MM"` — ex.: `"2026-05"`
- Timestamps em respostas: ISO 8601 `"yyyy-MM-dd'T'HH:mm:ss"` — ex.: `"2026-05-27T10:00:00"`

**Valores monetários:** `number` com exatamente 2 casas decimais (ex.: `1500.00`).
Backend usa `BigDecimal`; nunca `float`/`double`.

**Envelope de erro padrão** (todos os erros seguem este formato):
```json
{
  "timestamp": "2026-05-27T10:00:00",
  "status": 400,
  "error": "Requisição inválida",
  "message": "mensagem amigável em pt-br",
  "path": "/api/transactions"
}
```

Erros de validação com múltiplos campos retornam `errors` adicional:
```json
{
  "timestamp": "2026-05-27T10:00:00",
  "status": 400,
  "error": "Requisição inválida",
  "message": "Campos inválidos na requisição.",
  "path": "/api/transactions",
  "errors": {
    "amount": "O valor deve ser maior que zero.",
    "categoryId": "Categoria é obrigatória."
  }
}
```

---

## Fatia 0 — Esqueleto (implementado no Sprint 01)

### GET /api/health — público

Verifica se o backend está respondendo.

**Response 200:**
```json
{ "status": "ok" }
```

---

## Fatia 1 — Autenticação (implementado no Sprint 01)

### POST /api/auth/register — público

Cadastra novo usuário.

**Request:**
```json
{
  "name":     "string (2–100 chars, obrigatório)",
  "email":    "string (e-mail válido, obrigatório)",
  "password": "string (mínimo 6 chars, obrigatório)"
}
```

**Response 201:**
```json
{
  "token": "string (JWT)",
  "type":  "Bearer",
  "name":  "string",
  "email": "string"
}
```

| Status | Situação |
|--------|----------|
| 201 | Cadastro realizado com sucesso |
| 400 | Campos inválidos (nome vazio, e-mail inválido, senha curta) |
| 409 | E-mail já cadastrado — "Este e-mail já está em uso." |

---

### POST /api/auth/login — público

Autentica usuário e retorna JWT.

**Request:**
```json
{
  "email":    "string (obrigatório)",
  "password": "string (obrigatório)"
}
```

**Response 200:**
```json
{
  "token": "string (JWT)",
  "type":  "Bearer",
  "name":  "string",
  "email": "string"
}
```

| Status | Situação |
|--------|----------|
| 200 | Login realizado com sucesso |
| 400 | Campos obrigatórios ausentes |
| 401 | E-mail ou senha incorretos — "E-mail ou senha incorretos." |

---

## Fatia 2 — Categorias, Lançamentos e Persons (Sprint 02)

---

### GET /api/persons

Lista as duas pessoas cadastradas no sistema (seeds; somente leitura nesta fatia).
Usado para popular dropdowns de "quem pagou" no formulário de lançamentos.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
[
  {
    "id":    "uuid",
    "name":  "string",
    "color": "string (hex, ex.: #4a7fc4)"
  },
  {
    "id":    "uuid",
    "name":  "string",
    "color": "string (hex, ex.: #e8927c)"
  }
]
```

| Status | Situação |
|--------|----------|
| 200 | Lista retornada (sempre 2 registros com os seeds atuais) |
| 401 | Token ausente ou inválido |

---

### GET /api/categories

Lista todas as categorias ativas. Filtro opcional por tipo.
Categorias desativadas (soft delete) são excluídas desta listagem.

**Headers:** `Authorization: Bearer <token>`

**Query params:**

| Param  | Tipo                        | Obrigatório | Descrição |
|--------|-----------------------------|-------------|-----------|
| `type` | `EXPENSE \| INCOME \| BOTH` | não         | Filtra pelo tipo da categoria |

**Response 200:**
```json
[
  {
    "id":    "uuid",
    "name":  "string",
    "type":  "EXPENSE | INCOME | BOTH",
    "color": "string (hex)"
  }
]
```

| Status | Situação |
|--------|----------|
| 200 | Lista retornada (pode ser vazia `[]`) |
| 400 | Valor de `type` inválido — "Tipo de categoria inválido. Use EXPENSE, INCOME ou BOTH." |
| 401 | Token ausente ou inválido |

---

### POST /api/categories

Cria uma categoria personalizada.

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "name":  "string (1–100 chars, obrigatório)",
  "type":  "EXPENSE | INCOME | BOTH (obrigatório)",
  "color": "string (hex #RRGGBB, obrigatório)"
}
```

**Validações:**
- `name`: não pode ser vazio; máximo 100 caracteres
- `type`: deve ser exatamente `EXPENSE`, `INCOME` ou `BOTH`
- `color`: formato hex de 7 chars: `#` + 6 dígitos hexadecimais (ex.: `#4a7fc4`)

**Response 201:**
```json
{
  "id":    "uuid",
  "name":  "string",
  "type":  "EXPENSE | INCOME | BOTH",
  "color": "string (hex)"
}
```

| Status | Situação |
|--------|----------|
| 201 | Categoria criada com sucesso |
| 400 | Campo inválido — mensagens específicas (ver validações acima) |
| 401 | Token ausente ou inválido |

**Mensagens de erro por campo:**
- `name` vazio: `"Nome da categoria é obrigatório."`
- `name` > 100 chars: `"Nome da categoria deve ter no máximo 100 caracteres."`
- `type` inválido: `"Tipo inválido. Use EXPENSE, INCOME ou BOTH."`
- `color` inválido: `"Cor deve estar no formato hexadecimal #RRGGBB."`

---

### PUT /api/categories/{id}

Edita nome e/ou cor de uma categoria existente.
O campo `type` não pode ser alterado (mudaria a semântica de lançamentos já criados).

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID da categoria

**Request:**
```json
{
  "name":    "string (1–100 chars, obrigatório)",
  "color":   "string (hex #RRGGBB, obrigatório)",
  "version": "integer (obrigatório — valor retornado pela última leitura)"
}
```

O campo `version` implementa optimistic locking: o frontend deve sempre enviar o valor
que recebeu na última leitura. Se outro usuário editou o registro entretanto, o backend
retorna 409.

**Response 200:**
```json
{
  "id":      "uuid",
  "name":    "string",
  "type":    "EXPENSE | INCOME | BOTH",
  "color":   "string (hex)",
  "version": "integer (versão atualizada)"
}
```

| Status | Situação |
|--------|----------|
| 200 | Categoria atualizada com sucesso |
| 400 | Campo inválido — mensagens específicas |
| 401 | Token ausente ou inválido |
| 404 | Categoria não encontrada — "Categoria não encontrada." |
| 409 | Conflito de versão — "O registro foi alterado por outro usuário. Recarregue e tente novamente." |

> Nota: o `GET /api/categories` deve incluir o campo `version` na resposta para que o
> frontend possa enviar no PUT. Acrescentar `version: integer` no response de GET também.

---

### DELETE /api/categories/{id}

Desativa uma categoria (soft delete). A categoria some dos dropdowns mas os lançamentos
existentes mantêm a referência intacta.
Ver decisão completa em `memory/decisions/2026-05-27-delete-category.md`.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID da categoria

**Response 204:** sem body

| Status | Situação |
|--------|----------|
| 204 | Categoria desativada com sucesso |
| 401 | Token ausente ou inválido |
| 404 | Categoria não encontrada — "Categoria não encontrada." |

> Requisito de migration: o agente DBA deve criar `V4__add_category_inactive.sql`
> com `ALTER TABLE category ADD COLUMN inactive BOOLEAN NOT NULL DEFAULT false;`
> antes do backend implementar `CategoryService`.

---

### POST /api/transactions

Cria um lançamento financeiro. Nesta fatia (Sprint 02), apenas lançamentos à vista
(`installmentsTotal = 1`) são suportados.

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "type":               "EXPENSE | INCOME (obrigatório)",
  "amount":             "number > 0, máx. 2 casas decimais (obrigatório)",
  "date":               "string yyyy-MM-dd (obrigatório)",
  "description":        "string (opcional, máx. 255 chars)",
  "categoryId":         "uuid (obrigatório)",
  "paidByPersonId":     "uuid (obrigatório)",
  "paymentMethod":      "CASH | DEBIT | CREDIT | PIX | TRANSFER (obrigatório)",
  "cardId":             "uuid (obrigatório se paymentMethod=CREDIT; deve ser null caso contrário)",
  "splitRule":          "PERSON_A | PERSON_B | FIFTY_FIFTY | PROPORTIONAL (obrigatório)",
  "installmentsTotal":  "integer (opcional; se enviado, deve ser 1)"
}
```

**Validações de negócio:**
1. `amount` > 0
2. `date` não pode ser nula
3. `categoryId` deve existir e estar ativa (`inactive = false`)
4. `paidByPersonId` deve existir
5. `paymentMethod = CREDIT` exige `cardId` preenchido; qualquer outro método exige `cardId = null`
6. `installmentsTotal` deve ser 1 (parcelamento reservado para Sprint 03)
7. Compatibilidade de tipo: categoria tipo `EXPENSE` ou `BOTH` para lançamentos `EXPENSE`;
   categoria tipo `INCOME` ou `BOTH` para lançamentos `INCOME`

**Response 201:**
```json
{
  "id":          "uuid",
  "type":        "EXPENSE | INCOME",
  "amount":      "number",
  "date":        "string yyyy-MM-dd",
  "description": "string | null",
  "category": {
    "id":    "uuid",
    "name":  "string",
    "type":  "EXPENSE | INCOME | BOTH",
    "color": "string"
  },
  "paidByPerson": {
    "id":    "uuid",
    "name":  "string",
    "color": "string"
  },
  "paymentMethod":     "string",
  "cardId":            "uuid | null",
  "splitRule":         "string",
  "installmentsTotal": 1,
  "createdAt":         "string ISO 8601",
  "version":           "integer"
}
```

| Status | Situação |
|--------|----------|
| 201 | Lançamento criado com sucesso |
| 400 | Validação falhou — mensagem específica por campo |
| 400 | `installmentsTotal > 1` enviado — "Parcelamento não está disponível nesta versão. Use installmentsTotal = 1." |
| 400 | `paymentMethod = CREDIT` sem `cardId` — "Pagamento com cartão de crédito exige a seleção de um cartão." |
| 400 | `cardId` preenchido sem `paymentMethod = CREDIT` — "Cartão só pode ser informado para pagamentos com cartão de crédito." |
| 401 | Token ausente ou inválido |
| 404 | `categoryId` não encontrado ou inativo — "Categoria não encontrada." |
| 404 | `paidByPersonId` não encontrado — "Participante não encontrado." |
| 422 | Categoria incompatível com o tipo do lançamento — "A categoria selecionada não é compatível com o tipo do lançamento." |

---

### GET /api/transactions

Lista lançamentos de um mês. Retorna todos os lançamentos cuja `date` está no intervalo
`[primeiro dia do mês, último dia do mês]`.

**Headers:** `Authorization: Bearer <token>`

**Query params:**

| Param   | Tipo      | Obrigatório | Descrição |
|---------|-----------|-------------|-----------|
| `month` | `yyyy-MM` | sim         | Mês a listar (ex.: `2026-05`) |

**Ordenação:** `date` ascendente; em caso de empate, `created_at` ascendente.

**Response 200:**
```json
[
  {
    "id":          "uuid",
    "type":        "EXPENSE | INCOME",
    "amount":      "number",
    "date":        "string yyyy-MM-dd",
    "description": "string | null",
    "category": {
      "id":    "uuid",
      "name":  "string",
      "type":  "EXPENSE | INCOME | BOTH",
      "color": "string"
    },
    "paidByPerson": {
      "id":    "uuid",
      "name":  "string",
      "color": "string"
    },
    "paymentMethod":     "string",
    "cardId":            "uuid | null",
    "splitRule":         "string",
    "installmentsTotal": "integer",
    "createdAt":         "string ISO 8601",
    "version":           "integer"
  }
]
```

| Status | Situação |
|--------|----------|
| 200 | Lista retornada (pode ser `[]`) |
| 400 | `month` ausente — "Parâmetro 'month' é obrigatório no formato yyyy-MM." |
| 400 | `month` em formato inválido — "Formato de mês inválido. Use yyyy-MM (ex.: 2026-05)." |
| 401 | Token ausente ou inválido |

---

### GET /api/transactions/{id}

Retorna o detalhe de um único lançamento.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do lançamento

**Response 200:** mesmo formato de item do `GET /api/transactions`.

| Status | Situação |
|--------|----------|
| 200 | Lançamento encontrado |
| 401 | Token ausente ou inválido |
| 404 | Lançamento não encontrado — "Lançamento não encontrado." |

---

### PUT /api/transactions/{id}

Edita um lançamento existente. Todos os campos editáveis devem ser enviados
(substituição completa; sem suporte a PATCH parcial nesta fatia).

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do lançamento

**Request:**
```json
{
  "type":               "EXPENSE | INCOME (obrigatório)",
  "amount":             "number > 0 (obrigatório)",
  "date":               "string yyyy-MM-dd (obrigatório)",
  "description":        "string (opcional)",
  "categoryId":         "uuid (obrigatório)",
  "paidByPersonId":     "uuid (obrigatório)",
  "paymentMethod":      "CASH | DEBIT | CREDIT | PIX | TRANSFER (obrigatório)",
  "cardId":             "uuid | null (regra igual ao POST)",
  "splitRule":          "PERSON_A | PERSON_B | FIFTY_FIFTY | PROPORTIONAL (obrigatório)",
  "version":            "integer (obrigatório — optimistic locking)"
}
```

**Response 200:** mesmo formato da resposta do `POST /api/transactions`.

| Status | Situação |
|--------|----------|
| 200 | Lançamento atualizado com sucesso |
| 400 | Validação falhou — mensagens específicas (idem ao POST) |
| 401 | Token ausente ou inválido |
| 404 | Lançamento não encontrado — "Lançamento não encontrado." |
| 404 | `categoryId` ou `paidByPersonId` não encontrado |
| 409 | Conflito de versão — "O registro foi alterado por outro usuário. Recarregue e tente novamente." |
| 422 | Categoria incompatível com o tipo do lançamento |

---

### DELETE /api/transactions/{id}

Remove permanentemente um lançamento.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do lançamento

**Comportamento:**
- Lançamentos à vista (`installmentsTotal = 1`): remoção física.
- Lançamentos parcelados (`installmentsTotal > 1`): bloqueados nesta fatia; retorna 400.
  (O comportamento correto — remover transaction + cascade installments — vem no Sprint 03.)

**Response 204:** sem body

| Status | Situação |
|--------|----------|
| 204 | Lançamento removido com sucesso |
| 400 | Lançamento parcelado — "Exclusão de lançamentos parcelados não está disponível nesta versão." |
| 401 | Token ausente ou inválido |
| 404 | Lançamento não encontrado — "Lançamento não encontrado." |

---

## Fatia 3 — Cartões e Parcelamentos (Sprint 03 — a detalhar)

Endpoints previstos:
- `GET /api/cards`
- `POST /api/cards`
- `PUT /api/cards/{id}`
- `DELETE /api/cards/{id}`
- `POST /api/transactions` com `installmentsTotal > 1` (habilitado)
- `GET /api/transactions/{id}/installments`

---

## Fatia 4 — Dashboard (Sprint 04 — a detalhar)

Endpoints previstos:
- `GET /api/dashboard?month=yyyy-MM`

---

## Fatia 5 — Orçamento e Recorrência (Sprint 05 — a detalhar)

Endpoints previstos:
- `GET /api/budgets?month=yyyy-MM`
- `POST /api/budgets`
- `PUT /api/budgets/{id}`
- `DELETE /api/budgets/{id}`
- `GET /api/recurring-rules`
- `POST /api/recurring-rules`
- `PUT /api/recurring-rules/{id}`
- `DELETE /api/recurring-rules/{id}`

---

## Fatia 6 — Acerto de Contas (Sprint 06 — a detalhar)

Endpoints previstos:
- `GET /api/settlement?month=yyyy-MM`

---

## Pontos em aberto

| # | Ponto | Impacto | Ação necessária |
|---|-------|---------|-----------------|
| 1 | Migration V4 para coluna `inactive` em `category` | DBA deve criar antes do backend | Agente DBA — início do Sprint 02 |
| 2 | Retornar `version` nos GETs de categorias | Frontend precisa para enviar no PUT | Backend incluir no DTO de resposta |
| 3 | Status 422 vs 400 para categoria incompatível | Consistência de status codes | Backend decidir e manter consistente |
| 4 | Paginação de `/api/transactions` | Não necessária agora (base pequena) | Avaliar se o volume crescer |
