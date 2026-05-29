# Revisão de Código — Sprint 04 (Parcelamento)

**Data:** 2026-05-29
**Revisor:** Agente Revisor
**Branches revisadas:**
- `feature/s04-backend` — InstallmentService, Installment entity, InstallmentRepository, endpoint GET /api/transactions/{id}/installments, TransactionService e TransactionResponse atualizados
- `feature/s04-tests` — InstallmentServiceTest (12 casos de borda), 70/70 testes
- `feature/s04-schema` — V7__sprint04_noop.sql (auditoria de schema)
- `feature/s04-frontend` — InstallmentService HTTP, badge X/N, expansion panel lazy, modelos atualizados

---

## Veredito: APROVADO COM RESSALVAS

A fatia está funcional, segura e bem testada nos casos principais. Nenhum item **bloqueante** foi encontrado. Existem dois itens **recomendados** de lógica de borda que devem ser endereçados antes da Sprint 05, além de itens opcionais de qualidade.

---

## Itens OK

### Segurança
- Endpoint `GET /api/transactions/{id}/installments` protegido por JWT: **SIM**. O `SecurityConfig` aplica `.anyRequest().authenticated()` a todas as rotas exceto `/api/health` e `/api/auth/**`. O novo endpoint herda essa proteção automaticamente.
- Nenhum campo sensível (`password_hash`, credenciais) exposto nas respostas. `InstallmentResponse` e `TransactionResponse` expõem apenas dados funcionais.
- UUID inválido no path variable: Spring Boot lança `MethodArgumentTypeMismatchException` → 400 Bad Request por padrão (comportamento correto do framework, mesmo sem handler explícito).
- Senhas com BCrypt: confirmado na `SecurityConfig`.

### Lógica crítica — InstallmentService
- Algoritmo de `reference_month` implementado corretamente:
  - dia < closingDay → mesmo mês da compra
  - dia >= closingDay → mês seguinte
- Arredondamento: `RoundingMode.DOWN` nas N-1 parcelas, última parcela absorve diferença. Soma = total exato. **Correto.**
- Virada de ano (nov + 3x → dez/jan/fev): `YearMonth.plusMonths()` trata corretamente a virada de ano. **Correto.**
- `installmentsTotal = 1` no endpoint: `TransactionService.create()` só chama `generateInstallments()` quando `installmentsTotal > 1`, portanto nenhuma `Installment` é persistida. `findInstallments()` retorna `[]` para essas transações. **Correto.**

### Convenções
- `BigDecimal` para todos os valores monetários (`amount` em `Installment`, `InstallmentResponse`, cálculos no service). **Correto.**
- Mensagens de erro em pt-br: `EntityNotFoundException`, `IllegalArgumentException`, `IllegalStateException` — todas em pt-br. **Correto.**
- Camadas respeitadas: Controller → Service → Repository. Nenhuma entidade JPA exposta diretamente no controller. `InstallmentResponse` é record imutável. **Correto.**
- Schema somente via Flyway: `V7__sprint04_noop.sql` audita corretamente o schema existente. Nenhuma alteração DDL necessária pois a tabela `installment` já foi criada em V2. **Correto.**
- `@Version` na entidade `Transaction` (já existente, não regressão). `Installment` não tem `@Version` própria — adequado, pois o controle de concorrência ocorre na transação pai.

### Frontend
- Badge X/N exibido apenas em transações parceladas (`tx.installmentsTotal > 1`): **Correto.** Lançamentos à vista não exibem o badge.
- Lazy loading no expansion panel: `onInstallmentPanelOpened()` verifica `installmentsMap()[tx.id]` antes de fazer a requisição HTTP. **Correto.**
- Snackbar de erro em pt-br: `'Erro ao carregar parcelas.'` com `panelClass: 'snack--error'`. **Correto.**
- Cores do design system: badge usa `#b8a9d9` (lilás), destaque da parcela do mês usa `#7fc4a0` (verde-menta). **Correto.**
- Suporte a modo escuro: `:host-context(body.dark-theme)` (ThemeService) + `@media (prefers-color-scheme: dark)` (OS-level). Ambos presentes. **Correto.**

### Testes
- 12 casos de borda cobertos: TC-01 (antes do fechamento), TC-02 (após fechamento), TC-03 (exatamente no dia), TC-04/TC-05 (virada de ano), TC-06/TC-07 (arredondamento), TC-08 (parcela única), TC-09 (closingDay=31), TC-10 (12 parcelas), TC-EXTRA-01 (vínculo pai), TC-EXTRA-02 (dia 1 do mês).
- Soma das parcelas verificada em todos os casos relevantes. **Bem coberto.**
- 70/70 testes passando confirmado no checkpoint de QA.

---

## Problemas por Gravidade

### RECOMENDADO

#### R-01: Discrepância entre spec e código no limite exato do fechamento

**Arquivo:** `InstallmentService.java`, linha 41

**Descrição:**
O `CLAUDE.md` documenta: _"dia ≤ closing_day → mesmo mês; dia > closing_day → mês seguinte"_.
O código implementa: `purchaseDate.getDayOfMonth() < closingDay` (estritamente menor que).
Resultado: uma compra feita **exatamente no dia de fechamento** vai para o **mês seguinte** pelo código, mas deveria ir para o **mesmo mês** pela spec.

O TC-03 está alinhado com o código (não com a spec), o que significa que código e testes concordam, mas a documentação é divergente.

**Impacto:** Compras feitas no dia exato do fechamento caem no ciclo seguinte — comportamento provavelmente correto financeiramente (a maioria dos bancos fecha o ciclo no próprio dia), mas é preciso harmonizar o documento com a implementação ou vice-versa.

**Ação recomendada:** Decidir qual é o comportamento correto, atualizar `CLAUDE.md` para refletir `<` (não `≤`), ou ajustar o código para `<=`. Registrar como decisão técnica em `memory/decisions/`.

---

#### R-02: Algoritmo de reference_month incorreto quando closingDay > dias do mês

**Arquivo:** `InstallmentService.java`, linha 41

**Descrição:**
O algoritmo faz comparação inteira simples `purchaseDate.getDayOfMonth() < closingDay`. Quando `closingDay` é maior que o número de dias do mês (ex.: `closingDay=31` em abril/junho/setembro/novembro — meses com 30 dias), **nenhuma compra será tratada como "após o fechamento"**, porque todo dia do mês (máximo 30) é menor que 31.

**Exemplo concreto:**
- Cartão com `closingDay=31`, compra em 30 de abril
- `30 < 31 = true` → first month = abril
- O fechamento efetivo de abril com `closingDay=31` deveria ser 30 de abril (último dia), e uma compra no dia 30 deveria ir para o ciclo seguinte (maio)
- Resultado: compras em meses curtos com `closingDay > dias-do-mês` **sempre** entram no ciclo do mesmo mês, ignorando o fechamento efetivo

**Caso mais impactante:** `closingDay=31` em fevereiro (28/29 dias) ou abril/junho/setembro/novembro (30 dias).

**Correção sugerida:**
```java
int effectiveClosingDay = Math.min(closingDay, YearMonth.from(purchaseDate).lengthOfMonth());
YearMonth firstYearMonth = (purchaseDate.getDayOfMonth() < effectiveClosingDay)
        ? YearMonth.from(purchaseDate)
        : YearMonth.from(purchaseDate).plusMonths(1);
```

**Ação recomendada:** Corrigir antes da Sprint 05. Adicionar caso de teste TC-11: `closingDay=31`, compra em 30 de abril → first month deve ser maio (não abril).

---

#### R-03: Ausência de teste de integração para o endpoint de parcelas

**Descrição:**
Não existe `TransactionIntegrationTest` (ou similar) que cubra o endpoint `GET /api/transactions/{id}/installments`. Os testes existentes (`InstallmentServiceTest`) são unitários e cobrem apenas a lógica do service.

**Casos faltantes:**
- 401 sem token JWT
- 200 com lista de parcelas para transação parcelada
- 200 com lista vazia para transação à vista
- 404 para ID inexistente

**Ação recomendada:** Adicionar `TransactionInstallmentIntegrationTest` ou expandir suite existente de transações com pelo menos o caso 401.

---

### OPCIONAL

#### O-01: Frontend CardSummary tem campos não retornados pelo backend

**Arquivo:** `frontend/src/app/core/models/transaction.models.ts`

**Descrição:**
A interface `CardSummary` do frontend declara `closingDay: number` e `dueDay: number`, mas o `TransactionResponse.CardSummary` do backend retorna apenas `id` e `name`. Os campos `closingDay` e `dueDay` nunca são preenchidos — serão `undefined` em runtime.

**Impacto:** Sem impacto funcional atual (os campos não são usados na UI). Porém, é uma interface enganosa que pode causar bugs futuros se alguém os usar esperando valores válidos.

**Ação recomendada:** Remover `closingDay` e `dueDay` da interface frontend até que o backend os retorne, ou alinhar o backend para incluí-los.

---

#### O-02: N+1 queries em TransactionService.toResponse()

**Arquivo:** `TransactionService.java` — método `toResponse()`

**Descrição:**
Para cada `Transaction` com `cardId != null`, o método chama `cardRepository.findById()`. Na listagem por mês (`findByMonth()`), isso resulta em uma query extra por transação com cartão — potencial N+1.

**Impacto:** Baixo com volumes pequenos (casal com ~30 lançamentos/mês). Pode degradar com volumes maiores.

**Ação recomendada:** Adicionar `@ManyToOne(fetch = FetchType.EAGER)` na relação `card` da entidade `Transaction`, ou buscar as transações com `JOIN FETCH` no repository. Tratar na Sprint 05 ou no backlog de performance.

---

#### O-03: Estimativa de número de parcela pode ser imprecisa antes do carregamento

**Arquivo:** `transaction-list.component.ts` — método `estimateCurrentInstallmentNumber()`

**Descrição:**
A estimativa calcula o número de meses de diferença entre a data da compra e o mês atual. Mas o `referenceMonth` da primeira parcela pode ser o mês seguinte ao da compra (quando compra é após o fechamento). A estimativa pode exibir um número incorreto no badge antes de o painel ser expandido.

**Impacto:** Visual apenas — o badge pode mostrar `1/3` quando deveria mostrar `2/3`, até que o usuário expanda o painel. Após o carregamento, o valor correto é exibido.

**Ação recomendada:** Aceitar como limitação da estimativa sem carregamento, ou incluir o `referenceMonth` da primeira parcela no `TransactionResponse` para cálculo preciso.

---

## Resumo executivo

| Item | Gravidade | Status |
|------|-----------|--------|
| JWT exigido no endpoint | Segurança | ✓ OK |
| BigDecimal em valores monetários | Convenção | ✓ OK |
| Mensagens em pt-br | Convenção | ✓ OK |
| Camadas respeitadas | Convenção | ✓ OK |
| Schema via Flyway | Convenção | ✓ OK |
| Algoritmo reference_month (casos normais) | Lógica | ✓ OK |
| Arredondamento (floor + última parcela) | Lógica | ✓ OK |
| Virada de ano | Lógica | ✓ OK |
| installmentsTotal=1 → lista vazia | Lógica | ✓ OK |
| Limite exato do dia de fechamento (spec vs. código) | Lógica | ⚠ RECOMENDADO (R-01) |
| closingDay > dias do mês | Lógica | ⚠ RECOMENDADO (R-02) |
| Teste de integração do endpoint | Testes | ⚠ RECOMENDADO (R-03) |
| Frontend CardSummary com campos extras | Frontend | 💡 OPCIONAL (O-01) |
| N+1 em toResponse() | Performance | 💡 OPCIONAL (O-02) |
| Estimativa de parcela no badge | Frontend | 💡 OPCIONAL (O-03) |
