# Checkpoint Backend — API de Transactions
**Data:** 2026-05-27
**Sprint:** 02, Wave 2
**Branch:** feature/s02-transactions-api
**Agente:** Backend

## Status: CONCLUIDO

## O que foi implementado

### Enums (domain/)
- `TransactionType`: EXPENSE, INCOME
- `PaymentMethod`: CASH, DEBIT, CREDIT, PIX, TRANSFER
- `SplitRule`: PERSON_A, PERSON_B, FIFTY_FIFTY, PROPORTIONAL

### Entidade
- `Transaction` com todos os campos do schema V2, @Version para optimistic locking, @PrePersist para createdAt

### Repositório
- `TransactionRepository` com `findByDateBetweenOrderByDateAscCreatedAtAsc`

### DTOs (dto/transaction/)
- `TransactionResponse` com records internos CategorySummary e PersonSummary
- `CreateTransactionRequest` com Bean Validation (mensagens pt-br)
- `UpdateTransactionRequest` com campo version obrigatório

### Service
- `TransactionService` com:
  - Validação: parcelamento bloqueado (installmentsTotal > 1 = 400)
  - Validação: categoria ativa obrigatória (404)
  - Validação: compatibilidade tipo lançamento x tipo categoria (422 via IllegalStateException)
  - Validação: participante pagador obrigatório (404)
  - Validação: cartão obrigatório para CREDIT, proibido para outros métodos (400)
  - Optimistic locking via comparação de version
  - findByMonth com parse e validação de yyyy-MM

### Controller
- `TransactionController` expondo 5 endpoints conforme docs/api.md

### GlobalExceptionHandler atualizado
- IllegalArgumentException -> 400 (era 409, agora correto)
- IllegalStateException -> 422 (novo, para categoria incompatível)
- EntityNotFoundException -> 404 (novo)
- ObjectOptimisticLockingFailureException -> 409 (novo)

## Compilação
- `./mvnw compile` retorna BUILD SUCCESS sem erros

## Endpoints expostos
- POST /api/transactions (201)
- GET /api/transactions?month=yyyy-MM (200)
- GET /api/transactions/{id} (200)
- PUT /api/transactions/{id} (200)
- DELETE /api/transactions/{id} (204)

## Observacoes
- Category, CategoryType, Person, CategoryRepository e PersonRepository foram incluidos
  nesta branch pois sao dependencias necessarias para compilar Transaction.
  Esses arquivos tambem existem em feature/s02-categories-api (mesma implementacao).
- Os commits anteriores (cb6ef65, 74439a0, 1a4f7e1) registrados na branch tinham
  arquivos fisicamente ausentes do workspace — foi necessario recriar todos os arquivos.
