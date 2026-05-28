# Gestor de Gastos

Aplicacao web para controle de gastos e receitas entre duas pessoas, com acerto de
contas e suporte a compras parceladas no cartao de credito.

---

## Comando rapido para subir tudo

```powershell
docker compose up --build
```

Frontend: http://localhost:4200 | Backend: http://localhost:8080

---

## Visao geral

- **Dois logins independentes** que compartilham a mesma base de dados.
- Lancamentos de despesas e receitas, com categorias, cartoes e parcelas.
- Acerto de contas mensal: quem deve quanto a quem (parcelas entram mes a mes).
- Orcamento por categoria, lancamentos recorrentes e dashboard.

## Stack

- Backend: Java 21 + Spring Boot 3, PostgreSQL, Flyway, JWT.
- Frontend: Angular + Angular Material (modos claro e escuro).
- Infra local: Docker Compose.

---

## Pre-requisitos (Windows)

Instale na sua maquina de desenvolvimento. Validar versoes no PowerShell:

```powershell
java -version
node -v ; npm -v
docker --version ; docker compose version
git --version
```

| Ferramenta | Versao minima | Download |
|---|---|---|
| Docker Desktop | qualquer recente | https://www.docker.com/products/docker-desktop/ |
| Git | qualquer recente | https://git-scm.com/download/win |
| JDK 21 (opcional — so para dev local) | 21 | https://adoptium.net/ |
| Node.js (opcional — so para dev local) | 20+ | https://nodejs.org/ |

> Para apenas **rodar** o projeto, voce so precisa de Docker Desktop e Git.
> JDK e Node sao necessarios apenas se quiser rodar backend ou frontend fora do Docker.

---

## Como rodar com Docker (recomendado)

### 1. Clone o repositorio

```powershell
git clone <url-do-repositorio>
cd financial-management
```

### 2. Configure as variaveis de ambiente

```powershell
# Copie o arquivo de exemplo
Copy-Item .env.example .env

# Edite o .env se quiser mudar senhas (opcional para desenvolvimento local)
notepad .env
```

O arquivo `.env` ja vem configurado para funcionar localmente. Nunca commite o `.env` real.

### 3. Suba tudo com um comando

```powershell
docker compose up --build
```

Aguarde os tres servicos iniciarem. Voce vera no terminal:
- `gastos-postgres` — banco pronto
- `gastos-backend` — Spring Boot iniciado na porta 8080
- `gastos-frontend` — nginx servindo o Angular na porta 4200

### 4. Acesse no navegador

- Frontend: http://localhost:4200
- Backend (health check): http://localhost:8080/api/health

Se o frontend exibir "Backend conectado", tudo esta funcionando.

### 5. Credenciais de teste

Após o primeiro `docker compose up --build`, os seguintes usuários já estarão disponíveis para login:

- `alice@example.com` / `senha123`
- `bob@example.com` / `senha123`

Você também pode usar `POST /api/auth/register` para criar outros logins.

---

## Desenvolvimento local (sem Docker)

### Backend

Requer: JDK 21 instalado e um PostgreSQL rodando (pode ser o do Docker Compose).

```powershell
# Subir apenas o banco com Docker
docker compose up postgres -d

# Rodar o backend em modo desenvolvimento
cd backend
./mvnw spring-boot:run
```

O backend sobe em http://localhost:8080.

### Frontend

Requer: Node.js 20+ e Angular CLI instalados.

```powershell
# Instalar Angular CLI globalmente (uma vez)
npm install -g @angular/cli

# Instalar dependencias do projeto (primeira vez)
cd frontend
npm install

# Rodar em modo desenvolvimento (com proxy para o backend local)
npm start
```

O frontend sobe em http://localhost:4200 com hot-reload.

---

## Comandos uteis

```powershell
# Subir tudo (build + start)
docker compose up --build

# Subir em background
docker compose up --build -d

# Ver logs dos servicos
docker compose logs -f
docker compose logs -f backend
docker compose logs -f frontend

# Parar tudo
docker compose down

# Parar e remover volumes (apaga o banco — cuidado!)
docker compose down -v

# Rodar testes do backend
cd backend
./mvnw test

# Verificar status dos containers
docker compose ps
```

---

## Estrutura do projeto

```
financial-management/
├── backend/               # Spring Boot (Maven)
│   ├── src/
│   │   └── main/
│   │       ├── java/com/gastos/
│   │       │   ├── controller/   # Endpoints REST
│   │       │   ├── service/      # Logica de negocio
│   │       │   ├── repository/   # Acesso ao banco
│   │       │   ├── domain/       # Entidades JPA
│   │       │   ├── dto/          # Objetos de entrada/saida
│   │       │   └── config/       # Configuracoes (Security, etc.)
│   │       └── resources/
│   │           ├── application.properties
│   │           └── db/migration/ # Migrations Flyway
│   └── Dockerfile
├── frontend/              # Angular + Angular Material
│   ├── src/
│   │   └── app/
│   │       └── core/services/    # Servicos HTTP
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml     # Postgres + backend + frontend
├── .env.example           # Variaveis de ambiente (modelo)
├── .gitignore
├── CLAUDE.md              # Contexto do projeto para o Claude Code
└── docs/                  # Plano, design system, contrato da API
```

---

## Desenvolvimento com Claude Code

Este projeto e desenvolvido em fatias verticais com apoio de agentes. Veja `CLAUDE.md`
e a pasta `.claude/agents/`. Trabalhe uma fatia por vez, validando antes de seguir.

Documentacao em `docs/`:
- `plano.md` — plano completo (escopo, modelo de dados, roadmap).
- `design-system.html` — referencia visual aprovada (abra no navegador).
- `api.md` — contrato da API (mantido pelo arquiteto).
- `decisoes.md` — registro de decisoes de arquitetura.
