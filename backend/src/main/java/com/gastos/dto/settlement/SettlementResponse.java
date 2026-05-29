package com.gastos.dto.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta completa do acerto de contas de um mês.
 *
 * @param month                primeiro dia do mês de referência
 * @param totalExpense         total de despesas computadas no mês
 * @param personA              dados financeiros da Pessoa A
 * @param personB              dados financeiros da Pessoa B
 * @param debtor               "PERSON_A" | "PERSON_B" | null (se quitado ou pendente)
 * @param creditor             "PERSON_A" | "PERSON_B" | null (se quitado ou pendente)
 * @param amountOwed           valor que o devedor deve ao credor; null se pendingProportional ou settled
 * @param settled              true se o saldo é menor que R$ 0,01 (considerar quitado)
 * @param pendingProportional  true se há despesas PROPORTIONAL sem receitas individuais cadastradas
 * @param pendingMessage       mensagem em pt-br orientando o usuário; null se não houver pendência
 */
public record SettlementResponse(
        LocalDate month,
        BigDecimal totalExpense,
        PersonSettlementDTO personA,
        PersonSettlementDTO personB,
        String debtor,
        String creditor,
        BigDecimal amountOwed,
        boolean settled,
        boolean pendingProportional,
        String pendingMessage
) {}
