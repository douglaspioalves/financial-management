# Plano de Desenvolvimento — Gestor de Gastos

Versão legível para o Claude Code. O documento completo (formatado) está em `plano.docx`.

## 1. Visão geral

App web para controle de gastos e receitas entre duas pessoas. Dois logins
independentes compartilhando a mesma base; sem vínculo de "casal". As duas pessoas
são `Person` (rótulos) usadas em "quem pagou" e na divisão. Diferencial: acerto de
contas com parcelas de cartão entrando mês a mês.

## 2. Escopo (v1)

- Autenticação: cadastro/login por email+senha (JWT). Dois logins, sem convite.
- Lançamentos: CRUD com valor, data, tipo (despesa/receita), categoria, descrição,
  quem pagou, forma de pagamento, regra de divisão.
- Categorias personalizáveis (com conjunto inicial).
- Cartões com fechamento/vencimento; compra parcelada gera parcelas automaticamente.
- Orçamento mensal por categoria; lançamentos recorrentes.
- Acerto de contas: quem deve quanto a quem (parcelas no mês em que caem).
- Dashboard: total do mês, gráfico por categoria, comparativo mês a mês.
- Extra: exportação Excel/CSV.

Fora da v1: login social, multi-moeda, app nativo, anexos de comprovante.

## 3. Modelo de dados

Ver `CLAUDE.md` (seção Modelo de dados) — fonte de verdade.
Entidades: User, Person, Category, Card, Transaction, Installment, Budget, RecurringRule.

## 4. Arquitetura

Monorepo: `backend/` (Spring Boot, camadas controller/service/repository/domain,
Flyway), `frontend/` (Angular por feature), `docker-compose.yml`, `docs/`.
Boas práticas: API documentada (Swagger), migrations versionadas, validação com
mensagens pt-br, BCrypt + JWT, testes nos cálculos de parcela e acerto, optimistic locking.

## 5. Roadmap (fatias verticais)

0. Esqueleto rodando no Docker (backend health + Angular + Postgres).
1. Cadastro e login (JWT) dos dois usuários.
2. CRUD de lançamentos + categorias.
3. Cartões + compras parceladas (geração de Installment).
4. Dashboard.
5. Orçamento + recorrência.
6. Acerto de contas.
7. Exportação Excel/CSV (extra).

## 6. Fluxo de agentes

Papéis em `.claude/agents/`: arquiteto, backend, frontend, banco, qa, devops, revisor.
Ciclo por fatia: arquiteto detalha → banco prepara migrations → backend implementa →
qa testa → frontend constrói → revisor revisa → você aprova → devops sobe/comita.

## 7. Idioma

Código/identificadores em inglês. Interface, docs, comentários e commits em pt-br.
Mensagens de erro expostas ao usuário em pt-br.

## 8. Próximos passos

1. Confirmar versões das ferramentas (Windows).
2. `git init` e primeiro commit com este esqueleto de docs/agentes.
3. Iniciar Fatia 0 com o agente devops.
4. Seguir o roadmap, uma fatia por vez, validando cada uma.
