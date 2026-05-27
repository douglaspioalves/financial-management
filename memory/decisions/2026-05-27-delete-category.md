# Decisão — Comportamento de DELETE /api/categories/{id}
**Data:** 2026-05-27
**Sprint:** 02
**Tomada por:** Agente Arquiteto
**Contexto:** planning / início de sprint

---

## Problema / contexto

O endpoint `DELETE /api/categories/{id}` precisa definir o que acontece quando a
categoria a ser removida já tem lançamentos (`transaction`) associados a ela.

O schema atual em `V2__initial_schema.sql` define:

```sql
CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES category (id)
```

E o campo `category_id` na tabela `transaction` é `NOT NULL`. Isso significa que
uma deleção física da categoria quebraria a integridade referencial e o PostgreSQL
rejeitaria a operação com erro FK — a não ser que o esquema fosse alterado.

Além de `transaction`, a categoria também é referenciada por `budget` e `recurring_rule`
com FKs sem `ON DELETE CASCADE`.

---

## Opções consideradas

| Opção | Prós | Contras |
|-------|------|---------|
| **A — Bloqueio (409):** retorna 409 se há lançamentos vinculados; usuário deve reclassificar primeiro | Simples de implementar; sem alteração de schema; sem surpresas nos dados | UX ruim para categorias antigas com muitos lançamentos; pode bloquear indefinidamente |
| **B — Soft delete:** adiciona coluna `inactive BOOLEAN NOT NULL DEFAULT false`; categoria some dos dropdowns mas lançamentos existentes mantêm FK válida | Preserva histórico intacto; sem alteração de FKs existentes; sem risco de dados órfãos; implementação simples; reversível | Requer migration (V4); lógica de filtro `WHERE inactive = false` em todas as queries de categoria |
| **C — Cascade para null:** altera `category_id` para nullable, seta `NULL` nos lançamentos ao deletar | Permite deleção "limpa" | Requer migration complexa (alterar NOT NULL, adicionar ON DELETE SET NULL); lançamentos sem categoria perdem contexto de relatório; quebra a invariante "todo lançamento tem categoria" do modelo de domínio |

---

## Decisão tomada

**Optamos pela Opção B — Soft delete**, adicionando `inactive BOOLEAN NOT NULL DEFAULT false`
à tabela `category`.

### Justificativa

1. **Integridade do histórico:** o app serve para controle financeiro de um casal.
   Lançamentos antigos devem continuar referenciando a categoria original para que
   relatórios e o acerto de contas de meses passados permaneçam corretos. Deletar ou
   nullificar a FK corromperia esses dados retroativamente.

2. **Schema mínimo:** a Opção B requer apenas adicionar uma coluna com `DEFAULT false`,
   sem alterar FKs existentes. A migration é segura e não quebra dados atuais.

3. **UX adequada para o contexto:** o casal usa uma base pequena. Soft delete é
   transparente: a categoria some dos formulários mas ninguém precisa reclassificar
   manualmente dezenas de lançamentos (problema da Opção A). Se quiserem reativar,
   basta reverter — possibilidade que a Opção C não oferece.

4. **A Opção A (bloqueio)** seria aceitável se o app fosse multi-tenant com muitos
   usuários, onde categorias raramente têm lançamentos. Para um casal com histórico
   acumulado, o bloqueio seria uma frustração constante.

5. **A Opção C (null em cascade)** viola a invariante do domínio: "todo lançamento
   tem uma categoria". Isso complicaria todas as telas de listagem e o acerto de contas
   (precisaria tratar `null` como caso especial).

---

## Impacto

- [x] Agente DBA: criar `V4__add_category_inactive.sql`
  ```sql
  ALTER TABLE category ADD COLUMN inactive BOOLEAN NOT NULL DEFAULT false;
  ```
- [x] Backend: filtrar `WHERE inactive = false` em `CategoryRepository` (método padrão)
- [x] Backend: `DELETE /api/categories/{id}` executa `UPDATE category SET inactive = true WHERE id = ?`
- [x] Backend: `GET /api/categories` exclui categorias inativas por padrão
- [x] `docs/api.md`: documentado — DELETE retorna 204 sem verificação de lançamentos
- [ ] Avaliar se haverá endpoint para reativar categoria (`PUT /api/categories/{id}/activate`)
      — não está no escopo do Sprint 02; registrar como backlog se o usuário solicitar

---

## Revisão futura

Esta decisão deve ser reavaliada se:
- O app crescer para múltiplos casais (multi-tenant) e a proliferação de categorias
  inativas se tornar um problema de performance — nesse caso considerar limpeza periódica.
- O usuário solicitar uma tela de "categorias arquivadas" com opção de reativar —
  a coluna `inactive` já suporta isso, basta adicionar o endpoint e a UI.
