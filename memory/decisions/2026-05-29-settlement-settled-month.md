# Decisão — Comportamento de "mês acertado" no acerto de contas
**Data:** 2026-05-29
**Sprint:** 06
**Tomada por:** Arquiteto
**Contexto:** planning Sprint 06

---

## Problema / contexto

O endpoint `GET /api/settlement?month=yyyy-MM` calcula quem deve quanto a quem em um
dado mês. A questão é: o sistema precisa de um estado persistente de "mês acertado"
(flag, tabela de acertos, registro de transferência, etc.) ou o cálculo é sempre
feito ao vivo?

Exemplos de problemas que surgiriam com um estado de "acertado":
- O usuário marca o mês como acertado, depois lança um gasto esquecido: o acerto fica
  inconsistente e precisaria de lógica de "reabertura".
- Dois usuários simultâneos: um acerta, o outro lança — conflito de estado.
- Edge case de parcelas: uma parcela de mês anterior pode ser cadastrada retroativamente,
  modificando o acerto que já havia sido "fechado".

---

## Opções consideradas

| Opção | Prós | Contras |
|---|---|---|
| A — Sem estado: recálculo idempotente sempre | Simples; nunca fica inconsistente; sem edge cases de reabertura; sem migration extra | Usuário não sabe se já fez a transferência (mas isso é responsabilidade do usuário fora do sistema) |
| B — Campo `settled_at` / tabela `settlement` | Histórico de quando o acerto foi feito; permite registrar valor transferido | Complexidade de schema; edge cases de reabertura; requer UI de confirmação extra |
| C — Flag booleana na response calculada | Indica se o saldo é zero (settled=true); sem persistência extra | Pode confundir: "settled" = saldo zerado, não = confirmação manual |

---

## Decisão tomada

**Optamos pela Opção A — sem estado persistente de acerto.**

O endpoint `GET /api/settlement?month=yyyy-MM` é idempotente e recalcula sempre com
base nos lançamentos e parcelas do mês. O campo `settled` na response é derivado:
`settled = true` apenas quando `totalExpense = 0` ou `amountOwed = 0.00` (saldo zerado
naturalmente, pois cada um pagou exatamente o que devia).

O usuário consulta o resultado, realiza a transferência bancária fora do sistema, e
ao lançar receitas/despesas do próximo mês o saldo do mês anterior deixa de ser relevante.
Não há botão de "marcar como acertado" nem tela de confirmação de transferência.

**Motivo:** simplicidade, ausência de edge cases de reabertura e ausência de
necessidade de migration extra. Para v1 do produto, o histórico de quando a
transferência foi feita fora do sistema não é um requisito.

---

## Impacto

- [x] Documentado em `docs/api.md` (seção Fatia 6 — campo `settled` da response)
- [ ] Backend: `SettlementService` não persiste nenhum estado; apenas calcula e retorna
- [ ] Frontend: não exibir botão "Marcar como acertado"; apenas exibir o resultado calculado
- [ ] QA: testar que chamar o endpoint duas vezes com os mesmos dados retorna o mesmo resultado

---

## Revisão futura

Esta decisão deve ser reavaliada se:
- O produto decidir registrar o histórico de transferências realizadas entre as pessoas
  (ex.: "em 05/06 foi transferido R$ 750,00 de Pessoa B para Pessoa A").
- O produto precisar de notificações ("você ainda não acertou o mês de maio").
- Nesse caso, seria necessário criar uma tabela `settlement_record` com data, valor e
  quem realizou, sem alterar a lógica de cálculo do endpoint atual.
