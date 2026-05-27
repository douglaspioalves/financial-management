# Learnings — Negócio / PM
> Atualizado pelo Scrum Master e arquiteto após cada retrospectiva.

## Contexto do produto

- App para controle de gastos entre duas pessoas (casal).
- Dois logins independentes, base de dados compartilhada (sem multi-tenant).
- Dois participantes (`Person`): rótulos usados em lançamentos, sem login próprio.
- Uma das usuárias não é técnica — UX simples é requisito, não diferencial.

## Regras de produto já decididas

- **Parcelamento:** parcela cai no mês da fatura, não da compra.
- **Acerto:** calculado sobre `Installment` do mês, não sobre `Transaction`.
- **Proporcional:** usa receitas individuais (PERSON_A/B) do mês em que a despesa cai.
  Receita FIFTY_FIFTY não entra no cálculo. Mês sem receita → pendência (não 50/50).
- **Design system aprovado:** azul pastel, Fraunces + Plus Jakarta Sans, claro e escuro.
- **Idioma:** código em inglês; interface, docs e commits em pt-br.

## Decisões de produto pendentes (resolver na fatia relevante)

- [ ] **Fatia 2:** Excluir categoria com lançamentos existentes — impedir ou arquivar?
- [ ] **Fatia 6:** Mês já acertado pode ser editado retroativamente?
- [ ] **Pós-v1:** Hospedagem para acesso externo (fora de casa pelo celular).

## Backlog de ideias futuras (fora da v1)

- Login social (Google)
- Multi-moeda
- App mobile nativo
- Anexos de comprovantes
- Metas de poupança e investimentos

## Aprendizados das retrospectivas

> (preenchido ao longo dos sprints)
