# Decisões de Arquitetura

> Registro curto das decisões tomadas. Formato: data · decisão · motivo.
> Mantido pelo agente **arquiteto**.

## 2026-05 · Dois logins independentes, base compartilhada
Sem entidade de casal/household nem convite. Os dois usuários acessam os mesmos dados.
Motivo: uso pessoal entre duas pessoas; simplicidade. Não há multi-tenant.

## 2026-05 · Participantes como entidade própria (Person)
As duas pessoas dos lançamentos são `Person` (rótulos), independentes do `User` (login).
Motivo: separar identidade de acesso (login) da identidade de divisão de despesas.

## 2026-05 · Idioma: código em inglês, interface/docs em pt-br
Motivo: alinhamento com convenções de Spring/Angular e geração de código mais limpa,
mantendo a experiência do usuário e a documentação em português.

## 2026-05 · Parcela como entidade própria (Installment)
Compra parcelada gera N `Installment`; o acerto mensal usa as parcelas, não o valor cheio.
Motivo: refletir como o dinheiro realmente sai do bolso, mês a mês.

## 2026-05 · Optimistic locking nas entidades editáveis
Coluna `version` (`@Version`) para tratar edição concorrente dos dois logins.
Motivo: evitar sobrescrita silenciosa quando as duas pessoas editam ao mesmo tempo.

## 2026-05 · Design system aprovado
Azul pastel + apoios (menta/coral/areia/lilás), Fraunces + Plus Jakarta Sans, claro e escuro.
Referência: `docs/design-system.html`.

## 2026-05 · Divisão proporcional = receita real do mês
A regra `PROPORTIONAL` usa a soma das receitas individuais (type=INCOME, split PERSON_A/B)
lançadas no mês em que a despesa/parcela cai. Decisões associadas:
- Só receita individual conta (receita FIFTY_FIFTY não entra na proporção).
- Proporção é a do mês em que a despesa cai (coerência com parcelas).
- Mês sem receita individual → proporção indefinida: acerto sinaliza pendência, não assume 50/50.
- `Person` NÃO ganha campo de renda fixa — a informação vem das transações de receita.
Motivo: divisão mais justa para renda variável, ainda que com mais regras de borda.
