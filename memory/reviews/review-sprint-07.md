# Revisão de Segurança — Sprint 07

**Data:** 2026-05-30  
**Revisor:** Agente Revisor de Código  
**Branches:** feature/s07-backend-export, feature/s07-frontend-ux, feature/s07-tests  
**Veredito:** APROVADO COM RESSALVAS

---

## Resumo executivo

O Sprint 07 implementa exportação CSV/XLSX e melhorias de UX (loading states, empty states, responsividade). Não há item bloqueante de segurança. Dois itens recomendados e um opcional devem ser tratados antes ou logo após o merge.

---

## O que está OK

- **Autenticação JWT no endpoint de export:** `SecurityConfig` mantém `anyRequest().authenticated()` sem nenhuma exceção introduzida para `/api/export`. O endpoint está protegido.
- **Validação do parâmetro `format`:** o controller rejeita qualquer valor fora de `"csv"` e `"xlsx"` com 400 e mensagem em pt-br.
- **Sem path traversal no `Content-Disposition`:** o nome do arquivo é gerado de `YearMonth.toString()` (resultado de parse do parâmetro `month`), não de concatenação direta da string da URL. Um input malformado em `month` resulta em erro 400 antes de chegar ao header.
- **BigDecimal no CSV:** `row.amount().toPlainString()` — sem cast.
- **Queries corretas por mês:** `findCashExpensesByMonth` filtra por `BETWEEN start AND end`; `findExpenseInstallmentsByMonth` filtra por `referenceMonth = start` (primeiro dia do mês) — coerente com a convenção do modelo.
- **Sem vazamento de campos sensíveis:** `ExportRow` expõe apenas os 8 campos do contrato; nenhum `password_hash` ou campo de `User` aparece.
- **Headers `Content-Disposition` corretos** para CSV (`text/csv;charset=UTF-8`) e XLSX (MIME OOXML correto).
- **Autenticação no frontend:** `authInterceptor` global injeta `Authorization: Bearer` em todas as requisições HTTP, incluindo o serviço de export.
- **Labels pt-br:** tipo ("Receita"/"Despesa"), divisão ("50/50", "Pessoa A", "Pessoa B", "Proporcional"), cabeçalhos do arquivo — todos em pt-br.
- **8 colunas cobertas:** Data, Descrição, Categoria, Quem Pagou, Tipo, Valor, Divisão, Parcela.
- **Mensagens de erro em pt-br** em todos os `snackBar` do componente Angular.
- **Design system respeitado:** componente usa `var(--color-bg)`, `var(--color-income)`, `var(--color-expense)`, `var(--font-display)`, cantos arredondados, modo claro/escuro.
- **Dependência Apache POI:** versão 5.3.0 sem CVE crítico conhecido.
- **Testes de integração ponta a ponta:** 8 testes (TC-F01 a TC-F08) cobrindo registro, login, lançamentos, parcelamento, acerto de contas, listagem e dashboard.

---

## Problemas encontrados

### RECOMENDADO (2 itens)

**REC-01 — `doubleValue()` na célula XLSX viola convenção do projeto**  
Arquivo: `backend/src/main/java/com/gastos/service/ExportService.java`, linha 105  
```java
// Valor como número (BigDecimal → double é aceitável apenas para serialização XLS)
dataRow.createCell(5).setCellValue(row.amount().doubleValue());
```
O comentário justifica o uso, mas `CLAUDE.md` é explícito: "Valores monetários: `BigDecimal` (nunca `double`/`float`)". A API do Apache POI aceita `String` via `setCellValue(String)` — basta formatar com `row.amount().toPlainString()` e o Excel reconhece como número se a coluna for formatada com `CellType.NUMERIC` e o valor parseado via `setCellValue(double)` apenas com escala controlada. Alternativa: usar `setCellValue(row.amount().toPlainString())` e não perder precisão.  
Risco real: baixo para valores com até 2 casas decimais (dinheiro), mas a convenção deve ser seguida.

**REC-02 — Endpoint `/api/export` não coberto por nenhum teste**  
Arquivo: `backend/src/test/java/com/gastos/FlowIntegrationTest.java`  
Os 8 testes cobrem os fluxos de negócio, mas nenhum valida o endpoint de export. Mínimo recomendado: um teste que verifique 401 sem token, um que verifique 200 com CSV não-vazio para um mês com dados, e um que verifique 400 para `format=pdf`.

---

### OPCIONAL (1 item)

**OPT-01 — Labels "Pessoa A / Pessoa B" são genéricos**  
Arquivo: `ExportService.java`, método `splitRuleLabel`  
O export mostra "Pessoa A" e "Pessoa B" em vez dos nomes reais cadastrados (ex.: "Ana" e "Carlos"). Quem abrir o arquivo sem contexto não saberá a quem se refere. Sugestão: injetar os `Person` correspondentes e usar seus nomes reais na coluna Divisão, ou ao menos adicionar uma aba de legenda no XLSX.

---

## Checklist de segurança (9 pontos)

| # | Ponto | Resultado |
|---|-------|-----------|
| 1 | `/api/export` protegido por JWT | PASS — `anyRequest().authenticated()` sem exceção adicionada |
| 2 | Validação de `month` contra path traversal | PASS — parâmetro parsed como `YearMonth`; string da URL nunca chega ao header |
| 3 | `format` validado (somente csv/xlsx) | PASS — validação explícita com 400 pt-br para qualquer outro valor |
| 4 | Dados de outro usuário no export | N/A — base compartilhada por design; sem multi-tenant; comportamento esperado |
| 5 | `Content-Disposition` usa valor parseado, não string bruta | PASS — `month.toString()` pós-parse; input inválido rejeita antes |
| 6 | Valores monetários sem cast double/float | PARCIAL — CSV usa `toPlainString()` (OK); XLSX usa `doubleValue()` (viola convenção — ver REC-01) |
| 7 | Query do export filtra apenas o mês solicitado | PASS — `BETWEEN start AND end` para transações; `referenceMonth = start` para parcelas |
| 8 | 8 colunas presentes no export | PASS — Data, Descrição, Categoria, Quem Pagou, Tipo, Valor, Divisão, Parcela |
| 9 | Labels de divisão em pt-br | PASS — "50/50", "Pessoa A", "Pessoa B", "Proporcional" |

---

## Conclusão

Não há item bloqueante de segurança. O merge pode prosseguir após tratamento dos dois itens **RECOMENDADO** (idealmente REC-02 ainda neste sprint, REC-01 na mesma PR ou em fix imediato). O item **OPCIONAL** pode ser agendado para backlog.
