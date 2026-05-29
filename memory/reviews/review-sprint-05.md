# Revisão Sprint 05 — Dashboard + Orçamento

**Data:** 2026-05-29
**Revisor:** Agente Revisor de Código
**Branches revisadas:** `feature/s05-schema`, `feature/s05-backend-dashboard`, `feature/s05-backend-budget`, `feature/s05-tests`, `feature/s05-frontend-dashboard`, `feature/s05-frontend-budget`

---

## Veredito: REPROVADO

**Motivo bloqueante:** `remainingAmount` ausente no backend (`BudgetResponse`) mas consumido ativamente pelo frontend (`BudgetListComponent`). O campo será `undefined` em tempo de execução, quebrando a exibição de "Restam R$ X.XX" e "Excedido em R$ X.XX" para todos os orçamentos. Isto é uma regressão funcional visível ao usuário.

---

## Itens OK

### Segurança
- `SecurityConfig` usa `anyRequest().authenticated()` — cobre automaticamente `/api/dashboard` e todos os endpoints `/api/budgets`. Não há brecha de autenticação.
- BCrypt para senhas, JWT stateless, sem segredos hardcoded nas branches revisadas.
- Nenhum campo sensível exposto nos DTOs (nenhum `password_hash`, nenhum `User` entity direto).
- Validações Bean Validation presentes no `BudgetRequest` (`@NotNull`, `@Positive`), mensagens em pt-br.
- Unicidade `category+month` tratada com 409 via `ResponseStatusException` — correto.
- Rotas Angular protegidas com `authGuard` em `/dashboard` e `/budget`.

### Lógica de negócio — DashboardService
- Regra de agregação correta: despesas CREDIT parceladas (`installmentsTotal > 1`) entram via `Installment.referenceMonth`; demais pelo `transaction.date`. Sem dupla contagem.
- `computeVariation` retorna `null` quando `previous == 0`. Sem divisão por zero.
- `buildRecentTransactions` aplica `.limit(10)` — limitado a 10.
- `buildCategoryBreakdown` agrupa por `categoryId`, soma installments separado das cashExpenses, calcula percentual com `HALF_UP`. Soma ~100% garantida pela matemática da divisão.
- JOIN FETCH em todos os queries de repositório — sem problema N+1.

### Lógica de negócio — BudgetService
- `calculateSpent` usa mesma regra de agregação do dashboard (não-parcelado por `transaction.date`; parcelado por `installment.referenceMonth`).
- Status `OK/WARNING/EXCEEDED`: OK ≤ 70%, WARNING > 70% e ≤ 100%, EXCEEDED > 100%.
- `validateFirstDayOfMonth` aplicada em `getByMonth` e `create` — protege contra datas inválidas.
- Optimistic locking manual no `update`: compara `budget.getVersion()` vs `request.version()`, lança `ObjectOptimisticLockingFailureException` em divergência → `GlobalExceptionHandler` converte em 409 com mensagem pt-br.
- `@Version` presente na entidade `Budget` para proteção JPA de baixo nível.
- `COALESCE(SUM(...), 0)` no repositório — sem NPE em meses sem despesas.
- Conflito category+month lança 409 com mensagem pt-br.

### Schema e convenções
- `V8` apenas cria `idx_transaction_type` — motivação documentada, sem DROP, sem DDL destrutivo. Correto.
- `BigDecimal` usado em todos os valores monetários nos DTOs e entidades.
- Camadas respeitadas (controller → service → repository → domain) nas duas features.
- Entidades JPA nunca expostas diretamente nos controllers.
- `month` com `CHECK (DAY = 1)` já existia em V2 — constraint de banco garantida.

### Frontend
- `ng2-charts` instalado (`^10.0.0`), `chart.js` (`^4.5.1`). Registrado em `app.config.ts` com `provideCharts(withDefaultRegisterables())`.
- `DashboardComponent` usa `BaseChartDirective` importado corretamente; donut chart com `computed()` signal — reativo.
- `authGuard` protege `/dashboard` (branch `s05-frontend-dashboard`) e `/budget` (ambas as branches de frontend).
- Barras de progresso com cores por status (`status--ok`, `status--warning`, `status--exceeded`).
- `snackBar` com mensagens em pt-br em todos os handlers de erro HTTP.
- Valores monetários em fonte display (`var(--font-display)`) conforme design system.
- Modos claro/escuro via CSS variables, toggle implementado.
- `ChangeDetectionStrategy.OnPush` em ambos os componentes — boa prática.
- Navegação por mês com `previousMonth()` / `nextMonth()` — reutiliza sinal `currentDate`.

---

## Problemas encontrados

### BLOQUEANTE

#### B-01 — `remainingAmount` ausente no backend, consumido pelo frontend
**Branch afetada:** `feature/s05-backend-budget` (backend) e `feature/s05-frontend-budget` (frontend)

O `BudgetResponse` do backend define:
```java
public record BudgetResponse(
    UUID id, UUID categoryId, String categoryName, String categoryColor,
    LocalDate month, BigDecimal limitAmount, BigDecimal spentAmount,
    BigDecimal percentage, String status, Long version
) {}
```
Não há campo `remainingAmount`.

O modelo TypeScript (`budget.model.ts`) declara:
```typescript
remainingAmount: number; // limitAmount - spentAmount (pode ser negativo)
```
E o template `budget-list.component.html` usa ativamente:
```html
Excedido em {{ formatCurrency(-budget.remainingAmount) }}
Restam {{ formatCurrency(budget.remainingAmount) }}
```

Em tempo de execução, `budget.remainingAmount` será `undefined` → `formatCurrency(undefined)` → exibirá "NaN" ou string inválida para o usuário.

**Correção necessária:** Adicionar `remainingAmount` ao `BudgetResponse` Java:
```java
public record BudgetResponse(
    UUID id, UUID categoryId, String categoryName, String categoryColor,
    LocalDate month, BigDecimal limitAmount, BigDecimal spentAmount,
    BigDecimal remainingAmount,   // limitAmount.subtract(spentAmount)
    BigDecimal percentage, String status, Long version
) {}
```
E calcular em `BudgetService.toResponse()`:
```java
budget.getLimitAmount().subtract(spent)
```

---

### RECOMENDADO

#### R-01 — Conflito em `app.routes.ts` entre branches de frontend
**Branches afetadas:** `feature/s05-frontend-dashboard` e `feature/s05-frontend-budget`

As duas branches modificam `app.routes.ts` de forma incompatível:

- `feature/s05-frontend-dashboard`: `redirectTo: '/dashboard'`, usa `loadChildren` para `/dashboard`.
- `feature/s05-frontend-budget`: `redirectTo: '/auth/login'`, usa `loadComponent` diretamente para `/dashboard` (sem `loadChildren`), não contém rota `/budget`.

O merge das duas branches para `master` gerará conflito de git. É necessário consolidar `app.routes.ts` antes do merge, produzindo uma versão única que contenha todas as rotas (`/dashboard`, `/transactions`, `/cards`, `/budget`) com `redirectTo: '/dashboard'` (ou `/auth/login` — definir com equipe).

#### R-02 — TC-D6 do `DashboardIntegrationTest` falhará ao ser habilitado
**Branch afetada:** `feature/s05-tests`

O teste `TC-D6` espera `status().isBadRequest()` (400) quando `month` não é informado:
```java
mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + obtainToken()))
    .andExpect(status().isBadRequest());
```
O `DashboardController` usa `@RequestParam(required = false)` e faz fallback para `YearMonth.now()` quando `month == null` — retorna 200 OK, não 400.

O teste está `@Disabled` agora, mas ao ser habilitado após o merge, irá falhar.

**Opções:** (a) corrigir o teste para esperar 200 ou (b) remover o fallback do controller e tornar `month` obrigatório.

#### R-03 — TC-D8 usa `$.byCategory` mas campo JSON é `expenseByCategory`
**Branch afetada:** `feature/s05-tests`

O `DashboardResponse` serializa como `expenseByCategory` em JSON. O `TC-D8` usa:
```java
.andExpect(jsonPath("$.byCategory").isArray())
.andExpect(jsonPath("$.byCategory[0].categoryId").isNotEmpty())
```
`byCategory` não existe no JSON → os `andExpect` retornarão erro ao habilitado. Também `@Disabled` agora, mas precisa ser corrigido.

**Correção:** usar `$.expenseByCategory` nos jsonPath.

#### R-04 — Divergência de limiar WARNING entre `BudgetIntegrationTest` (javadoc) e `BudgetService` (código)
**Branches afetadas:** `feature/s05-tests` e `feature/s05-backend-budget`

O javadoc de `BudgetIntegrationTest` documenta `WARNING se > 75%`, enquanto `BudgetService` implementa `> 70%`. O `BudgetServiceTest` está consistente com o código (70%). O javadoc do teste está errado — é apenas documentação, o comportamento real é o do serviço (70%), e os testes unitários testam o comportamento correto.

Deve-se corrigir o javadoc do `BudgetIntegrationTest` para `> 70%`.

---

### OPCIONAL

#### O-01 — Dashboard: `recentTransactions` inclui transações parceladas pela data da compra
**Branch afetada:** `feature/s05-backend-dashboard`

`findRecentByMonth` busca transações com `transaction.date BETWEEN :start AND :end`. Uma compra parcelada feita em maio e cuja primeira parcela cai em junho aparecerá no dashboard de maio (onde foi comprada). Isto pode surpreender o usuário, que vê a compra de R$ 300 em maio mas o dashboard de maio mostra apenas R$ 100 de despesa (a parcela). Recomendado discutir com a equipe se o comportamento desejado é exibir a transação-mãe ou nenhuma.

#### O-02 — `BudgetRequest.version` é `Long` (não primitivo): `null` no POST não é validado
**Branch afetada:** `feature/s05-backend-budget`

`version` é `Long version` (nullable) no `BudgetRequest`. No `update()`, compara-se `budget.getVersion().equals(request.version())`. Se alguém enviar `PUT` com `"version": null`, `request.version()` será `null` → `budget.getVersion().equals(null)` → `false` → `ObjectOptimisticLockingFailureException`. Comportamento incorreto mas relativamente seguro (retorna 409). Considere validar `version` como não-nulo explicitamente no `update`.

#### O-03 — Navegação do mês no dashboard não sincroniza com URL
**Branch afetada:** `feature/s05-frontend-dashboard`

Navegar para mês anterior/próximo não atualiza a URL (sem `queryParams`). Atualizar a página recarrega o mês atual, perdendo o contexto. Opcional para sprint 05 mas pode ser adicionado.

---

## Resumo por branch

| Branch | Resultado |
|--------|-----------|
| `feature/s05-schema` | APROVADA |
| `feature/s05-backend-dashboard` | APROVADA |
| `feature/s05-backend-budget` | REPROVADA (B-01: `remainingAmount` ausente) |
| `feature/s05-tests` | APROVADA com ressalvas (R-02, R-03 ao habilitar) |
| `feature/s05-frontend-dashboard` | APROVADA com ressalvas (R-01 ao merjar) |
| `feature/s05-frontend-budget` | REPROVADA (B-01: consome `remainingAmount` inexistente) |

---

## Ação requerida antes do merge

1. Adicionar `remainingAmount` ao `BudgetResponse` Java e calcular em `BudgetService.toResponse()`. **(BLOQUEANTE)**
2. Consolidar `app.routes.ts` entre as duas branches de frontend antes do merge. **(RECOMENDADO)**
3. Corrigir `TC-D6` (espera 400, recebe 200) e `TC-D8` (`$.byCategory` → `$.expenseByCategory`) antes de remover `@Disabled`. **(RECOMENDADO)**
4. Corrigir javadoc do `BudgetIntegrationTest` para usar `> 70%` ao invés de `> 75%`. **(RECOMENDADO)**
