# Retrospectiva — Sprint 01
**Data:** 2026-05-28
**Conduzido por:** Scrum Master
**Formato:** Start / Stop / Continue + Ação concreta

---

## O que foi bem (Continue)

| Agente | O que funcionou |
|---|---|
| Backend | JJWT 0.12.x configurado corretamente de primeira; GlobalExceptionHandler centralizando todos os erros de forma limpa |
| Frontend | Angular Material MD3 com design system aplicado; lazy loading e guards funcionais implementados corretamente |
| DBA | Schema completo com constraints, FKs e índices na primeira migration; seeds idempotentes com ON CONFLICT DO NOTHING |
| QA | Testes de integração encontraram bugs reais (403 vs 401) antes do merge; H2 em memória para testes isolados |
| Revisor | Encontrou `@JsonIgnore` faltando na senha e ausência do `AuthenticationEntryPoint` — ambos bloqueantes de segurança |
| Processo | Paralelismo entre agentes (DBA + Backend + Frontend simultâneos) foi eficaz; sprint entregue em 2 dias |

## O que pode melhorar (Stop / Change)

| Agente | Problema observado | Impacto |
|---|---|---|
| DevOps | Validação do `docker compose up` com banco zerado não foi feita — Docker Desktop offline | Não é possível garantir que migrations rodam limpas até validar |
| Processo | Sprint 02 foi implementado antes do Sprint 01 ser formalmente fechado | Documentação ficou defasada em relação ao código |
| Geral | JWT_SECRET padrão não era Base64 válido, causando falha na inicialização | Descoberto apenas na integração; deveria ter sido pego no Sprint 01 |

## O que começar a fazer (Start)

| Sugestão | Proposto por | Prioridade |
|---|---|---|
| Validar `docker compose up` antes de fechar cada sprint | DevOps/Processo | Alta |
| Manter documentação de sprint em sincronia com o código | Scrum Master | Alta |
| Usar valor Base64 como padrão de JWT_SECRET desde o início | Backend/DevOps | Média |

---

## Ações concretas para o próximo sprint

| Ação | Responsável | Prazo |
|---|---|---|
| Validar `docker compose up --build` com banco zerado | DevOps | Início do Sprint 02, Dia 1 |
| Sprint 02 Planning com backlog atualizado | Scrum Master | Início do Sprint 02 |
| Documentar decisão sobre exclusão de categorias | Arquiteto | Sprint 02, Dia 1–2 |

## Aprendizados para registrar em /memory/learnings/

- **Backend:** JWT_SECRET precisa ser Base64 válido OU o JwtService precisa de fallback UTF-8 (já implementado)
- **Backend:** Tabela `users` (não `user`) — palavra reservada no PostgreSQL
- **Frontend:** `@use '@angular/material'` deve ser a primeira linha do SCSS
- **DevOps:** Validar `docker compose up` com banco zerado antes do fechamento do sprint
- **Processo:** Fechar o sprint formalmente antes de iniciar o próximo

## Decisões tomadas nesta retro

- Manter a V5 migration com usuários de teste (alice + bob) como parte padrão do projeto — facilita onboarding e testes manuais.

---

## Humor do time (saúde do processo)

- Ritmo: ⚡ Acelerado (Sprint 01 entregue em 2 de 10 dias; Sprint 02 já implementado em paralelo)
- Qualidade: ✅ Alta (15/15 testes; revisão de segurança aplicada)
- Processo Scrum: ⚠️ Ajustes necessários (documentação atrasada em relação ao código)
