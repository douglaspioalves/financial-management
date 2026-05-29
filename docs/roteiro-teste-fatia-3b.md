# Roteiro de Teste Manual — Fatia 3b: Geração de Parcelas

**Sprint:** 04
**Data:** 2026-05-28
**Responsável:** QA

---

## Pré-requisitos

- Aplicação rodando: `docker compose up --build`
- Dois usuários criados (ou use os usuários seed do sistema)
- Pelo menos um cartão de crédito cadastrado em cada pessoa
- Pelo menos uma categoria do tipo EXPENSE cadastrada
- Ferramenta HTTP disponível: navegador + interface Angular, ou Postman/Insomnia
- Token JWT obtido via POST /api/auth/login

---

## 1. Criar um cartão de crédito (se não existir)

**Via interface Angular:**
1. Acesse o menu "Cartões"
2. Clique em "Adicionar cartão"
3. Preencha:
   - Nome: `Nubank Alice`
   - Proprietário: `Alice`
   - Dia de fechamento: `20`
   - Dia de vencimento: `27`
4. Salve e confirme que o cartão aparece na lista

**Via API (Postman):**
```http
POST /api/cards
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Nubank Alice",
  "ownerPersonId": "{id-de-alice}",
  "closingDay": 20,
  "dueDay": 27
}
```
Resultado esperado: HTTP 201 com o cartão criado e campos `id`, `closingDay=20`, `dueDay=27`.

---

## 2. Criar uma compra parcelada

### Caso A — Compra antes do fechamento (primeiro mes = mes da compra)

**Dados:**
- Data da compra: `2026-05-15` (dia 15, fechamento do cartão: dia 20)
- Valor: `R$ 1.500,00`
- Parcelas: `3x`
- Cartão: `Nubank Alice` (fechamento=20)

**Via interface Angular:**
1. Acesse "Lançamentos" > "Novo lançamento"
2. Tipo: Despesa
3. Data: 15/05/2026
4. Valor: 1500,00
5. Forma de pagamento: Cartão de Crédito
6. Cartão: selecione `Nubank Alice`
7. Parcelas: 3
8. Categoria: qualquer categoria de despesa
9. Quem pagou: Alice
10. Divisão: 50/50
11. Salve

**Verificação esperada na interface:**
- O lançamento aparece na lista com badge `1/3`
- Ao expandir (ou acessar detalhe), as parcelas mostram:

| Parcela | Valor | Mês de referência |
|---------|-------|-------------------|
| 1/3     | R$ 500,00 | maio/2026 |
| 2/3     | R$ 500,00 | junho/2026 |
| 3/3     | R$ 500,00 | julho/2026 |

**Via API — verificar parcelas geradas:**
```http
GET /api/transactions/{id}/installments
Authorization: Bearer {token}
```
Resultado esperado: array com 3 objetos, `referenceMonth` em `[2026-05-01, 2026-06-01, 2026-07-01]`.

---

### Caso B — Compra no dia do fechamento ou depois (primeiro mes = mes seguinte)

**Dados:**
- Data da compra: `2026-05-20` (exatamente no fechamento)
- Valor: `R$ 200,00`
- Parcelas: `2x`
- Cartão: `Nubank Alice` (fechamento=20)

**Verificação esperada:**

| Parcela | Valor | Mês de referência |
|---------|-------|-------------------|
| 1/2     | R$ 100,00 | junho/2026 |
| 2/2     | R$ 100,00 | julho/2026 |

Confirme que a 1a parcela cai em junho (e NAO em maio), porque a compra foi feita no dia do fechamento.

---

### Caso C — Virada de ano

**Dados:**
- Data da compra: `2026-11-25` (após fechamento)
- Valor: `R$ 300,00`
- Parcelas: `3x`
- Cartão com fechamento: dia 20

**Verificação esperada:**

| Parcela | Valor | Mês de referência |
|---------|-------|-------------------|
| 1/3     | R$ 100,00 | dezembro/2026 |
| 2/3     | R$ 100,00 | janeiro/2027 |
| 3/3     | R$ 100,00 | fevereiro/2027 |

Confirme que a virada de ano foi tratada corretamente (2a parcela em 2027).

---

### Caso D — Arredondamento (valor que não divide exato)

**Dados:**
- Data da compra: `2026-05-01` (antes do fechamento)
- Valor: `R$ 100,00`
- Parcelas: `3x`
- Qualquer cartão com fechamento > 1

**Verificação esperada:**

| Parcela | Valor | Mês de referência |
|---------|-------|-------------------|
| 1/3     | R$ 33,33 | conforme cartão |
| 2/3     | R$ 33,33 | conforme cartão |
| 3/3     | R$ 33,34 | conforme cartão |

Confirme:
1. As duas primeiras parcelas usam FLOOR (truncam para baixo): R$ 33,33
2. A ultima parcela absorve a diferença: R$ 33,34
3. Soma total: R$ 100,00 (obrigatoriamente igual ao valor original)

---

### Caso E — Parcela única (1x)

**Dados:**
- Qualquer data, forma de pagamento: cartão de crédito, parcelas: 1

**Verificação esperada:**
- Exatamente 1 parcela gerada
- Valor da parcela = valor total da compra
- Sem badge de parcelamento na lista (ou badge `1/1` oculto)

---

### Caso F — Cartão com fechamento no dia 31

**Dados:**
- Cartão com fechamento=31 (meses curtos como fevereiro ou abril)
- Data da compra: `2026-02-15` (antes do fechamento)
- Valor: `R$ 200,00`
- Parcelas: `2x`

**Verificação esperada:**
- O sistema não lança exceção
- 1a parcela: fevereiro/2026 (dia 15 < fechamento 31)
- 2a parcela: março/2026

---

## 3. Verificar integridade dos dados

### 3.1 Soma das parcelas = valor total

Para qualquer compra parcelada criada, verifique via API:

```http
GET /api/transactions/{id}/installments
Authorization: Bearer {token}
```

Some os valores de `amount` de todas as parcelas e confirme que o resultado é igual ao campo `amount` do lançamento original. Esta regra nunca pode ser violada.

---

### 3.2 Autenticacao obrigatoria

Tente acessar os seguintes endpoints SEM o header `Authorization`:

```http
GET /api/transactions/{id}/installments
POST /api/transactions
```

Resultado esperado: HTTP 401 (Unauthorized) em ambos. Nenhum endpoint de parcelas pode ser acessado sem autenticacao.

---

### 3.3 Lançamento parcelado aparece no acerto do mes correto

1. Crie uma compra parcelada com data em maio/2026 (antes do fechamento):
   - Parcelas: 2x
   - 1a parcela deve cair em maio/2026
   - 2a parcela deve cair em junho/2026

2. Acesse o resumo/acerto do mes de maio/2026
   - Deve mostrar apenas a 1a parcela (R$ X) — NAO o valor total da compra

3. Acesse o resumo/acerto do mes de junho/2026
   - Deve mostrar apenas a 2a parcela (R$ X)

Este comportamento e critico: o acerto usa `Installment.reference_month`, nao a data original da compra.

---

## 4. Casos de erro esperados

### 4.1 Parcelas com metodo de pagamento diferente de credito

Tente criar um lançamento parcelado (installmentsTotal > 1) com pagamento em dinheiro/PIX/debito:

```http
POST /api/transactions
{
  "paymentMethod": "CASH",
  "installmentsTotal": 3,
  ...
}
```

Resultado esperado: HTTP 400 ou 422 com mensagem de erro explicativa.

### 4.2 Parcelas sem cartao selecionado

Tente criar um lançamento parcelado com `paymentMethod=CREDIT` mas sem `cardId`:

Resultado esperado: HTTP 400 com mensagem "Pagamento com cartão de crédito exige a seleção de um cartão."

---

## 5. Checklist de aprovacao

Marque cada item apos validar:

- [ ] Caso A: compra antes do fechamento — 1a parcela no mesmo mes
- [ ] Caso B: compra no dia do fechamento — 1a parcela no mes seguinte
- [ ] Caso C: virada de ano — meses calculados corretamente (2027)
- [ ] Caso D: arredondamento — ultima parcela absorve diferenca; soma = total
- [ ] Caso E: parcela unica — 1 installment com valor total
- [ ] Caso F: closingDay=31 — nao lanca excecao em meses curtos
- [ ] Autenticacao obrigatoria em todos os endpoints de parcelas
- [ ] Acerto usa referenceMonth da parcela (nao data da compra)
- [ ] Erro 400/422 para parcelado sem cartao de credito
- [ ] Badge de parcelamento visivel na lista de lancamentos

---

## Resultado do teste

**Data de execucao:**
**Executado por:**
**Versao/commit:**

**Status geral:** [ ] APROVADO  [ ] REPROVADO

**Observacoes:**
