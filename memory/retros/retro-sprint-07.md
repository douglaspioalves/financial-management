# Retrospectiva — Sprint 07 (FINAL)
**Data:** 2026-05-30
**Conduzido por:** Scrum Master
**Formato:** Start / Stop / Continue + Ação concreta
**Natureza:** Retrospectiva FINAL do projeto — Sprint 07 encerra o roadmap v1.0.0

---

## O que foi bem (Continue)

| Agente | O que funcionou |
|---|---|
| Backend | ExportService implementado de forma limpa: ExportRow com 8 colunas, separação clara entre CSV e XLSX, validação de parâmetros com 400 pt-br, sem vazamento de campos sensíveis |
| Frontend | Todo o polimento UX (loading states, empty states, responsividade mobile) entregue em uma única sessão; botão de exportação com download automático funcionando |
| QA | FlowIntegrationTest TC-F01 a TC-F11 cobrindo o fluxo completo end-to-end, incluindo os 3 testes de /api/export exigidos pelo revisor |
| Revisor | Encontrou REC-01 (BigDecimal doubleValue no XLSX) e REC-02 (endpoint export sem testes) antes do merge — ambos corrigidos no mesmo sprint via fix/s07-export-fixes |
| DevOps | Merges de 4 branches (feature/s07-backend-export, feature/s07-frontend-ux, feature/s07-tests, fix/s07-export-fixes, fix/enable-disabled-integration-tests) para master sem nenhum conflito |
| Processo | Paralelismo de 3 agentes (Backend + Frontend + QA) funcionou conforme o modelo definido em CLAUDE.md; resultado final: 129 testes, 0 falhas, BUILD SUCCESS |

## O que pode melhorar (Stop / Change)

| Agente | Problema observado | Impacto |
|---|---|---|
| QA | Agente QA atingiu limite de sessão durante a implementação dos testes — trabalho foi salvo, mas gerou interrupção e necessidade de retomada | Risco de perda de contexto; mitigado pelo checkpoint de sessão |
| DevOps | Commits de infraestrutura (@Disabled nos testes, ajuste de .gitignore) foram feitos em branches erradas (feature/s07-frontend-ux e feature/s07-tests) e precisaram ser revertidos | Poluição do histórico com commits de revert; branches ficaram com mais commits do que o necessário |
| DevOps | BudgetIntegrationTest e DashboardIntegrationTest permaneceram com @Disabled por toda a duração do sprint, sendo removidos apenas pelo segundo DevOps via fix/enable-disabled-integration-tests | Testes ignorados podem mascarar regressões; a omissão passou despercebida por vários agentes |
| Infra | docker compose up --build não pôde ser validado em nenhum ponto do sprint — Docker daemon indisponível no ambiente sandbox | Item do Definition of Done não verificável; ficou como "pendente de validação manual" |
| Processo | Alguns campos do sprint-07.md permaneceram com status PENDENTE mesmo após as tarefas serem concluídas (revisão modo escuro, erros via toast, api.md) | Rastreabilidade comprometida — sprint-07.md não refletia o estado real do sprint |

## O que começar a fazer (Start)

| Sugestao | Proposto por | Prioridade |
|---|---|---|
| Commits de infra/chore (gitignore, @Disabled, CI) devem ir direto para uma branch chore/sprint-NN, nunca misturar com branches de feature | Processo | Alta |
| Antes de fechar um sprint, o Scrum Master deve auditar sprint-NN.md e reconciliar status com o git log — verificar cada tarefa PENDENTE contra os commits existentes | Scrum Master | Alta |
| Manter uma lista explícita de testes com @Disabled no sprint-NN.md para que qualquer agente possa identificar e remover ao longo do sprint, sem depender do DevOps final | QA / Processo | Media |
| Definir um critério claro para "docker compose nao validavel": registrar como impedimento desde o dia 1, nao como pendencia silenciosa no DoD | DevOps / Scrum Master | Media |

---

## Aprendizado retrospectivo sobre o projeto completo (Sprints 01 a 07)

Esta retrospectiva encerra o projeto Gestor de Gastos. Um balanço geral:

**O que funcionou de forma consistente ao longo dos 7 sprints:**
- Paralelismo de agentes (DBA + Backend + Frontend simultâneos) foi eficaz em todos os sprints onde foi aplicado.
- O agente revisor encontrou bugs reais antes do merge em todos os sprints em que participou — a etapa de revisão tem ROI comprovado.
- Testes de integração com banco H2 em memória provaram ser o padrão correto para este projeto: isolamento, velocidade, cobertura de regras de negócio.
- O modelo de branches por sprint (feature/sNN-xxx) manteve o histórico rastreável e os merges limpos.

**O que se repetiu como problema:**
- Validacao do docker compose ficou bloqueada por falta de Docker daemon no ambiente sandbox em todos os sprints. Esse impedimento nunca foi resolvido de forma definitiva.
- Documentacao de sprint (sprint-NN.md) sistematicamente ficou desatualizada em relacao ao codigo real. O Scrum Master deveria ter auditado esses arquivos mais ativamente.
- Commits de infra/chore foram misturados com branches de feature em mais de um sprint.

---

## Acoes concretas (relevantes para manutencao futura)

| Acao | Responsavel | Prazo |
|---|---|---|
| Validar docker compose up --build com banco zerado em ambiente com Docker disponivel | DevOps (humano) | Pos-sprint |
| Criar tag v1.0.0 no git apos validacao manual do Docker | DevOps | Pos-sprint |
| Revisar modo escuro em todas as telas (OPT pendente do revisor) | Frontend | Backlog |
| Tratar erros de API com toasts/snackbar em todas as telas restantes | Frontend | Backlog |
| Completar docs/api.md com todos os endpoints | Arquiteto | Backlog |
| Considerar usar nomes reais (Person.name) no lugar de "Pessoa A/B" no export (OPT-01 do revisor) | Backend | Backlog |

## Aprendizados para registrar em /memory/learnings/

- **Backend:** BigDecimal nunca deve virar double mesmo em serializacao para bibliotecas de terceiros (Apache POI) — usar setCellValue(String) com toPlainString() e ajustar o tipo de celula XLSX conforme necessario.
- **Backend:** Ao adicionar um endpoint novo, garantir que pelo menos 3 testes o cubram: autenticacao (401), caso de sucesso (200), parametro invalido (400) — padrao estabelecido pelo revisor no Sprint 07.
- **DevOps:** Commits de infraestrutura (@Disabled, .gitignore, dependencias) devem ir em branch chore/sprint-NN separada das branches de feature, para nao gerar reverts desnecessarios.
- **DevOps:** Testes com @Disabled devem ser rastreados explicitamente no sprint-NN.md e removidos assim que o motivo do disable for resolvido — nao deixar para o DevOps final.
- **Negocio/PM:** O modelo de dados e as regras de negocio (parcelamento, proporcional, acerto) foram estabilizados no Sprint 03 e nao sofreram alteracoes nos sprints seguintes — a estabilidade do modelo foi um fator critico de sucesso.

## Decisoes tomadas nesta retro

- O projeto encerra o Sprint 07 com 129 testes passando, exportacao CSV/XLSX funcionando, fluxo completo coberto por testes de integracao ponta a ponta e UX responsiva. A versao v1.0.0 esta pronta para validacao final em ambiente com Docker disponivel.
- Nao ha novos sprints planejados. Itens opcionais (modo escuro completo, nomes reais no export, api.md) ficam no backlog para manutencao.

---

## Humor do time (saude do processo)

- Ritmo: Acelerado (Sprint 07 entregue em 1 dia util com 5 branches mergeadas)
- Qualidade: Alta (129 testes, 0 falhas; revisor aplicado com resultado concreto)
- Processo Scrum: Ajustes necessarios (documentacao de sprint desatualizada; commits de infra em branches erradas; docker nao validado)
