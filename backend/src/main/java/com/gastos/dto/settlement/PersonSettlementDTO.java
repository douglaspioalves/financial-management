package com.gastos.dto.settlement;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resumo financeiro de uma pessoa no acerto de contas.
 *
 * @param id         identificador da pessoa
 * @param name       nome da pessoa
 * @param totalPaid  quanto a pessoa pagou (soma dos lançamentos cujo paidByPerson == esta pessoa)
 * @param shouldPay  quanto a pessoa deveria pagar conforme as regras de divisão
 * @param balance    totalPaid - shouldPay (positivo = credor; negativo = devedor)
 */
public record PersonSettlementDTO(
        UUID id,
        String name,
        BigDecimal totalPaid,
        BigDecimal shouldPay,
        BigDecimal balance
) {}
