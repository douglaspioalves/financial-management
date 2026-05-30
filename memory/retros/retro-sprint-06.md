# Retrospectiva — Sprint 06
**Data:** 2026-05-30
**Conduzido por:** Scrum Master
**Formato:** Start / Stop / Continue + Ação concreta

---

## O que foi bem (Continue)

| Agente | O que funcionou |
|---|---|
| Backend | SettlementService implementado com todos os 5 casos (FIFTY_FIFTY, PERSON_A, PERSON_B, PROPORTIONAL, pending) corretos desde a primeira entrega. RoundingMode.HALF_UP consistente. |
| Backend | RecurringRuleService com job `@Scheduled` e idempotência: não duplica se job rodar 2x no mesmo dia. Soft delete correto (active=false). |
| QA | 10 cenários unitários do SettlementService cobrindo todos os edge cases críticos do CLAUDE.md. Fixtures Alice/Bob garantem ordenação alfabética determinística. |
| QA | Testes de integração TC-IT-01 a TC-IT-06 com meses distintos por cenário — sem interferência entre testes. |
| Revisor | Revisão aprofundada identificou REC-01 (NotBlank em description) antes do merge — evitou bug de idempotência silencioso. |
| Frontend | Settlement screen com Angular Signals + OnPush, navegação por mês, alerta de pendência proporcional, design system respeitado. |
| Frontend | Reutilização de CategoryService e PersonService existentes nas telas de recorrência — sem duplicação de código. |
| DBA | Índice composto `(type, date)` na tabela transaction via migration V9 — melhora performance do SettlementService. |
| Arquiteto | Decisão de acerto stateless (sem tabela settlement_record) registrada em memory/decisions/ antes do coding. |

---

## O que pode melhorar (Stop / Change)

| Agente | Problema observado | Impacto |
|---|---|---|
| Backend (agents) | Agentes paralelos commitaram código de settlement na branch de recorrência (commits f0096b0 e dedcb29 na branch errada). | Retrabalho: commit de remoção necessário + auditoria pós-merge |
| QA | Testes escritos contra API interna (método privado `calculateShares`) em vez da API pública `calculate(YearMonth)`. Precisou reescrever toda a classe. | 1 sessão extra de correção |
| QA | TransactionRepository com método duplicado `findIndividualIncomesByMonth` — QA adicionou versão sem JOIN FETCH enquanto backend já tinha adicionado com JOIN FETCH. | Erro de compilação pós-merge |
| DevOps | `mvnw` quebrado em ambiente remoto (`.mvn/wrapper/maven-wrapper.properties` ausente). Sistema `mvn` não executava Lombok sem `annotationProcessorPaths`. | 407 erros de compilação, 1 sessão extra para diagnosticar |
| DevOps | Tag `sprint-06` criada localmente mas servidor git rejeita push de tags (HTTP 403). | Rastreabilidade de release prejudicada no remote |
| Processo | Agentes trabalhando no repositório principal (não em worktrees) causaram conflitos de branch mid-sprint. | Múltiplos commits de "fix" e auditoria manual |

---

## O que começar a fazer (Start)

| Sugestão | Proposto por | Prioridade |
|---|---|---|
| Auditoria pós-merge: verificar `git log --name-only <merge-commit>` para confirmar que só os arquivos esperados estão no merge | Backend/DevOps | Alta |
| Cada agente deve rodar `mvn test` (ou `npm run build`) na própria branch antes de considerar pronto | QA/Backend | Alta |
| Manter `annotationProcessorPaths` para Lombok documentado como padrão do projeto (já adicionado ao pom.xml) | Backend | Média |
| Validar `docker compose up --build` com banco zerado uma vez por sprint — pendente para Sprint 07 | DevOps | Alta |

---

## Ações concretas para o próximo sprint

| Ação | Responsável | Prazo |
|---|---|---|
| `docker compose up --build` com banco zerado para validar a stack completa do Sprint 06 | DevOps | Sprint 07 - Dia 1 |
| Sprint 07: Exportação Excel/CSV + ajustes finais + tag v1.0.0 | Backend/Frontend | Sprint 07 |
| Cada backend agent deve verificar se o método que vai adicionar ao repositório já existe | Backend | Sprint 07 |

---

## Aprendizados para registrar em /memory/learnings/

- **Backend:** `personRepository.findAll()` retorna lista imutável — sempre `new ArrayList<>()` antes de `.sort()`. Verificar duplicatas em repositórios após merge de múltiplas branches.
- **Backend:** `@NotBlank` obrigatório em campos usados como chave de idempotência.
- **QA:** Testes de integração devem usar meses distintos por cenário de test para evitar interferência.
- **DevOps:** `annotationProcessorPaths` para Lombok necessário em ambiente remoto sem Maven Wrapper. Tag push HTTP 403 no servidor local — criar localmente é suficiente.
- **Processo:** Agentes paralelos devem usar worktrees isolados e validar branch correta antes de commitar.

---

## Humor do time (saúde do processo)

Sprint tecnicamente bem-sucedido — todas as funcionalidades entregues e testadas. Porém com mais retrabalho do que o desejável por problemas de coordenação entre agentes paralelos e incompatibilidade de ambiente de build. O algoritmo de acerto de contas (a story mais complexa do projeto) foi implementado corretamente desde o início, o que é o mais importante.

**Saúde:** 🟡 OK — entregou tudo, mas com atrito de processo médio-alto.
