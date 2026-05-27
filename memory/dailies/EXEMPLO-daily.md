# Daily — EXEMPLO (como preencher)
> Este arquivo é um exemplo de como registrar uma daily.
> Crie o arquivo do dia real com o nome YYYY-MM-DD.md em memory/dailies/.

**Sprint:** 01 | **Dia do sprint:** 1/10
**Agentes ativos:** DevOps, DBA

---

## 🌅 MANHÃ — Planejamento do dia

### DevOps
- **O que vou fazer hoje:**
  - [ ] Criar projeto Spring Boot via Spring Initializr com as dependências corretas
  - [ ] Criar projeto Angular com Angular Material
  - [ ] Escrever estrutura inicial do docker-compose.yml
- **Dependências/bloqueios antecipados:** Nenhum — é o início do projeto.

### DBA
- **O que vou fazer hoje:**
  - [ ] Aguardar esqueleto do DevOps para validar conexão com Postgres
  - [ ] Preparar rascunho do V1__initial_schema.sql enquanto aguarda
- **Dependências/bloqueios antecipados:** Depende de S-01-01 estar minimamente pronto.

### Backend
- **O que vou fazer hoje:**
  - [ ] Acompanhar estrutura criada pelo DevOps; sem implementação de feature hoje
- **Dependências/bloqueios antecipados:** —

### Frontend
- **O que vou fazer hoje:**
  - [ ] Estudar o design-system.html aprovado para preparar o tema Angular Material
- **Dependências/bloqueios antecipados:** —

---

## 🌆 FIM DO DIA — Fechamento

### DevOps
- **O que foi feito:**
  - [x] Projeto Spring Boot criado com Web, JPA, Security, Validation, Flyway, PostgreSQL
  - [x] Projeto Angular criado com Angular Material
  - [x] docker-compose.yml com postgres, backend e frontend (sem health check ainda)
- **O que ficou para amanhã:**
  - [ ] Implementar GET /api/health
  - [ ] Criar .env.example
  - [ ] README inicial
- **Impedimentos:** Versão do Node no Windows precisou ser atualizada para 20+ (30min de delay).
- **Status das stories:** S-01-01: EM ANDAMENTO (70%)

### DBA
- **O que foi feito:**
  - [x] Rascunho do V1__initial_schema.sql com todas as tabelas
- **O que ficou para amanhã:**
  - [ ] Revisar com Arquiteto e finalizar constraints
  - [ ] Escrever V2__seeds.sql
- **Impedimentos:** —

### Backend
- **O que foi feito:** Sem entrega hoje (aguardando esqueleto).
- **O que ficou para amanhã:** Iniciar após DevOps finalizar health endpoint.
- **Impedimentos:** —

### Frontend
- **O que foi feito:**
  - [x] Design system estudado; tokens de cor e tipografia mapeados para o Material
- **O que ficou para amanhã:**
  - [ ] Criar custom theme Angular Material
- **Impedimentos:** —

---

## ⚠️ Impedimentos do dia

| Impedimento | Agente | Impacto | Ação |
|---|---|---|---|
| Node.js precisou atualização no Windows | DevOps | 30min delay | Resolvido; adicionar ao README de setup |

## 📊 Progresso do sprint (atualizado)

- Stories concluídas hoje: nenhuma
- Stories em andamento: S-01-01 (70%), S-01-02 (30% — rascunho)
- Stories bloqueadas: nenhuma
- Ritmo: ✅ No prazo para o sprint
