---
name: Checkpoint Sprint 01 — Estado para retomada
description: Snapshot completo do estado do projeto ao fim da sessão de 2026-05-27 para retomada sem perda de contexto
type: project
---

# Checkpoint — Sprint 01 — 2026-05-27

**Why:** Sessão encerrada após conclusão das stories de código. Guardar estado para retomada.
**How to apply:** Leia este arquivo no início da próxima sessão antes de qualquer ação.

---

## Estado das stories

| Story | Status | Branch |
|---|---|---|
| S-01-01 Esqueleto + Docker Compose | ✅ CONCLUÍDO | feature/esqueleto-monorepo |
| S-01-02 Migrations iniciais | ✅ CONCLUÍDO (código) | feature/migrations-iniciais |
| S-01-03 Cadastro/login JWT | ✅ CONCLUÍDO (código) | feature/auth-jwt |
| S-01-04 Telas login/cadastro | ✅ CONCLUÍDO | feature/git-actions |

## O que FALTA para fechar o Sprint 01

### 1. Testes de integração — Agente QA
Story S-01-03 tem tarefa de QA pendente. O agente QA precisa escrever:
- Teste de integração `POST /api/auth/register` — cenários: sucesso (201), e-mail duplicado (409), campos inválidos (400)
- Teste de integração `POST /api/auth/login` — cenários: sucesso (200 + JWT), credenciais erradas (401)
- Confirmar que `GET /api/health` responde sem autenticação
- Confirmar que endpoint protegido retorna 401 sem JWT
- Usar `@SpringBootTest` + `MockMvc` + banco H2 em memória
- Rodar `./mvnw test` e garantir BUILD SUCCESS

### 2. Validação Docker Compose — Agente DevOps
Última tarefa da S-01-02 (ainda PENDENTE):
- Rodar `docker compose up --build` num ambiente com banco zerado
- Verificar: Flyway aplica V1, V2, V3 sem erros; seeds (2 persons + 12 categorias) presentes
- Verificar: `GET /api/health` responde 200
- Verificar: Frontend abre em localhost:4200
- Anotar qualquer ajuste necessário

### 3. Revisão de segurança — Agente Revisor
- Revisar `SecurityConfig`, `JwtAuthenticationFilter`, `AuthController`
- Confirmar: nenhum endpoint além de /api/health e /api/auth/** público
- Confirmar: password_hash nunca aparece em nenhum DTO de resposta
- Confirmar: JWT_SECRET não está hardcoded no código (só em .env.example com placeholder)

### 4. Merge das branches → main
Após QA e revisão aprovados, mergear na ordem:
1. `feature/esqueleto-monorepo` → main
2. `feature/migrations-iniciais` → main (ou rebased em cima de 1)
3. `feature/auth-jwt` → main
4. `feature/git-actions` → main (tem CI + S-01-04)

### 5. Cerimônias de fechamento — Scrum Master
- Sprint Review: o que foi entregue vs. planejado
- Retrospectiva: o que funcionou, o que melhorar
- Criar `memory/reviews/review-sprint-01.md`
- Criar `memory/retros/retro-sprint-01.md`
- Atualizar `memory/learnings/*.md`
- Criar tag `sprint-01` no git após merge

---

## Arquitetura atual — o que existe

### Backend (`backend/`)
```
src/main/java/com/gastos/
  BackendApplication.java
  config/
    SecurityConfig.java          — JWT filter, BCrypt, DaoAuthenticationProvider
    GlobalExceptionHandler.java  — erros de validação (400) e negócio (409) em pt-br
  controller/
    HealthController.java        — GET /api/health → {"status":"ok"}
    AuthController.java          — POST /api/auth/register, POST /api/auth/login
  domain/
    User.java                    — entity JPA @Table("users"), implements UserDetails
  dto/auth/
    RegisterRequest.java         — name, email, password (Bean Validation pt-br)
    LoginRequest.java            — email, password
    AuthResponse.java            — token, type="Bearer", name, email
  repository/
    UserRepository.java          — findByEmail, existsByEmail
  security/
    JwtService.java              — JJWT 0.12.6: generateToken, extractUsername, isTokenValid
    JwtAuthenticationFilter.java — OncePerRequestFilter: valida Bearer token
    UserDetailsServiceImpl.java  — implementa UserDetailsService
  service/
    AuthService.java             — register (BCrypt) + login (AuthenticationManager)
src/main/resources/
  application.properties         — datasource via env vars, Flyway, jwt.secret, jwt.expiration
  db/migration/
    V1__baseline.sql             — vazio (inicializa Flyway)
    V2__initial_schema.sql       — 8 tabelas, FKs, CHECK constraints, 7 índices
    V3__seeds.sql                — 2 persons + 12 categorias, UUIDs fixos, idempotente
```

### Frontend (`frontend/src/app/`)
```
app.ts / app.html / app.scss    — shell: router-outlet + botão flutuante tema
app.routes.ts                   — / → /auth/login; /auth (lazy); /dashboard (lazy+authGuard)
app.config.ts                   — provideHttpClient(withInterceptors([authInterceptor]))
auth/
  auth.routes.ts                — login (guestGuard), register (guestGuard)
  login/                        — LoginComponent: reactive form, Material outline fields
  register/                     — RegisterComponent: reactive form, 3 campos
core/
  models/auth.models.ts         — LoginRequest, RegisterRequest, AuthResponse, CurrentUser
  services/
    auth.service.ts             — login(), register(), logout(), currentUser (signal), isAuthenticated (computed)
    health.service.ts           — mantido
  guards/
    auth.guard.ts               — authGuard funcional
    guest.guard.ts              — guestGuard funcional
  interceptors/
    auth.interceptor.ts         — injeta Authorization: Bearer <token>
  theme/
    theme.service.ts            — isDark (signal), toggle(), localStorage + matchMedia
dashboard/
  dashboard.component.ts        — placeholder: saudação + logout
src/styles.scss                 — CSS variables design system, Angular Material MD3 (azure palette)
```

### Infra
```
docker-compose.yml              — postgres:16-alpine + backend + frontend (nginx:4200)
.env.example                    — DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET
.github/workflows/ci.yml        — CI: backend (Maven/Java21) + frontend (Node/Angular build)
```

---

## Decisões técnicas tomadas nesta sessão

| Decisão | Motivo |
|---|---|
| Tabela `users` (não `user`) | `user` é palavra reservada no PostgreSQL |
| JJWT 0.12.6 (API nova: `Jwts.parser().verifyWith()`) | Spring Boot 3 requer JJWT 0.11.5+ |
| `User` implementa `UserDetails` diretamente | Evita classe wrapper desnecessária |
| Seeds com UUIDs fixos (`...0001`, `...0002`) | Estabilidade em testes de integração |
| `ON CONFLICT DO NOTHING` nos seeds | Migrations idempotentes no docker compose up |
| `reference_month` e `month` com CHECK `DAY = 1` | Invariante garantida no banco, não só na aplicação |
| `chk_transaction_card_credit` no schema | Garante card_id somente com CREDIT no nível do banco |
| Angular: signals + functional guards/interceptors | Padrão moderno Angular 17+ / Angular 21 |
| `guestGuard` separado do `authGuard` | Evita que usuário logado acesse /auth/login |
| Feature branches por story | Facilita revisão; merge sequencial para main |
