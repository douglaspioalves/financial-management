-- =============================================================================
-- V2__initial_schema.sql
-- Gestor de Gastos — Schema completo inicial
--
-- Cria as tabelas: users, person, category, card, transaction,
--                  installment, budget, recurring_rule
-- Cria todos os índices obrigatórios
-- NÃO insere dados (seeds ficam em V3)
--
-- Banco: PostgreSQL 16
-- Gerado em: 2026-05-26
-- =============================================================================

-- -----------------------------------------------------------------------------
-- users
-- Representa os dois logins da aplicação. Não confundir com Person (participante).
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- -----------------------------------------------------------------------------
-- person
-- Participante/rótulo dos lançamentos (quem pagou, para quem se atribui a despesa).
-- Não possui login. A aplicação inicia com exatamente dois registros (via V3).
-- version: controle de concorrência otimista (optimistic locking).
-- -----------------------------------------------------------------------------
CREATE TABLE person (
    id      UUID         NOT NULL DEFAULT gen_random_uuid(),
    name    VARCHAR(100) NOT NULL,
    color   VARCHAR(20)  NOT NULL,
    version BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_person PRIMARY KEY (id)
);

-- -----------------------------------------------------------------------------
-- category
-- Categoria de lançamento. type indica se é usada para despesas, receitas ou ambos.
-- version: controle de concorrência otimista.
-- -----------------------------------------------------------------------------
CREATE TABLE category (
    id      UUID        NOT NULL DEFAULT gen_random_uuid(),
    name    VARCHAR(100) NOT NULL,
    type    VARCHAR(10)  NOT NULL,
    color   VARCHAR(20)  NOT NULL,
    version BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT chk_category_type CHECK (type IN ('EXPENSE', 'INCOME', 'BOTH'))
);

-- -----------------------------------------------------------------------------
-- card
-- Cartão de crédito vinculado a um participante (person).
-- closing_day: dia do fechamento da fatura (1–31).
-- due_day: dia do vencimento da fatura (1–31).
-- Usado na lógica de cálculo do reference_month das parcelas.
-- version: controle de concorrência otimista.
-- -----------------------------------------------------------------------------
CREATE TABLE card (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    owner_person_id UUID         NOT NULL,
    name            VARCHAR(100) NOT NULL,
    closing_day     INT          NOT NULL,
    due_day         INT          NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_card PRIMARY KEY (id),
    CONSTRAINT fk_card_owner_person FOREIGN KEY (owner_person_id) REFERENCES person (id),
    CONSTRAINT chk_card_closing_day CHECK (closing_day BETWEEN 1 AND 31),
    CONSTRAINT chk_card_due_day     CHECK (due_day     BETWEEN 1 AND 31)
);

-- -----------------------------------------------------------------------------
-- transaction
-- Lançamento financeiro (despesa ou receita).
-- card_id só deve ser preenchido quando payment_method = 'CREDIT'.
-- installments_total >= 1 (1 = compra à vista, N > 1 = parcelada).
-- Para compras parceladas, a tabela installment armazena cada parcela.
-- version: controle de concorrência otimista.
-- -----------------------------------------------------------------------------
CREATE TABLE transaction (
    id                 UUID           NOT NULL DEFAULT gen_random_uuid(),
    type               VARCHAR(10)    NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    date               DATE           NOT NULL,
    description        VARCHAR(255),
    category_id        UUID           NOT NULL,
    paid_by_person_id  UUID           NOT NULL,
    payment_method     VARCHAR(20)    NOT NULL,
    card_id            UUID,
    split_rule         VARCHAR(20)    NOT NULL,
    installments_total INT            NOT NULL DEFAULT 1,
    created_at         TIMESTAMP      NOT NULL DEFAULT now(),
    version            BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_transaction PRIMARY KEY (id),
    CONSTRAINT fk_transaction_category       FOREIGN KEY (category_id)       REFERENCES category (id),
    CONSTRAINT fk_transaction_paid_by_person FOREIGN KEY (paid_by_person_id) REFERENCES person   (id),
    CONSTRAINT fk_transaction_card           FOREIGN KEY (card_id)           REFERENCES card      (id),
    CONSTRAINT chk_transaction_type             CHECK (type           IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_transaction_amount           CHECK (amount         >  0),
    CONSTRAINT chk_transaction_payment_method   CHECK (payment_method IN ('CASH', 'DEBIT', 'CREDIT', 'PIX', 'TRANSFER')),
    CONSTRAINT chk_transaction_split_rule       CHECK (split_rule     IN ('PERSON_A', 'PERSON_B', 'FIFTY_FIFTY', 'PROPORTIONAL')),
    CONSTRAINT chk_transaction_installments     CHECK (installments_total >= 1),
    -- card_id só faz sentido quando payment_method = 'CREDIT'
    CONSTRAINT chk_transaction_card_credit      CHECK (
        (payment_method = 'CREDIT' AND card_id IS NOT NULL)
        OR
        (payment_method <> 'CREDIT' AND card_id IS NULL)
    )
);

-- -----------------------------------------------------------------------------
-- installment
-- Parcela gerada automaticamente para compras parceladas.
-- reference_month deve ser sempre o dia 1 do mês de referência (ex.: 2024-03-01).
-- ON DELETE CASCADE: excluir a transaction apaga todas as suas parcelas.
-- Não possui version própria — o controle está na transaction pai.
-- -----------------------------------------------------------------------------
CREATE TABLE installment (
    id              UUID           NOT NULL DEFAULT gen_random_uuid(),
    transaction_id  UUID           NOT NULL,
    number          INT            NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    reference_month DATE           NOT NULL,

    CONSTRAINT pk_installment PRIMARY KEY (id),
    CONSTRAINT fk_installment_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id) ON DELETE CASCADE,
    CONSTRAINT chk_installment_number CHECK (number >= 1),
    CONSTRAINT chk_installment_amount CHECK (amount >  0),
    -- Garante que reference_month seja sempre o primeiro dia do mês
    CONSTRAINT chk_installment_reference_month_day CHECK (EXTRACT(DAY FROM reference_month) = 1),
    -- Garante que não existam duas parcelas com mesmo número para a mesma transação
    CONSTRAINT uq_installment_transaction_number UNIQUE (transaction_id, number)
);

-- -----------------------------------------------------------------------------
-- budget
-- Orçamento mensal por categoria.
-- month deve ser sempre o dia 1 do mês (ex.: 2024-03-01).
-- Unicidade: cada categoria só pode ter um orçamento por mês.
-- version: controle de concorrência otimista.
-- -----------------------------------------------------------------------------
CREATE TABLE budget (
    id           UUID           NOT NULL DEFAULT gen_random_uuid(),
    category_id  UUID           NOT NULL,
    month        DATE           NOT NULL,
    limit_amount NUMERIC(12, 2) NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_budget PRIMARY KEY (id),
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT uq_budget_category_month UNIQUE (category_id, month),
    CONSTRAINT chk_budget_limit_amount CHECK (limit_amount > 0),
    -- Garante que month seja sempre o primeiro dia do mês
    CONSTRAINT chk_budget_month_day CHECK (EXTRACT(DAY FROM month) = 1)
);

-- -----------------------------------------------------------------------------
-- recurring_rule
-- Template para geração automática de lançamentos recorrentes.
-- next_date: próxima data em que o job deve gerar um lançamento a partir desta regra.
-- active: quando false, a regra é ignorada pelo job sem precisar ser excluída.
-- version: controle de concorrência otimista.
-- -----------------------------------------------------------------------------
CREATE TABLE recurring_rule (
    id                UUID           NOT NULL DEFAULT gen_random_uuid(),
    type              VARCHAR(10)    NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,
    description       VARCHAR(255),
    category_id       UUID           NOT NULL,
    paid_by_person_id UUID           NOT NULL,
    payment_method    VARCHAR(20)    NOT NULL,
    split_rule        VARCHAR(20)    NOT NULL,
    frequency         VARCHAR(20)    NOT NULL,
    next_date         DATE           NOT NULL,
    active            BOOLEAN        NOT NULL DEFAULT true,
    created_at        TIMESTAMP      NOT NULL DEFAULT now(),
    version           BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_recurring_rule PRIMARY KEY (id),
    CONSTRAINT fk_recurring_rule_category       FOREIGN KEY (category_id)       REFERENCES category (id),
    CONSTRAINT fk_recurring_rule_paid_by_person FOREIGN KEY (paid_by_person_id) REFERENCES person   (id),
    CONSTRAINT chk_recurring_rule_type           CHECK (type           IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_recurring_rule_amount         CHECK (amount         >  0),
    CONSTRAINT chk_recurring_rule_payment_method CHECK (payment_method IN ('CASH', 'DEBIT', 'CREDIT', 'PIX', 'TRANSFER')),
    CONSTRAINT chk_recurring_rule_split_rule     CHECK (split_rule     IN ('PERSON_A', 'PERSON_B', 'FIFTY_FIFTY', 'PROPORTIONAL')),
    CONSTRAINT chk_recurring_rule_frequency      CHECK (frequency      IN ('MONTHLY', 'WEEKLY', 'YEARLY'))
);

-- =============================================================================
-- ÍNDICES
-- =============================================================================

-- transaction: filtros frequentes por data, categoria e participante pagador
CREATE INDEX idx_transaction_date             ON transaction (date);
CREATE INDEX idx_transaction_category_id      ON transaction (category_id);
CREATE INDEX idx_transaction_paid_by_person_id ON transaction (paid_by_person_id);

-- installment: filtro principal por mês de referência (acerto mensal)
CREATE INDEX idx_installment_reference_month ON installment (reference_month);
-- installment: navegação pelas parcelas de uma transação (complementa a FK)
CREATE INDEX idx_installment_transaction_id  ON installment (transaction_id);

-- budget: filtro por mês
CREATE INDEX idx_budget_month ON budget (month);

-- recurring_rule: job de geração processa apenas regras ativas com next_date vencida
CREATE INDEX idx_recurring_rule_next_date_active ON recurring_rule (next_date) WHERE active = true;
