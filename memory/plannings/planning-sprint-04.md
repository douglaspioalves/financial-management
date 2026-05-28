# Planning — Sprint 04
**Data:** 2026-05-28
**Sprint:** 04 de 07
**Período:** 2026-05-28 → 2026-06-10 (10 dias úteis)
**Fatia do roadmap:** 3b — Geração automática de parcelas (Installment)
**Conduzido por:** Scrum Master / Arquiteto

---

## Objetivo do sprint

Ao fim do sprint, uma compra parcelada no cartão de crédito gerará automaticamente N
registros de `Installment`, cada um com o `reference_month` correto baseado no
`closing_day` do cartão. A lista de lançamentos exibirá badge "X/N" e o usuário
poderá expandir para ver todas as parcelas. A soma das parcelas será sempre exatamente
igual ao valor total da compra.

---

## Contexto técnico

- **Schema:** tabela `installment` já existe em V2__initial_schema.sql com todos os
  campos necessários (id UUID, transaction_id FK com ON DELETE CASCADE, number int,
  amount NUMERIC 12,2, reference_month DATE). Índices em `reference_month` e
  `transaction_id` já criados. **Nenhuma migration nova é necessária.**
- **TransactionService.create():** atualmente lança exceção placeholder quando
  `installmentsTotal > 1`. O backend deve remover esse placeholder e implementar
  a chamada a `InstallmentService.generateInstallments()`.
- **TransactionResponse:** o campo `cardId` (UUID simples) deve ser substituído
  pelo objeto `card: { id, name }` para facilitar exibição no frontend.
  Para transações sem cartão, `card: null`.
- **Card entity:** já implementada com `closingDay` e `dueDay` (int 1–31).
- **Algoritmo de reference_month:** documentado em
  `memory/decisions/2026-05-28-installment-reference-month-algorithm.md`.
- **Limite de parcelas:** máximo 48 parcelas (`installmentsTotal` entre 1 e 48).

---

## Stories selecionadas

| ID | Story | Papel | Pontos | Depende de |
|---|---|---|---|---|
| S-04-00 | Validar schema de installment (sem migration) | DBA | 1 | — |
| S-04-01 | Geração de parcelas — backend completo | Backend | 8 | S-04-00 |
| S-04-02 | Testes críticos do algoritmo de parcelas | QA | 5 | S-04-01 |
| S-04-03 | Visualização de parcelas no frontend | Frontend | 4 | S-04-01 |
| S-04-04 | Revisão e merge | Revisor + DevOps | 1 | S-04-02, S-04-03 |

**Total de pontos:** 19

---

## Detalhamento das stories

### S-04-00 · Validar schema de installment (DBA)
**Branch:** não necessária (apenas leitura/validação — sem commit)

Responsabilidades:
- Confirmar que `V2__initial_schema.sql` contém a tabela `installment` com:
  - `id UUID PRIMARY KEY`
  - `transaction_id UUID NOT NULL REFERENCES transaction(id) ON DELETE CASCADE`
  - `number INTEGER NOT NULL CHECK (number >= 1)`
  - `amount NUMERIC(12,2) NOT NULL`
  - `reference_month DATE NOT NULL` (sempre dia 1 do mês)
  - Índice em `installment(reference_month)` e `installment(transaction_id)`
- Confirmar que `ON DELETE CASCADE` está ativo na FK `transaction_id` →
  quando a `Transaction` pai é excluída, todas as `Installment` filhas são
  removidas automaticamente pelo banco.
- Documentar resultado em `memory/sprints/sprint-04.md`.

> Resultado esperado: confirmação de que tudo está em ordem. Nenhuma migration nova.

---

### S-04-01 · Geração de parcelas — backend completo
**Branch:** `feature/s04-backend`

Tarefas em ordem:

| # | Tarefa | Detalhe |
|---|--------|---------|
| 1 | `Installment.java` (entidade JPA) | `@Entity @Table("installment")`, campos: id (UUID, gerado), transaction (ManyToOne lazy), number (int), amount (BigDecimal), referenceMonth (LocalDate) |
| 2 | `InstallmentRepository.java` | `JpaRepository<Installment, UUID>` + `findByTransactionIdOrderByNumberAsc(UUID transactionId)` |
| 3 | `InstallmentResponse.java` (DTO saída) | id (UUID), number (int), amount (BigDecimal), referenceMonth (LocalDate serializado como "yyyy-MM-dd") |
| 4 | `InstallmentService.java` | Método `generateInstallments(Transaction tx, Card card)`: implementa o algoritmo canônico de reference_month e arredondamento; persiste N registros |
| 5 | Atualizar `TransactionService.create()` | Remover placeholder de exceção; carregar `Card` pelo `cardId`; chamar `installmentService.generateInstallments(tx, card)` quando `installmentsTotal >= 2` |
| 6 | Novas validações em `TransactionService.create()` | Parcelado exige CREDIT + cardId; `installmentsTotal` entre 1 e 48 |
| 7 | Bloquear `TransactionService.update()` para parcelados | Retornar 422 com mensagem pt-br quando `transaction.installmentsTotal > 1` |
| 8 | `DELETE /api/transactions/{id}` para parcelados | Remover restrição que bloqueava; ON DELETE CASCADE cuida das parcelas no banco |
| 9 | `InstallmentController.java` | `GET /api/transactions/{id}/installments` → delega ao `InstallmentService`; retorna 200 com lista ou 404 |
| 10 | Ajustar `TransactionResponse` | Campo `cardId` (UUID) → objeto `card: { id, name }` usando `CardResponse` simplificado ou DTO interno |

**Regras críticas de implementação:**

```java
// Algoritmo de firstReferenceMonth:
YearMonth ym = (purchaseDate.getDayOfMonth() < card.getClosingDay())
    ? YearMonth.from(purchaseDate)
    : YearMonth.from(purchaseDate).plusMonths(1);
LocalDate firstReferenceMonth = ym.atDay(1);

// Cálculo de amounts:
BigDecimal base = totalAmount.divide(
    BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
BigDecimal last = totalAmount.subtract(
    base.multiply(BigDecimal.valueOf(n - 1)));
// parcelas 1..N-1 recebem `base`; parcela N recebe `last`
```

**Tratamento de erros:**
- `installmentsTotal >= 2` sem CREDIT → 400 "Compras parceladas só podem ser feitas com cartão de crédito."
- `installmentsTotal >= 2` sem `cardId` → 400 "Pagamento parcelado exige a seleção de um cartão de crédito."
- `installmentsTotal` fora de 1–48 → 400 "O número de parcelas deve ser entre 1 e 48."
- `PUT` em parcelado → 422 "Lançamentos parcelados não podem ser editados individualmente. Exclua e recrie o lançamento se necessário."
- `GET /api/transactions/{id}/installments` com ID inválido → 404 "Lançamento não encontrado."

---

### S-04-02 · Testes críticos do algoritmo de parcelas (QA)
**Branch:** `feature/s04-tests`

Casos de teste obrigatórios — unitários (`InstallmentServiceTest`):

| Caso | Cenário | Resultado esperado |
|------|---------|-------------------|
| TC-01 | Compra 2026-05-15, closing=20, 3x, R$ 1.500,00 | referenceMonths: 05-01, 06-01, 07-01; amounts: 500,00, 500,00, 500,00 |
| TC-02 | Compra 2026-05-25, closing=20, 3x, R$ 1.500,00 | referenceMonths: 06-01, 07-01, 08-01 |
| TC-03 | Compra 2026-05-20, closing=20, 2x | Dia igual ao closing → mês seguinte: 06-01, 07-01 |
| TC-04 | Compra 2026-11-25, closing=20, 3x | Virada de ano: 12-01, 2027-01-01, 2027-02-01 |
| TC-05 | Compra 2026-01-31, closing=1, 2x | closing=1, dia 31 >= 1 → mês seguinte: 02-01, 03-01 |
| TC-06 | Arredondamento: R$ 10,00 em 3x | 3,33 + 3,33 + 3,34 = 10,00 |
| TC-07 | Arredondamento: R$ 100,00 em 3x | 33,33 + 33,33 + 33,34 = 100,00 |
| TC-08 | Arredondamento: R$ 1.500,01 em 3x | 500,00 + 500,00 + 500,01 = 1.500,01 |
| TC-09 | Compra divisível exata: R$ 300,00 em 3x | 100,00 + 100,00 + 100,00 = 300,00 |
| TC-10 | closing_day=31, compra 2026-02-15 | 15 < 31 → mesmo mês: 02-01 |
| TC-11 | closing_day=28, compra 2026-02-28 | 28 >= 28 → mês seguinte: 03-01 |
| TC-12 | installmentsTotal=1 para compra CREDIT | Nenhuma installment gerada; lista de installments retorna [] |
| TC-13 | POST parcelado sem paymentMethod=CREDIT | 400 com mensagem correta |
| TC-14 | POST parcelado sem cardId | 400 com mensagem correta |
| TC-15 | POST com installmentsTotal=49 | 400 com mensagem de limite |
| TC-16 | PUT em lançamento parcelado | 422 com mensagem de bloqueio |
| TC-17 | DELETE em lançamento parcelado | 204; lançamento e installments removidos |
| TC-18 | GET installments de lançamento à vista | 200 com lista vazia [] |
| TC-19 | GET installments de lançamento parcelado | 200 com N itens; soma = totalAmount |
| TC-20 | GET installments de transação inexistente | 404 |

Testes de integração (`InstallmentIntegrationTest`):
- Criar compra parcelada via POST e verificar installments geradas no banco
- Excluir transação parcelada e verificar que installments foram removidas (CASCADE)
- GET /api/transactions/{id}/installments retorna lista ordenada por number

---

### S-04-03 · Visualização de parcelas no frontend
**Branch:** `feature/s04-frontend`

Tarefas em ordem (S-04-01 deve estar mergeada ou contratos mockados):

| # | Tarefa | Detalhe |
|---|--------|---------|
| 1 | `InstallmentService` (HTTP) | Serviço Angular — `getByTransactionId(id)`: GET /api/transactions/{id}/installments |
| 2 | Badge "X/N" na lista de lançamentos | Em `TransactionListComponent`: exibir badge coral/areia quando `installmentsTotal > 1`. Badge mostra apenas "Nx" (ex.: "3x") pois o número da parcela corrente requer cruzar com reference_month. |
| 3 | Componente de expansão de parcelas | Ao clicar no badge ou no lançamento parcelado, expandir painel com lista de parcelas; chamar `InstallmentService.getByTransactionId()` somente ao expandir (lazy load) |
| 4 | Destaque visual na parcela do mês atual | Parcela cujo `referenceMonth` == mês selecionado no filtro recebe estilo destacado (fundo azul claro ou borda azul) |
| 5 | Ocultar botão "Editar" para parcelados | Lançamentos com `installmentsTotal > 1` não exibem o botão de edição |
| 6 | Campo "Parcelas" no formulário de lançamento | Exibir `<mat-select>` com opções 1x a 48x quando `paymentMethod = CREDIT`; ocultar para outros métodos |

Design system:
- Badge de parcela: cor areia (#F5ECD7) com texto "Nx" em fonte Plus Jakarta Sans bold.
- Parcela do mês atual: fundo azul pálido (#E8F0FB) ou borda esquerda azul (#4a7fc4).
- Painel de expansão: deve ser colapsável; animação suave (Angular Material expansion panel).

---

### S-04-04 · Revisão e merge (Revisor + DevOps)
**Branch:** — (age sobre as branches abertas)

Checklist do revisor:
- [ ] Algoritmo de reference_month produz exatamente os resultados dos exemplos em TC-01 a TC-05
- [ ] Soma das parcelas sempre == totalAmount (cobrir TC-06 a TC-09)
- [ ] Nenhuma entidade JPA exposta diretamente em controller (DTOs na fronteira)
- [ ] `Installment` entity não tem `@Version` (sem necessidade: parcelas são imutáveis após geração)
- [ ] `InstallmentService.generateInstallments()` é chamado em transação (`@Transactional`)
- [ ] ON DELETE CASCADE testado: excluir transaction remove installments
- [ ] Frontend: badge visível; expansão carrega parcelas; parcela do mês destacada
- [ ] Frontend: campo "Parcelas" visível apenas quando método = CREDIT
- [ ] Todos os testes passando: `./mvnw test`

Checklist DevOps:
- [ ] Branches mergeadas para master via `git merge --no-ff`
- [ ] `docker compose up --build` sem erro
- [ ] Tag `fatia-3` criada em master (esta tag encerra a fatia 3 completa: 3a + 3b)

---

## Ordem de execução e paralelismo

```
DIA 1 (hoje):
  ├── Arquiteto:  Planning + contrato API Fatia 3b (concluído)
  └── DBA:        S-04-00 — validar schema installment (sem commit)

DIA 1–7 (núcleo — backend sequencial, depende de S-04-00 confirmado):
  └── Backend:    S-04-01 — Installment entity + service + controller + ajustes Transaction

DIA 4–7 (paralelo ao backend — QA pode usar contrato como base):
  └── QA:         S-04-02 — testes unitários e de integração (branch: feature/s04-tests)

DIA 6–9 (após backend mergeado ou usando mocks do contrato):
  └── Frontend:   S-04-03 — badge, expansão, campo parcelas (branch: feature/s04-frontend)

DIA 8–9:
  └── Revisor:    S-04-04 — revisão de todos os branches

DIA 9–10:
  └── DevOps:     merges + docker compose up --build + tag fatia-3
```

**Dependências críticas:**
- S-04-01 depende de S-04-00 (DBA confirma schema)
- S-04-02 requer S-04-01 para testar o service real (unit tests podem antecipar com mocks)
- S-04-03 pode iniciar com mock baseado no contrato do api.md
- S-04-04 requer S-04-01, S-04-02 e S-04-03 concluídos

---

## Riscos e pontos de atenção

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Arredondamento incorreto — soma das parcelas ≠ totalAmount | Alta | Crítico | TC-06 a TC-09 cobrem os casos; algoritmo com RoundingMode.DOWN + ajuste na última parcela |
| closing_day >= 29 em meses curtos — comportamento do algoritmo | Média | Alto | TC-10 e TC-11 cobrem os casos; algoritmo compara getDayOfMonth() diretamente, sem problema |
| Virada de ano — plusMonths() além de dezembro | Média | Alto | TC-04 cobre: Java YearMonth.plusMonths() lida automaticamente |
| ON DELETE CASCADE não ativo em V2 — delete de transaction não remove installments | Baixa | Crítico | DBA confirma em S-04-00; TC-17 valida o comportamento |
| Frontend: lazy load de parcelas causa flickering | Baixa | Baixo | Angular Material expansion panel com loading spinner enquanto chama API |
| Ajuste em TransactionResponse (card object) quebra frontend existente | Alta | Alto | Backend altera o DTO e Frontend atualiza na mesma sprint para manter coerência |

---

## Definição de pronto (DoD) do sprint

- [ ] S-04-00: DBA confirma schema installment sem migration necessária
- [ ] S-04-01: Compra parcelada gera N Installments; reference_month correto; soma exata
- [ ] S-04-01: PUT em parcelado retorna 422; DELETE remove transaction + cascade installments
- [ ] S-04-01: GET /api/transactions/{id}/installments funcionando
- [ ] S-04-01: TransactionResponse.card inclui { id, name } em vez de cardId UUID simples
- [ ] S-04-02: 20 casos de teste passando (TC-01 a TC-20)
- [ ] S-04-03: Badge "Nx" visível na lista; expansão exibe parcelas; mês atual destacado
- [ ] S-04-03: Campo "Parcelas" visível apenas para método CREDIT
- [ ] S-04-04: Revisão sem itens bloqueantes; `./mvnw test` BUILD SUCCESS
- [ ] `docker compose up --build` limpo
- [ ] Tag `fatia-3` criada em master
- [ ] Review registrada em `memory/reviews/review-sprint-04.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-04.md`
- [ ] Learnings atualizados em `memory/learnings/`
