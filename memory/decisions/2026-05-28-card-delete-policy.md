# Decisão — Política de Exclusão de Cartão
**Data:** 2026-05-28
**Sprint:** 03
**Tomada por:** Arquiteto
**Contexto:** planning Sprint 03

---

## Problema / contexto

O endpoint `DELETE /api/cards/{id}` precisa de uma política clara para o caso em que
o cartão a ser excluído possui transações vinculadas (`card_id` referenciando o cartão
em uma ou mais linhas da tabela `transaction`).

Caso o cartão seja excluído enquanto existem transações apontando para ele, ocorreria
violação de integridade referencial (FK `fk_transaction_card`), quebrando o histórico
financeiro. A pergunta é: devemos permitir a exclusão com cascade, bloquear ou fazer
soft delete?

---

## Opções consideradas

| Opção | Prós | Contras |
|---|---|---|
| A — Exclusão com cascade (apagar cartão e suas transações) | Remove consistentemente | Destrói histórico financeiro; irreversível; perigoso em produção |
| B — Bloquear exclusão com 409 | Preserva integridade; comportamento previsível; usuário é avisado | Usuário precisa remover manualmente as transações antes |
| C — Soft delete (marcar cartão como inativo) | Preserva histórico; cartão some dos dropdowns | Complexidade extra de schema (coluna `inactive`); cartão morto aparece em transações existentes |

---

## Decisão tomada

**Optamos pela Opção B — Bloquear exclusão com 409.**

Motivo: a tabela `card` já tem constraint de FK em `transaction(card_id)`. Um cartão
com transações vinculadas representa um histórico financeiro real que não deve ser
apagado silenciosamente. O usuário deve ser informado claramente que precisa remover
(ou reclassificar) os lançamentos associados antes de poder excluir o cartão.

Mensagem de erro ao usuário (pt-br):
> "Este cartão possui lançamentos vinculados e não pode ser excluído. Remova os
> lançamentos antes de excluir o cartão."

A verificação deve ser feita no `CardService`, antes de tentar o delete, para retornar
o status 409 com mensagem amigável em vez de deixar o banco lançar um erro de FK.

---

## Implementação esperada

No `CardService.delete(UUID id)`:
1. Verificar se o cartão existe → 404 se não encontrado.
2. Verificar se existem transações com `card_id = id` →
   `transactionRepository.existsByCardId(id)` (query derivada JPA).
3. Se existirem transações → lançar exceção de negócio mapeada para 409.
4. Se não existirem → deletar o cartão fisicamente.

---

## Impacto

- [x] Documentado em `docs/api.md` (seção DELETE /api/cards/{id})
- [ ] Backend deve implementar verificação em `CardService`
- [ ] QA deve cobrir com TC-09 (sem transações → 204) e TC-10 (com transações → 409)
- [ ] Frontend deve exibir a mensagem de erro 409 ao usuário no dialog de confirmação

---

## Revisão futura

Esta decisão deve ser reavaliada se:
- O produto decidir oferecer uma funcionalidade de "arquivar cartão" (encerramento de
  cartão com histórico preservado). Nesse caso, a Opção C (soft delete) seria mais
  adequada.
- O volume de dados crescer e os usuários precisarem migrar lançamentos entre cartões
  em lote. Nesse caso, um endpoint específico de migração seria necessário antes de
  permitir a exclusão.
