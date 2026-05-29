# Decisão — Algoritmo de cálculo de reference_month para parcelas
**Data:** 2026-05-28
**Sprint:** 04
**Tomada por:** Arquiteto
**Contexto:** planning Sprint 04 — início da Fatia 3b (geração automática de parcelas)

---

## Problema / contexto

Ao criar uma compra parcelada no cartão de crédito, o sistema precisa calcular em qual
mês cada parcela cai (campo `reference_month` da entidade `Installment`). O mês correto
depende de quando a compra foi feita em relação ao `closing_day` do cartão:
- Se a compra foi antes do fechamento, a primeira parcela cai no mês corrente.
- Se a compra foi no fechamento ou depois, a primeira parcela cai no mês seguinte.

Além disso, o valor total pode não ser divisível exatamente em N parcelas com 2 casas
decimais — é necessário definir qual parcela absorve a diferença.

## Opções consideradas

### Para o cálculo do mês de referência:

| Opção | Descrição | Prós | Contras |
|---|---|---|---|
| A | Sempre no mês seguinte (ignora closing_day) | Simples | Incorreto — parcelas cairiam no mês errado para compras antes do fechamento |
| B | Baseado em closing_day: antes = mesmo mês; no/após = mês seguinte | Correto para a lógica de cartão de crédito | Requer verificação do dia da compra vs. closing_day |
| C | Baseado em due_day (vencimento) | Reflete quando o usuário paga a fatura | Confunde referência de competência com data de pagamento; devido = quando cai na fatura |

### Para o arredondamento:

| Opção | Descrição | Prós | Contras |
|---|---|---|---|
| A | Primeira parcela absorve diferença | Simples | Valor diferente logo de cara; estranha o usuário |
| B | Última parcela absorve diferença | Padrão comum (ex.: financeiras, parcelamento de loja) | Pequena assimetria na última parcela, imperceptível ao usuário |

## Decisão tomada

**Optamos pela Opção B (baseado em closing_day) para reference_month e Opção B (última
parcela absorve diferença) para arredondamento.**

### Algoritmo completo (canônico — backend deve seguir exatamente):

```
Entradas:
  purchaseDate = Transaction.date           (LocalDate)
  closingDay   = Card.closingDay            (int, 1–31)
  N            = Transaction.installmentsTotal (int >= 2)
  totalAmount  = Transaction.amount         (BigDecimal)

Passo 1 — Calcular firstReferenceMonth:
  se purchaseDate.getDayOfMonth() < closingDay:
    firstReferenceMonth = primeiro dia do mesmo mês de purchaseDate
  senão (purchaseDate.getDayOfMonth() >= closingDay):
    firstReferenceMonth = primeiro dia do mês seguinte a purchaseDate

  Em código Java:
    YearMonth ym = (purchaseDate.getDayOfMonth() < closingDay)
        ? YearMonth.from(purchaseDate)
        : YearMonth.from(purchaseDate).plusMonths(1);
    LocalDate firstReferenceMonth = ym.atDay(1);

Passo 2 — Calcular referenceMonth de cada parcela:
  Para i de 1 a N:
    referenceMonth[i] = firstReferenceMonth.plusMonths(i - 1)

  Observação: plusMonths() do Java lida automaticamente com virada de ano
  (ex.: dezembro + 1 mês = janeiro do ano seguinte). Não é necessário tratamento especial.

Passo 3 — Calcular amount de cada parcela:
  installmentAmount = totalAmount.divide(N, 2, HALF_DOWN)
  lastInstallmentAmount = totalAmount.subtract(installmentAmount.multiply(N - 1))

  (A última parcela absorve a diferença de centavos, garantindo que a soma seja
   exatamente totalAmount independentemente do arredondamento.)

  Em código Java:
    BigDecimal base = totalAmount.divide(
        BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
    BigDecimal last = totalAmount.subtract(base.multiply(BigDecimal.valueOf(n - 1)));
```

### Exemplos de referência (usados como casos de teste):

| Compra | closingDay | N | Parcelas (referenceMonth) |
|--------|-----------|---|--------------------------|
| 2026-05-15 | 20 | 3 | 2026-05-01, 2026-06-01, 2026-07-01 |
| 2026-05-25 | 20 | 3 | 2026-06-01, 2026-07-01, 2026-08-01 |
| 2026-05-20 | 20 | 2 | 2026-06-01, 2026-07-01 (dia igual ao closing = mês seguinte) |
| 2026-11-25 | 20 | 3 | 2026-12-01, 2027-01-01, 2027-02-01 (virada de ano) |
| 2026-01-31 | 1  | 2 | 2026-02-01, 2026-03-01 (closing_day=1, compra no dia 31 → mês seguinte) |

### Exemplo de arredondamento:

| totalAmount | N | installmentAmount | lastInstallmentAmount | Soma |
|-------------|---|-------------------|-----------------------|------|
| R$ 10,00    | 3 | R$ 3,33           | R$ 3,34               | R$ 10,00 |
| R$ 100,00   | 3 | R$ 33,33          | R$ 33,34              | R$ 100,00 |
| R$ 1.500,01 | 3 | R$ 500,00         | R$ 500,01             | R$ 1.500,01 |

## Impacto

- [x] Documentar algoritmo em `memory/decisions/` — este arquivo
- [x] Atualizar `docs/api.md` com seção Fatia 3b e endpoint de installments
- [ ] Backend: `InstallmentService.generateInstallments()` deve implementar exatamente este algoritmo
- [ ] QA: cobrir todos os exemplos da tabela como casos de teste (unit tests)
- [ ] QA: caso especial — `closing_day` >= 29 e mês de fevereiro (ex.: closing_day=30, compra em 2028-01-15 → mês seguinte é fevereiro; não há dia 30 em fev, mas reference_month é sempre dia 1, então não há problema)

## Observação sobre closing_day >= 29

O campo `closingDay` aceita valores de 1 a 31 (validado pelo Bean Validation). Meses
com menos dias (ex.: fevereiro com 28 dias) podem ter `closingDay=30` ou `closingDay=31`
que nunca ocorre naquele mês. O algoritmo compara `purchaseDate.getDayOfMonth()` com
`closingDay` diretamente — se o mês não tem o dia 30, uma compra de fevereiro sempre
terá `getDayOfMonth() <= 28 < 30`, portanto sempre cairá no próprio mês. Comportamento
correto e sem tratamento especial necessário.

## Revisão futura

Esta decisão deve ser reavaliada se: o produto decidir suportar lógica de "vencimento"
(due_day) em vez de fechamento para definir o reference_month — ex.: para fins de
relatório de "quando sai do bolso". Por ora, reference_month = competência da fatura.
