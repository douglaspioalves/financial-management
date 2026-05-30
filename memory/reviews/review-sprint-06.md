# Revisão Sprint 06 — Recorrência + Acerto de Contas

**Data:** 2026-05-30
**Revisor:** Agente Revisor
**Base de comparação:** `06c5de8 origin/master`

---

## Branches revisadas

| Branch | Escopo |
|---|---|
| `feature/s06-schema` | V9 migration: índice composto (type, date) |
| `feature/s06-backend-recurring` | RecurringRule entity, service, controller |
| `feature/s06-backend-settlement` | SettlementService, controller, DTOs |
| `feature/s06-tests` | SettlementServiceTest (10 cenários) + SettlementIntegrationTest |
| `feature/s06-frontend-recurring` | Tela de regras recorrentes |
| `feature/s06-frontend-settlement` | Tela de acerto de contas |

---

## Itens verificados

### Segurança

| Item | Status | Detalhe |
|---|---|---|
| Todos os endpoints protegidos por JWT | ✅ OK | `SecurityConfig`: `anyRequest().authenticated()` — apenas `/api/auth/**` e `/api/health` públicos. Novos endpoints `/api/recurring-rules` e `/api/settlement` ficam dentro do `anyRequest()`. |
| Senhas com BCrypt | ✅ OK | `BCryptPasswordEncoder` configurado em `SecurityConfig`. |
| Nenhum secret hardcoded | ✅ OK | Nenhum token, senha ou chave fixa encontrada nos arquivos das branches. |
| DTOs não expõem `password_hash` | ✅ OK | `PersonSettlementDTO`, `SettlementResponse`, `RecurringRuleResponse` e `RecurringRuleRequest` não possuem campos sensíveis. |
| Sem SQL injection nas JPQL | ✅ OK | Todas as queries em `TransactionRepository` e `InstallmentRepository` usam parâmetros nomeados (`@Param`). Nenhuma concatenação de string. |
| SettlementController não vaza dados indevidos | ✅ OK | Retorna apenas IDs, nomes e valores monetários de Person. Nenhum campo de User exposto. |

### Regras de negócio

| Item | Status | Detalhe |
|---|---|---|
| PROPORTIONAL usa apenas receitas INCOME com PERSON_A/PERSON_B | ✅ OK | `findIndividualIncomesByMonth` filtra `t.type = 'INCOME' AND (t.splitRule = 'PERSON_A' OR t.splitRule = 'PERSON_B')`. FIFTY_FIFTY excluído. |
| PROPORTIONAL sem receitas → `pendingProportional=true`, `amountOwed=null` | ✅ OK | `computeShare` retorna `ShareResult(ZERO, ZERO, pending=true)`. Ao final, `amountOwedResult = null`, `debtor = null`, `creditor = null`. |
| Parcelas via `Installment.referenceMonth`, não `transaction.date` | ✅ OK | `installmentRepository.findExpenseInstallmentsByMonth(start)` usa `i.referenceMonth = :month`. |
| Ordenação determinística personA/personB (alfabética) | ✅ OK | `new java.util.ArrayList<>(personRepository.findAll())` + `persons.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()))`. |
| `new ArrayList<>()` antes do sort (não lista imutável) | ✅ OK | Wrapper defensivo correto para evitar `UnsupportedOperationException`. |
| `@Scheduled(cron="0 0 6 * * *")` no RecurringRuleService | ✅ OK | Anotação presente, `@EnableScheduling` adicionado em `BackendApplication`. |
| Idempotência do job de recorrência | ✅ OK | `existsByDescriptionAndAmountAndDateAndCategoryId` verificado antes de criar. `nextDate` avança independente da criação. |
| Soft delete nas RecurringRule | ✅ OK | `deactivate()` seta `active=false` — sem DELETE físico. |
| Despesas parceladas não entram pelo valor cheio | ✅ OK | `cashExpenses` filtra `(paymentMethod <> 'CREDIT' OR installmentsTotal = 1)`. |
| BigDecimal com `RoundingMode.HALF_UP` em todos os cálculos | ✅ OK | `computeShare` e todos os `setScale(2, RoundingMode.HALF_UP)` presentes. |
| Proporção usada é a do mês da parcela, não da compra | ✅ OK | `ratioA`/`ratioB` calculados a partir de `findIndividualIncomesByMonth(start, end)` onde `start`/`end` são do mês solicitado. |

### Qualidade e convenções

| Item | Status | Detalhe |
|---|---|---|
| `@Version` em entidade editável (RecurringRule) | ✅ OK | Campo `private Long version` com `@Version` presente. |
| `@Version` em Transaction/Installment | ✅ OK (herdado) | Verificado em sprints anteriores; SettlementService usa as entidades via repositório, sem alterar. |
| Camadas respeitadas (controller → service → repository) | ✅ OK | Nenhum acesso direto a repositório fora de service. |
| DTOs usados no controller (sem entidade JPA exposta) | ✅ OK | Controllers retornam apenas records DTO. |
| Migrations Flyway versionadas (sem `ddl-auto: update`) | ✅ OK | V9 criada com nome correto. |
| Mensagens de validação em pt-br | ✅ OK | `RecurringRuleRequest` tem mensagens em pt-br em todos os `@NotNull`/`@Positive`. |
| Código em inglês, textos de usuário em pt-br | ✅ OK | Classes, métodos e variáveis em inglês. Labels do frontend, mensagens de erro e `pendingMessage` em pt-br. |
| Nenhum `double`/`float` para valores monetários | ✅ OK | `BigDecimal` em toda a cadeia de cálculo. |
| Stacktrace não exposta ao cliente | ✅ OK | Controllers não propagam exceções brutas. Mensagem de erro retornada via SnackBar já formatada. |
| Erro em uma regra recorrente não para o job | ✅ OK | `try/catch` por regra dentro do loop `generateDueTransactions`. |

### Testes

| Item | Status | Detalhe |
|---|---|---|
| 10 cenários unitários no SettlementServiceTest | ✅ OK | TC-S01 a TC-S10 implementados e passando (BUILD SUCCESS relatado pelo QA). |
| TC-S01: FIFTY_FIFTY simples | ✅ OK | Cobre caso base. |
| TC-S04: PROPORTIONAL com receitas 60%/40% | ✅ OK | Cobre regra de proporção. |
| TC-S05: PROPORTIONAL sem receitas → pendente | ✅ OK | Cobre regra crítica do CLAUDE.md §3c. |
| TC-S08: parcela entra pelo referenceMonth | ✅ OK | Cobre regra crítica do CLAUDE.md §2. |
| TC-S09: receita FIFTY_FIFTY excluída da proporção | ✅ OK | Cobre regra crítica do CLAUDE.md §3a. |
| TC-S10: arredondamento HALF_UP sem perda de centavo | ✅ OK | Cobre caso de borda de arredondamento. |
| SettlementIntegrationTest — autenticação | ✅ OK | Teste ativo verifica 401 sem token. |
| SettlementIntegrationTest — testes de fluxo completo | ⚠️ Atenção | 5 de 6 testes marcados com `@Disabled`. Necessário habilitar após merge das branches. |

### Frontend

| Item | Status | Detalhe |
|---|---|---|
| Settlement: não calcula personA/personB (usa servidor) | ✅ OK | `creditorPerson` e `debtorPerson` resolvem com base no `debtor`/`creditor` da API. |
| Settlement: `pendingProportional=true` → alerta, sem card de débito | ✅ OK | Template Angular usa `@if (!data()!.pendingProportional)` para bloquear cards de pessoa e card de débito. |
| Settlement: saldo positivo em verde, negativo em coral | ✅ OK | `.balance--positive { color: var(--color-income); }` e `.balance--negative { color: var(--color-expense); }`. |
| Design system: CSS variables (sem cores hardcoded) | ✅ OK | SCSS usa exclusivamente `var(--color-*)`, `var(--font-*)`, `var(--radius-*)`. |
| Responsivo | ✅ OK | `grid-template-columns: 1fr` → `1fr 1fr` acima de 560px. |
| Recurring: reutiliza CategoryService e PersonService existentes | ✅ OK | `import { CategoryService } from '../../core/services/category.service'` e PersonService. |
| Lazy-load configurado | ✅ OK | `settlement.routes.ts` usa `loadComponent`, `app.routes.ts` usa `loadChildren` com `canActivate: [authGuard]`. |
| Mensagens de erro em pt-br | ✅ OK | SnackBar exibe mensagem da API ou fallback em pt-br. |

---

## Problemas encontrados

### Bloqueantes

Nenhum item bloqueante encontrado.

### Recomendados (nao bloqueiam o merge)

**REC-01 — `description` sem `@NotBlank` em `RecurringRuleRequest`**

Arquivo: `backend/src/main/java/com/gastos/dto/recurring/RecurringRuleRequest.java`, linha 25.

O campo `description` tem apenas `@Size(max = 255)`, mas não tem `@NotBlank`. Isso permite criar uma regra com `description = null` ou `description = ""`. O método de idempotência `existsByDescriptionAndAmountAndDateAndCategoryId` faz match por `description IS NULL` em SQL, o que pode gerar falso positivo: duas regras diferentes sem descrição, com mesmo valor e categoria no mesmo dia, nunca gerariam a segunda transação — mesmo se pertencerem a regras distintas.

Correção sugerida: adicionar `@NotBlank(message = "A descrição é obrigatória.")` ao campo `description`.

**REC-02 — Testes de integração desabilitados precisam ser completados antes do release**

Arquivo: `backend/src/test/java/com/gastos/SettlementIntegrationTest.java`, testes TC-IT-02 a TC-IT-06.

Os 5 testes de fluxo completo estão com `@Disabled` e corpos que lançam `UnsupportedOperationException`. Isso é aceitável durante o desenvolvimento paralelo de branches, mas os testes devem ser implementados e habilitados antes da tag de release da fatia 6. O TC-IT-04 em especial (fluxo FIFTY_FIFTY completo) é crítico para validação end-to-end.

**REC-03 — Idempotência frágil quando `description` é nula em regras recorrentes**

Consequência direta de REC-01. Regras sem descrição compartilham o mesmo "fingerprint" para a verificação de duplicidade, podendo suprimir criação de transações legítimas de regras distintas. A correção de REC-01 resolve este item.

### Opcionais

**OPC-01 — `SettlementService.calculate()` não valida `month` muito no futuro/passado**

Não há limitação no mês informado. Um cliente pode requisitar acerto de 2090-01 sem erro. Não é um bug, mas pode ser confuso. Avaliar se faz sentido limitar o range.

**OPC-02 — `RecurringRuleService.getAll()` não pagina resultados**

Se o usuário criar muitas regras (incomum para o contexto de casal, mas possível), a resposta cresce sem limite. Aceitável para o escopo do projeto.

**OPC-03 — Frontend settlement envia `month` no formato `yyyy-MM` mas o controller aceita `@DateTimeFormat(pattern = "yyyy-MM")`**

O serviço Angular monta `2026-05` e o controller usa `@DateTimeFormat(pattern = "yyyy-MM")`. Spring consegue parsear `YearMonth` corretamente com esse pattern. Sem problema funcional, mas vale atenção se houver alteração futura no formato.

---

## Resumo por branch

| Branch | Veredito |
|---|---|
| `feature/s06-schema` | Aprovado |
| `feature/s06-backend-recurring` | Aprovado com recomendação (REC-01, REC-03) |
| `feature/s06-backend-settlement` | Aprovado |
| `feature/s06-tests` | Aprovado com recomendação (REC-02) |
| `feature/s06-frontend-recurring` | Aprovado |
| `feature/s06-frontend-settlement` | Aprovado |

---

## Veredito final

**APROVADO PARA MERGE** — sem item bloqueante.

Antes da tag de release da fatia 6, o DevOps deve garantir que:

1. Os testes de integração `@Disabled` em `SettlementIntegrationTest` sejam implementados e habilitados após o merge das branches de backend.
2. O campo `description` em `RecurringRuleRequest` receba `@NotBlank` (REC-01) — recomendado em PR de correção `fix/s06-recurring-description-validation` antes do release.
3. `./mvnw test` → BUILD SUCCESS com as 6 branches mergeadas em master.
4. `docker compose up --build` valida a stack completa.
