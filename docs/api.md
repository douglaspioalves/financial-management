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

## Fatia 3a — Cartões (Sprint 03)

> Schema já existente em V2__initial_schema.sql. Nenhuma migration nova é necessária
> para esta fatia. O DBA deve apenas validar índices e constraints existentes.

---

### GET /api/cards

Lista todos os cartões cadastrados, ordenados por nome.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
[
  {
    "id":              "uuid",
    "name":            "string",
    "ownerPersonId":   "uuid",
    "ownerPersonName": "string",
    "closingDay":      "integer (1–31)",
    "dueDay":          "integer (1–31)",
    "version":         "integer"
  }
]
```

| Status | Situação |
|--------|----------|
| 200 | Lista retornada (pode ser `[]`) |
| 401 | Token ausente ou inválido |

---

### POST /api/cards

Cria um novo cartão de crédito.

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "name":          "string (1–100 chars, obrigatório)",
  "ownerPersonId": "uuid (obrigatório)",
  "closingDay":    "integer 1–31 (obrigatório)",
  "dueDay":        "integer 1–31 (obrigatório)"
}
```

**Validações:**
- `name`: não pode ser vazio; máximo 100 caracteres
- `ownerPersonId`: deve referenciar uma `Person` existente
- `closingDay`: inteiro entre 1 e 31 (inclusive)
- `dueDay`: inteiro entre 1 e 31 (inclusive)

**Response 201:**
```json
{
  "id":              "uuid",
  "name":            "string",
  "ownerPersonId":   "uuid",
  "ownerPersonName": "string",
  "closingDay":      "integer",
  "dueDay":          "integer",
  "version":         0
}
```

| Status | Situação |
|--------|----------|
| 201 | Cartão criado com sucesso |
| 400 | Campo inválido — mensagens específicas por campo |
| 401 | Token ausente ou inválido |
| 404 | `ownerPersonId` não encontrado — "Participante não encontrado." |

**Mensagens de erro por campo:**
- `name` vazio: `"Nome do cartão é obrigatório."`
- `name` > 100 chars: `"Nome do cartão deve ter no máximo 100 caracteres."`
- `ownerPersonId` nulo: `"Titular do cartão é obrigatório."`
- `closingDay` nulo: `"Dia de fechamento é obrigatório."`
- `closingDay` fora de 1–31: `"Dia de fechamento deve estar entre 1 e 31."`
- `dueDay` nulo: `"Dia de vencimento é obrigatório."`
- `dueDay` fora de 1–31: `"Dia de vencimento deve estar entre 1 e 31."`

---

### GET /api/cards/{id}

Retorna o detalhe de um cartão específico.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do cartão

**Response 200:** mesmo formato de item do `GET /api/cards`.

| Status | Situação |
|--------|----------|
| 200 | Cartão encontrado |
| 401 | Token ausente ou inválido |
| 404 | Cartão não encontrado — "Cartão não encontrado." |

---

### PUT /api/cards/{id}

Edita um cartão existente. Todos os campos editáveis devem ser enviados.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do cartão

**Request:**
```json
{
  "name":          "string (1–100 chars, obrigatório)",
  "ownerPersonId": "uuid (obrigatório)",
  "closingDay":    "integer 1–31 (obrigatório)",
  "dueDay":        "integer 1–31 (obrigatório)",
  "version":       "integer (obrigatório — optimistic locking)"
}
```

O campo `version` implementa optimistic locking. O frontend deve enviar o valor
recebido na última leitura. Se outro usuário editou o registro entretanto, retorna 409.

**Response 200:**
```json
{
  "id":              "uuid",
  "name":            "string",
  "ownerPersonId":   "uuid",
  "ownerPersonName": "string",
  "closingDay":      "integer",
  "dueDay":          "integer",
  "version":         "integer (versão atualizada)"
}
```

| Status | Situação |
|--------|----------|
| 200 | Cartão atualizado com sucesso |
| 400 | Campo inválido — mensagens específicas (idem ao POST) |
| 401 | Token ausente ou inválido |
| 404 | Cartão não encontrado — "Cartão não encontrado." |
| 404 | `ownerPersonId` não encontrado — "Participante não encontrado." |
| 409 | Conflito de versão — "O registro foi alterado por outro usuário. Recarregue e tente novamente." |

---

### DELETE /api/cards/{id}

Remove permanentemente um cartão de crédito.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do cartão

**Comportamento:**
- Se o cartão possuir transações vinculadas (`card_id` em qualquer `transaction`),
  a exclusão é **bloqueada** com status 409.
- Se não houver transações vinculadas, o cartão é removido fisicamente.

> Decisão de arquitetura: ver `memory/decisions/2026-05-28-card-delete-policy.md`.

**Response 204:** sem body

| Status | Situação |
|--------|----------|
| 204 | Cartão removido com sucesso |
| 401 | Token ausente ou inválido |
| 404 | Cartão não encontrado — "Cartão não encontrado." |
| 409 | Cartão possui lançamentos vinculados e não pode ser excluído — "Este cartão possui lançamentos vinculados e não pode ser excluído. Remova os lançamentos antes de excluir o cartão." |

---

## Fatia 3b — Parcelamentos (Sprint 04)

> Schema já existente em V2__initial_schema.sql: tabela `installment` com id,
> transaction_id (FK → transaction, ON DELETE CASCADE), number (int), amount (NUMERIC 12,2),
> reference_month (DATE, sempre dia 1). Índices em `reference_month` e `transaction_id`
> já criados. **Nenhuma migration nova é necessária para esta fatia.**
>
> Algoritmo canônico de cálculo do `reference_month`:
> ver `memory/decisions/2026-05-28-installment-reference-month-algorithm.md`.

---

### POST /api/transactions (atualizado — parcelamento habilitado)

Mesmo endpoint da Fatia 2, agora com suporte a `installmentsTotal > 1`.

Quando `installmentsTotal >= 2`:
- `paymentMethod` deve ser `CREDIT` (obrigatório)
- `cardId` deve estar preenchido com um cartão existente (obrigatório)
- O backend cria a `Transaction` e gera automaticamente N registros em `Installment`
- Cada `Installment` tem `reference_month` calculado conforme o algoritmo do `closingDay` do cartão
- A última parcela absorve a diferença de centavos (arredondamento DOWN nas demais)

**Validações adicionais para parcelamento:**

| Regra | Mensagem de erro |
|-------|-----------------|
| `installmentsTotal >= 2` sem `paymentMethod = CREDIT` | "Compras parceladas só podem ser feitas com cartão de crédito." |
| `installmentsTotal >= 2` sem `cardId` | "Pagamento parcelado exige a seleção de um cartão de crédito." |
| `installmentsTotal` < 1 | "O número de parcelas deve ser pelo menos 1." |
| `installmentsTotal` > 48 | "O número máximo de parcelas é 48." |

**Response 201:** mesmo formato de antes, com `installmentsTotal` refletindo o valor enviado.

**Comportamento de edição e exclusão de lançamentos parcelados:**
- `PUT /api/transactions/{id}`: **bloqueado** para lançamentos com `installmentsTotal > 1`.
  Retorna 422 com mensagem: "Lançamentos parcelados não podem ser editados individualmente.
  Exclua e recrie o lançamento se necessário."
- `DELETE /api/transactions/{id}`: **permitido** e remove a `Transaction` + todas as
  `Installment` filhas (ON DELETE CASCADE no banco). Retorna 204.

| Status adicional | Situação |
|-----------------|----------|
| 201 | Lançamento parcelado criado; N installments geradas |
| 400 | `installmentsTotal` fora do intervalo 1–48 |
| 400 | Parcelado sem `paymentMethod = CREDIT` |
| 400 | Parcelado sem `cardId` |

---

### GET /api/transactions/{id}/installments

Lista todas as parcelas de um lançamento, ordenadas por `number` ascendente.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do lançamento (Transaction)

**Response 200:**
```json
[
  {
    "id":             "uuid",
    "number":         1,
    "amount":         500.00,
    "referenceMonth": "2026-05-01"
  },
  {
    "id":             "uuid",
    "number":         2,
    "amount":         500.00,
    "referenceMonth": "2026-06-01"
  },
  {
    "id":             "uuid",
    "number":         3,
    "amount":         500.17,
    "referenceMonth": "2026-07-01"
  }
]
```

Notas sobre o response:
- `referenceMonth` é sempre o primeiro dia do mês (`yyyy-MM-01`).
- Para lançamentos à vista (`installmentsTotal = 1`), retorna lista vazia `[]`.
- A soma de todos os `amount` é sempre igual ao `amount` da `Transaction` pai.

| Status | Situação |
|--------|----------|
| 200 | Lista retornada (vazia se lançamento à vista) |
| 401 | Token ausente ou inválido |
| 404 | Lançamento não encontrado — "Lançamento não encontrado." |

---

### Contrato fechado — regras que Backend e Frontend devem seguir

1. `TransactionResponse` inclui `installmentsTotal` em todos os GETs (já incluía).
2. Frontend deve exibir badge "N/M" (ex.: "1/3", "2/3", "3/3") na lista de lançamentos
   parcelados, chamando `GET /api/transactions/{id}/installments` apenas ao expandir o item.
3. Lançamentos parcelados NÃO exibem botão "Editar" na lista — apenas "Excluir" (que
   remove a transaction e todas as parcelas via cascade).
4. O campo `cardId` no `TransactionResponse` deve ser expandido para incluir o nome do cartão
   em lançamentos parcelados (facilita exibição no frontend):
   ```json
   "card": {
     "id":   "uuid",
     "name": "string"
   }
   ```
   Para lançamentos sem cartão, `"card": null`.
   **Atenção:** isso é uma atualização do `TransactionResponse` — backend deve ajustar.

---

## Fatia 4 — Dashboard (Sprint 05)

> Contrato fechado em 2026-05-29.
> Nenhuma migration nova é necessária — todas as tabelas já existem (V1–V7).

---

### GET /api/dashboard

Retorna os dados agregados do mês para exibição no dashboard principal.

**Headers:** `Authorization: Bearer <token>`

**Query params:**

| Param   | Tipo      | Obrigatório | Descrição |
|---------|-----------|-------------|-----------|
| `month` | `yyyy-MM` | sim         | Mês consultado (ex.: `2026-05`) |

**Response 200:**
```json
{
  "month": "2026-05",
  "totalIncome": 10000.00,
  "totalExpense": 6500.00,
  "balance": 3500.00,
  "previousMonth": {
    "totalIncome": 9500.00,
    "totalExpense": 6000.00,
    "balance": 3500.00
  },
  "incomeVariation": 5.26,
  "expenseVariation": 8.33,
  "expenseByCategory": [
    {
      "categoryId": "uuid",
      "categoryName": "Alimentação",
      "categoryColor": "#e8927c",
      "total": 1500.00,
      "percentage": 23.08
    }
  ],
  "recentTransactions": [
    {
      "id": "uuid",
      "date": "2026-05-15",
      "description": "Supermercado",
      "amount": 250.00,
      "type": "EXPENSE",
      "categoryName": "Alimentação",
      "categoryColor": "#e8927c",
      "paidByPersonName": "Alice"
    }
  ]
}
```

**Campos do response:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `month` | `string yyyy-MM` | Mês consultado |
| `totalIncome` | `number` | Soma de todas as receitas do mês (ver regras de agregação) |
| `totalExpense` | `number` | Soma de todas as despesas do mês (ver regras de agregação) |
| `balance` | `number` | `totalIncome - totalExpense` (pode ser negativo) |
| `previousMonth` | `object` | Mesmos campos `totalIncome`, `totalExpense`, `balance` do mês anterior |
| `incomeVariation` | `number` | Variação percentual de receitas em relação ao mês anterior; `null` se mês anterior sem receita |
| `expenseVariation` | `number` | Variação percentual de despesas em relação ao mês anterior; `null` se mês anterior sem despesa |
| `expenseByCategory` | `array` | Despesas agrupadas por categoria, ordenadas por `total` decrescente |
| `recentTransactions` | `array` | Últimos 10 lançamentos do mês, ordenados por `date` decrescente + `created_at` decrescente |

**Campos de `expenseByCategory`:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `categoryId` | `uuid` | ID da categoria |
| `categoryName` | `string` | Nome da categoria |
| `categoryColor` | `string hex` | Cor da categoria (para exibição no gráfico) |
| `total` | `number` | Total de despesas nesta categoria no mês |
| `percentage` | `number` | Percentual sobre o `totalExpense` do mês; 2 casas decimais |

**Campos de `recentTransactions`:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | `uuid` | ID do lançamento ou da parcela (ver nota abaixo) |
| `date` | `string yyyy-MM-dd` | Data do lançamento (`transaction.date`) |
| `description` | `string \| null` | Descrição do lançamento |
| `amount` | `number` | Valor efetivo no mês (ver regras de agregação) |
| `type` | `EXPENSE \| INCOME` | Tipo do lançamento |
| `categoryName` | `string` | Nome da categoria |
| `categoryColor` | `string hex` | Cor da categoria |
| `paidByPersonName` | `string` | Nome de quem pagou |

> **Nota sobre `id` em `recentTransactions`:** para lançamentos à vista, `id` é o UUID da
> `Transaction`. Para lançamentos parcelados (CREDIT, `installmentsTotal > 1`), o valor
> exibido vem da `Installment` cujo `reference_month` cai no mês consultado — use o UUID
> da `Transaction` pai neste campo também, para que o frontend possa navegar ao detalhe.

**Regras de agregação — OBRIGATÓRIAS (backend e QA devem cobrir com testes):**

1. **Lançamentos parcelados (CREDIT + `installmentsTotal > 1`):**
   - O valor que entra no dashboard é o `amount` da `Installment` cujo `reference_month`
     é igual ao mês consultado (formato `yyyy-MM-01`).
   - A `transaction.date` (data de compra) é irrelevante para a agregação mensal.

2. **Todos os outros lançamentos (CASH, DEBIT, PIX, TRANSFER e CREDIT com `installmentsTotal = 1`):**
   - O valor que entra no dashboard é o `transaction.amount`.
   - O critério de inclusão é `transaction.date` no intervalo
     `[primeiro dia do mês, último dia do mês]`.

3. **Receitas (type = INCOME):**
   - Sempre pelo `transaction.date` (receitas não são parceladas).
   - Critério: `transaction.date` no mês consultado.

4. **Variação percentual:**
   - `incomeVariation = ((mesMes - mesAnterior) / mesAnterior) * 100`, arredondado a 2 casas.
   - Se `mesAnterior = 0`, retornar `null` (indeterminado — não exibir no frontend).
   - `expenseVariation` segue a mesma fórmula.

5. **`expenseByCategory.percentage`:**
   - `(total da categoria / totalExpense) * 100`, arredondado a 2 casas decimais.
   - Se `totalExpense = 0`, retornar lista vazia em `expenseByCategory`.

| Status | Situação |
|--------|----------|
| 200 | Dados do dashboard retornados com sucesso |
| 400 | `month` ausente — "Parâmetro 'month' é obrigatório no formato yyyy-MM." |
| 400 | `month` em formato inválido — "Formato de mês inválido. Use yyyy-MM (ex.: 2026-05)." |
| 401 | Token ausente ou inválido |

---

## Fatia 5 — Orçamento (Sprint 05)

> Contrato fechado em 2026-05-29.
> A tabela `budget` já existe no schema V2. Nenhuma migration nova é necessária.
>
> **Escopo do Sprint 05:** apenas CRUD de orçamento + cálculo de consumo.
> Lançamentos recorrentes (RecurringRule) foram movidos para o Sprint 06.

---

### BudgetResponse — formato compartilhado por GET e PUT

```json
{
  "id": "uuid",
  "categoryId": "uuid",
  "categoryName": "Alimentação",
  "categoryColor": "#e8927c",
  "month": "2026-05-01",
  "limitAmount": 2000.00,
  "spentAmount": 1500.00,
  "remainingAmount": 500.00,
  "percentage": 75.0,
  "status": "WARNING",
  "version": 0
}
```

**Campos:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | `uuid` | ID do orçamento |
| `categoryId` | `uuid` | ID da categoria |
| `categoryName` | `string` | Nome da categoria |
| `categoryColor` | `string hex` | Cor da categoria |
| `month` | `string yyyy-MM-dd` | Mês do orçamento, sempre no formato `yyyy-MM-01` |
| `limitAmount` | `number` | Limite definido para a categoria no mês |
| `spentAmount` | `number` | Total já gasto na categoria no mês (mesma regra de agregação do dashboard) |
| `remainingAmount` | `number` | `limitAmount - spentAmount` (pode ser negativo se excedido) |
| `percentage` | `number` | `(spentAmount / limitAmount) * 100`, 2 casas decimais |
| `status` | `string` | `OK` se percentage ≤ 70%; `WARNING` se > 70% e ≤ 100%; `EXCEEDED` se > 100% |
| `version` | `integer` | Versão para optimistic locking |

**Regras de cálculo de `spentAmount`:** aplica as mesmas regras de agregação do dashboard
(parcelas pelo `installment.reference_month`, demais pelo `transaction.date`).
Considera apenas transações do tipo `EXPENSE` para a categoria e mês informados.

**Regras de status:**
- `OK`: percentage ≤ 70%
- `WARNING`: percentage > 70% e ≤ 100%
- `EXCEEDED`: percentage > 100%

---

### GET /api/budgets

Lista todos os orçamentos do mês consultado, com o gasto real calculado.

**Headers:** `Authorization: Bearer <token>`

**Query params:**

| Param   | Tipo      | Obrigatório | Descrição |
|---------|-----------|-------------|-----------|
| `month` | `yyyy-MM` | sim         | Mês a consultar (ex.: `2026-05`) |

**Response 200:** array de `BudgetResponse` (pode ser `[]`), ordenado por `categoryName` ascendente.

| Status | Situação |
|--------|----------|
| 200 | Lista retornada (pode ser `[]`) |
| 400 | `month` ausente — "Parâmetro 'month' é obrigatório no formato yyyy-MM." |
| 400 | `month` em formato inválido — "Formato de mês inválido. Use yyyy-MM (ex.: 2026-05)." |
| 401 | Token ausente ou inválido |

---

### POST /api/budgets

Cria um novo orçamento para uma categoria em um mês específico.

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "categoryId":  "uuid (obrigatório)",
  "month":       "string yyyy-MM (obrigatório)",
  "limitAmount": "number > 0 (obrigatório)"
}
```

**Validações:**
- `categoryId`: deve existir e estar ativa; categoria deve ser do tipo `EXPENSE` ou `BOTH`
  (não faz sentido orçar receitas)
- `month`: formato `yyyy-MM`; internamente armazenado como `yyyy-MM-01`
- `limitAmount`: deve ser maior que zero
- Unicidade: não pode existir outro `Budget` com o mesmo `categoryId` e `month`

**Response 201:** `BudgetResponse` completo (com `spentAmount` calculado).

| Status | Situação |
|--------|----------|
| 201 | Orçamento criado com sucesso |
| 400 | Campo inválido — mensagens específicas (ver abaixo) |
| 401 | Token ausente ou inválido |
| 404 | `categoryId` não encontrado ou inativo — "Categoria não encontrada." |
| 409 | Já existe orçamento para esta categoria neste mês — "Já existe um orçamento para esta categoria neste mês." |
| 422 | Categoria não é do tipo EXPENSE ou BOTH — "Orçamentos só podem ser criados para categorias de despesa." |

**Mensagens de erro por campo:**
- `categoryId` nulo: `"Categoria é obrigatória."`
- `month` nulo ou inválido: `"Mês é obrigatório no formato yyyy-MM."`
- `limitAmount` nulo: `"Limite é obrigatório."`
- `limitAmount` ≤ 0: `"O limite deve ser maior que zero."`

---

### PUT /api/budgets/{id}

Atualiza o limite de um orçamento existente. Suporta optimistic locking.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do orçamento

**Request:**
```json
{
  "limitAmount": "number > 0 (obrigatório)",
  "version":     "integer (obrigatório — optimistic locking)"
}
```

> Nota: `categoryId` e `month` não são editáveis. Para trocar categoria ou mês, exclua
> e recrie o orçamento.

**Response 200:** `BudgetResponse` atualizado (com `spentAmount` recalculado).

| Status | Situação |
|--------|----------|
| 200 | Orçamento atualizado com sucesso |
| 400 | `limitAmount` ≤ 0 — "O limite deve ser maior que zero." |
| 400 | `version` ausente — "Campo 'version' é obrigatório para atualização." |
| 401 | Token ausente ou inválido |
| 404 | Orçamento não encontrado — "Orçamento não encontrado." |
| 409 | Conflito de versão — "O registro foi alterado por outro usuário. Recarregue e tente novamente." |

---

### DELETE /api/budgets/{id}

Remove permanentemente um orçamento. Não afeta lançamentos existentes.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID do orçamento

**Response 204:** sem body

| Status | Situação |
|--------|----------|
| 204 | Orçamento removido com sucesso |
| 401 | Token ausente ou inválido |
| 404 | Orçamento não encontrado — "Orçamento não encontrado." |

---

## Fatia 6a — Acerto de Contas (Sprint 06)

> Contrato fechado em 2026-05-30. Nenhuma migration nova necessária.

---

### GET /api/settlement

Calcula o acerto de contas do mês: quanto cada pessoa pagou, quanto deveria pagar e quem deve quanto a quem.

**Headers:** `Authorization: Bearer <token>`

**Query params:**

| Param   | Tipo      | Obrigatório | Descrição |
|---------|-----------|-------------|-----------|
| `month` | `yyyy-MM` | não         | Mês consultado; padrão: mês corrente |

**Response 200:**
```json
{
  "month":               "2026-05-01",
  "totalExpense":        900.00,
  "personA": {
    "id":          "uuid",
    "name":        "Alice",
    "totalPaid":   900.00,
    "shouldPay":   450.00,
    "balance":     450.00
  },
  "personB": {
    "id":          "uuid",
    "name":        "Bob",
    "totalPaid":   0.00,
    "shouldPay":   450.00,
    "balance":    -450.00
  },
  "debtor":              "PERSON_B",
  "creditor":            "PERSON_A",
  "amountOwed":          450.00,
  "settled":             false,
  "pendingProportional": false,
  "pendingMessage":      null
}
```

**Campos:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `month` | `string yyyy-MM-dd` | Primeiro dia do mês consultado |
| `totalExpense` | `number` | Total de despesas computadas no mês |
| `personA` | `object` | Pessoa com menor nome alfabético |
| `personB` | `object` | Pessoa com maior nome alfabético |
| `personA/B.totalPaid` | `number` | Quanto a pessoa pagou no mês |
| `personA/B.shouldPay` | `number` | Quanto a pessoa deveria pagar conforme regras de divisão |
| `personA/B.balance` | `number` | `totalPaid - shouldPay`; positivo = credor, negativo = devedor |
| `debtor` | `"PERSON_A" \| "PERSON_B" \| null` | Quem deve pagar; `null` se quitado ou pendente |
| `creditor` | `"PERSON_A" \| "PERSON_B" \| null` | Quem deve receber; `null` se quitado ou pendente |
| `amountOwed` | `number \| null` | Valor da dívida; `null` se `settled=true` ou `pendingProportional=true` |
| `settled` | `boolean` | `true` se o saldo é menor que R$ 0,01 |
| `pendingProportional` | `boolean` | `true` se há despesas PROPORTIONAL sem receitas individuais cadastradas |
| `pendingMessage` | `string \| null` | Mensagem em pt-br orientando o usuário; `null` se sem pendência |

**Regras de cálculo:**

- Entram no acerto as `Installment` cujo `reference_month` é o mês consultado, mais lançamentos à vista do mês.
- `FIFTY_FIFTY`: 50% para cada pessoa.
- `PERSON_A` / `PERSON_B`: 100% da despesa atribuída à pessoa indicada.
- `PROPORTIONAL`: proporcional às receitas individuais (`split_rule=PERSON_A/B`, `type=INCOME`) do mês. Se não houver receita individual cadastrada → `pendingProportional=true`, `amountOwed=null`.
- Ordenação de personA/personB: sempre pelo nome, ordem alfabética ascendente.

| Status | Situação |
|--------|----------|
| 200 | Acerto calculado com sucesso |
| 400 | `month` em formato inválido — "Formato de mês inválido. Use yyyy-MM (ex.: 2026-05)." |
| 401 | Token ausente ou inválido |

---

## Fatia 6b — Lançamentos Recorrentes (Sprint 06)

> Contrato fechado em 2026-05-30. Migration `V9__recurring_rules.sql` já aplicada.

---

### GET /api/recurring-rules

Lista todas as regras de lançamento recorrente ativas, ordenadas por `created_at` ascendente.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
[
  {
    "id":               "uuid",
    "type":             "EXPENSE | INCOME",
    "amount":           500.00,
    "description":      "Aluguel",
    "categoryId":       "uuid",
    "categoryName":     "Moradia",
    "paidByPersonId":   "uuid",
    "paidByPersonName": "Alice",
    "paymentMethod":    "TRANSFER",
    "splitRule":        "FIFTY_FIFTY",
    "frequency":        "MONTHLY",
    "nextDate":         "2026-06-01",
    "active":           true,
    "createdAt":        "2026-05-30T10:00:00",
    "version":          0
  }
]
```

| Status | Situação |
|--------|----------|
| 200 | Lista retornada (pode ser `[]`) |
| 401 | Token ausente ou inválido |

---

### POST /api/recurring-rules

Cria uma nova regra de lançamento recorrente.

**Headers:** `Authorization: Bearer <token>`

**Request:**
```json
{
  "type":            "EXPENSE | INCOME (obrigatório)",
  "amount":          "number > 0 (obrigatório)",
  "description":     "string 1–255 chars (obrigatório)",
  "categoryId":      "uuid (obrigatório)",
  "paidByPersonId":  "uuid (obrigatório)",
  "paymentMethod":   "CASH | DEBIT | CREDIT | PIX | TRANSFER (obrigatório)",
  "splitRule":       "PERSON_A | PERSON_B | FIFTY_FIFTY | PROPORTIONAL (obrigatório)",
  "frequency":       "MONTHLY (obrigatório)",
  "startDate":       "string yyyy-MM-dd (obrigatório — primeira data de geração)"
}
```

**Validações:**
- `description` não pode ser nulo ou vazio (obrigatório para garantir idempotência do job)
- `amount` deve ser maior que zero
- `frequency` suporta apenas `MONTHLY` nesta versão

**Response 201:** mesmo formato do `GET /api/recurring-rules` (item único).

| Status | Situação |
|--------|----------|
| 201 | Regra criada com sucesso |
| 400 | Campo inválido — mensagens específicas |
| 401 | Token ausente ou inválido |
| 404 | `categoryId` ou `paidByPersonId` não encontrado |

---

### DELETE /api/recurring-rules/{id}

Desativa uma regra recorrente (soft delete — `active=false`). Lançamentos já gerados não são removidos.

**Headers:** `Authorization: Bearer <token>`

**Path param:** `id` — UUID da regra

**Response 204:** sem body

| Status | Situação |
|--------|----------|
| 204 | Regra desativada com sucesso |
| 401 | Token ausente ou inválido |
| 404 | Regra não encontrada — "Regra recorrente não encontrada." |

---

## Fatia 7 — Exportação (Sprint 07)

> Contrato fechado em 2026-05-30. Nenhuma migration necessária.

---

### GET /api/export

Exporta todos os lançamentos do mês em formato CSV ou XLSX para download no browser.

**Headers:** `Authorization: Bearer <token>`

**Query params:**

| Param    | Tipo      | Obrigatório | Descrição |
|----------|-----------|-------------|-----------|
| `month`  | `yyyy-MM` | não         | Mês a exportar; padrão: mês corrente |
| `format` | `string`  | sim         | `csv` ou `xlsx` |

**Response 200 — CSV (`format=csv`):**
- `Content-Type: text/csv;charset=UTF-8`
- `Content-Disposition: attachment; filename="gastos-2026-05.csv"`
- Body: arquivo CSV com BOM UTF-8, separador vírgula, primeira linha com cabeçalho

**Response 200 — XLSX (`format=xlsx`):**
- `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- `Content-Disposition: attachment; filename="gastos-2026-05.xlsx"`
- Body: arquivo XLSX com cabeçalho em negrito e fundo cinza

**Colunas do arquivo (em pt-br):**

| # | Coluna | Tipo | Descrição |
|---|--------|------|-----------|
| 1 | Data | `yyyy-MM-dd` | Data efetiva (da transação ou da parcela) |
| 2 | Descrição | `string` | Descrição do lançamento |
| 3 | Categoria | `string` | Nome da categoria |
| 4 | Quem Pagou | `string` | Nome de quem realizou o pagamento |
| 5 | Tipo | `Receita` \| `Despesa` | Tipo do lançamento em pt-br |
| 6 | Valor | `string decimal` | Valor com ponto decimal (ex.: `150.00`) |
| 7 | Divisão | `50/50` \| `Pessoa A` \| `Pessoa B` \| `Proporcional` | Regra de divisão em pt-br |
| 8 | Parcela | `N/M` \| `À vista` | Número da parcela (ex.: `1/3`) ou `À vista` |

**Regras de inclusão:**
- Despesas à vista (não parceladas): filtradas pela `transaction.date` no mês
- Receitas: filtradas pela `transaction.date` no mês
- Parcelas de despesas no cartão: filtradas pelo `installment.reference_month`
- Ordenação: Data ASC, Descrição ASC

| Status | Situação |
|--------|----------|
| 200 | Arquivo gerado e retornado para download |
| 400 | `format` ausente ou inválido — "Formato inválido. Use 'csv' ou 'xlsx'." |
| 400 | `month` em formato inválido |
| 401 | Token ausente ou inválido |

---

## Pontos em aberto

| # | Ponto | Impacto | Ação necessária | Sprint |
|---|-------|---------|-----------------|--------|
| 1 | Migration V4 para coluna `inactive` em `category` | DBA deve criar antes do backend | CONCLUÍDO — V4__add_category_inactive.sql aplicada | 02 |
| 2 | Retornar `version` nos GETs de categorias | Frontend precisa para enviar no PUT | CONCLUÍDO — backend inclui `version` nos DTOs | 02 |
| 3 | Status 422 vs 400 para categoria incompatível | Consistência de status codes | CONCLUÍDO — adotado 422 para incompatibilidade de tipo | 02 |
| 4 | Paginação de `/api/transactions` | Não necessária agora (base pequena) | Avaliar se o volume crescer | futuro |
| 5 | Integração de `cardId` no formulário de lançamentos | Frontend ainda não integra seleção de cartão | CONCLUÍDO — Frontend Sprint 03 (S-03-02) | 03 |
| 6 | `DELETE /api/transactions` para parcelados | Bloqueado no Sprint 02; precisa cascade installments | CONCLUÍDO — ON DELETE CASCADE já no schema V2; habilitado no Sprint 04 | 04 |
| 7 | `PUT /api/transactions/{id}` para parcelados | Comportamento indefinido antes do Sprint 04 | DEFINIDO — retorna 422; lançamentos parcelados não editáveis individualmente | 04 |
| 8 | `TransactionResponse.cardId` → expandir para objeto `card` | Frontend precisa do nome do cartão na lista | Backend Sprint 04 — ajustar DTO de resposta | 04 |
| 9 | `closing_day` >= 29 em meses curtos (fev) | Possível confusão no cálculo de reference_month | DOCUMENTADO — algoritmo correto; purchaseDate.dayOfMonth nunca excede 28 em fev | 04 |
| 10 | Lançamentos recorrentes (RecurringRule) | Movido para Sprint 06 | API a detalhar no planning do Sprint 06 | 06 |
| 11 | `incomeVariation`/`expenseVariation` quando mês anterior = 0 | Divisão por zero | DEFINIDO — retornar `null`; frontend não exibe variação | 05 |
| 12 | `spentAmount` em Budget usa mesma regra de agregação do dashboard | Consistência parcelas vs à vista | DEFINIDO — sim, mesma lógica do dashboard (installment.reference_month) | 05 |
