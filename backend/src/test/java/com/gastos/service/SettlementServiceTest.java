package com.gastos.service;

import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.domain.Installment;
import com.gastos.domain.PaymentMethod;
import com.gastos.domain.Person;
import com.gastos.domain.SplitRule;
import com.gastos.domain.Transaction;
import com.gastos.domain.TransactionType;
import com.gastos.dto.settlement.SettlementResponse;
import com.gastos.repository.InstallmentRepository;
import com.gastos.repository.PersonRepository;
import com.gastos.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do SettlementService — lógica de acerto de contas.
 *
 * Nomenclatura dos cenários: TC-S01 … TC-S10
 *
 * PersonA = "Alice" (ordem alfabética → posição personA na response)
 * PersonB = "Bob"
 *
 * Mês de referência padrão: maio/2026
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private PersonRepository personRepository;

    @InjectMocks
    private SettlementService settlementService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final YearMonth MAY_2026    = YearMonth.of(2026, 5);
    private static final LocalDate MAY_START   = LocalDate.of(2026, 5,  1);
    private static final LocalDate MAY_END     = LocalDate.of(2026, 5, 31);

    private static final YearMonth FEB_2026    = YearMonth.of(2026, 2);
    private static final LocalDate FEB_START   = LocalDate.of(2026, 2,  1);
    private static final LocalDate FEB_END     = LocalDate.of(2026, 2, 28);

    private UUID personAId;
    private UUID personBId;
    private Person personA;
    private Person personB;
    private Category expenseCategory;
    private Category incomeCategory;

    @BeforeEach
    void setUp() {
        personAId = UUID.randomUUID();
        personBId = UUID.randomUUID();

        // "Alice" < "Bob" → serviço ordena alfabeticamente → Alice=personA, Bob=personB
        personA = Person.builder()
                .id(personAId)
                .name("Alice")
                .color("#4a7fc4")
                .version(0L)
                .build();

        personB = Person.builder()
                .id(personBId)
                .name("Bob")
                .color("#e88a74")
                .version(0L)
                .build();

        expenseCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Mercado")
                .type(CategoryType.EXPENSE)
                .color("#e88a74")
                .version(0L)
                .build();

        incomeCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Salário")
                .type(CategoryType.INCOME)
                .color("#7ec8a0")
                .version(0L)
                .build();

        // Stub padrão: serviço chama findAll() e ordena por nome
        when(personRepository.findAll()).thenReturn(List.of(personA, personB));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Transaction cashExpense(BigDecimal amount, SplitRule splitRule, Person paidBy) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .date(LocalDate.of(2026, 5, 10))
                .category(expenseCategory)
                .paidByPerson(paidBy)
                .paymentMethod(PaymentMethod.PIX)
                .splitRule(splitRule)
                .installmentsTotal(1)
                .version(0L)
                .build();
    }

    private Transaction income(BigDecimal amount, SplitRule splitRule, Person earnedBy) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.INCOME)
                .amount(amount)
                .date(LocalDate.of(2026, 5, 5))
                .category(incomeCategory)
                .paidByPerson(earnedBy)
                .paymentMethod(PaymentMethod.TRANSFER)
                .splitRule(splitRule)
                .installmentsTotal(1)
                .version(0L)
                .build();
    }

    private Installment installment(BigDecimal amount, SplitRule splitRule,
                                    Person paidBy, LocalDate referenceMonth) {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(amount.multiply(BigDecimal.valueOf(3)))
                .date(LocalDate.of(2026, 1, 10))
                .category(expenseCategory)
                .paidByPerson(paidBy)
                .paymentMethod(PaymentMethod.CREDIT)
                .splitRule(splitRule)
                .installmentsTotal(3)
                .version(0L)
                .build();

        return Installment.builder()
                .id(UUID.randomUUID())
                .transaction(tx)
                .number(2)
                .amount(amount)
                .referenceMonth(referenceMonth)
                .build();
    }

    private void stubNoExpensesMay() {
        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of());
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END)).thenReturn(List.of());
    }

    // =========================================================================
    // TC-S01 — FIFTY_FIFTY simples
    // =========================================================================

    @Test
    @DisplayName("TC-S01: FIFTY_FIFTY — Alice paga R$1000, Bob deve R$500")
    void fiftyFifty_personAPaysFull_personBOwes500() {
        Transaction expense = cashExpense(new BigDecimal("1000.00"), SplitRule.FIFTY_FIFTY, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of(expense));
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END)).thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.settled()).isFalse();
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
        assertThat(result.amountOwed()).isEqualByComparingTo("500.00");
        assertThat(result.totalExpense()).isEqualByComparingTo("1000.00");

        assertThat(result.personA().totalPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personA().balance()).isEqualByComparingTo("500.00");

        assertThat(result.personB().totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personB().balance()).isEqualByComparingTo("-500.00");
    }

    // =========================================================================
    // TC-S02 — PERSON_A (despesa 100% atribuída a PersonA)
    // =========================================================================

    @Test
    @DisplayName("TC-S02: PERSON_A — Alice paga R$500, despesa 100% dela → settled=true")
    void personA_paysFull_isSettled() {
        Transaction expense = cashExpense(new BigDecimal("500.00"), SplitRule.PERSON_A, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of(expense));
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END)).thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.settled()).isTrue();
        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.amountOwed()).isNull();
        assertThat(result.debtor()).isNull();
        assertThat(result.creditor()).isNull();

        assertThat(result.personA().totalPaid()).isEqualByComparingTo("500.00");
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personA().balance()).isEqualByComparingTo("0.00");

        assertThat(result.personB().totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("0.00");
    }

    // =========================================================================
    // TC-S03 — PERSON_B (despesa 100% atribuída a PersonB)
    // =========================================================================

    @Test
    @DisplayName("TC-S03: PERSON_B — Bob paga R$800, despesa 100% dele → settled=true")
    void personB_paysFull_isSettled() {
        Transaction expense = cashExpense(new BigDecimal("800.00"), SplitRule.PERSON_B, personB);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of(expense));
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END)).thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.settled()).isTrue();
        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.amountOwed()).isNull();

        assertThat(result.personB().totalPaid()).isEqualByComparingTo("800.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("800.00");
        assertThat(result.personB().balance()).isEqualByComparingTo("0.00");
    }

    // =========================================================================
    // TC-S04 — PROPORTIONAL com receitas lançadas
    // =========================================================================

    @Test
    @DisplayName("TC-S04: PROPORTIONAL — IncomeA=R$6000 (60%), IncomeB=R$4000 (40%), despesa R$1000 → Bob deve R$400")
    void proportional_withIncomes_personBOwes400() {
        Transaction incomeA = income(new BigDecimal("6000.00"), SplitRule.PERSON_A, personA);
        Transaction incomeB = income(new BigDecimal("4000.00"), SplitRule.PERSON_B, personB);
        Transaction expense  = cashExpense(new BigDecimal("1000.00"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of(expense));
        // findIndividualIncomesByMonth já filtra por PERSON_A/PERSON_B no banco
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END))
                .thenReturn(List.of(incomeA, incomeB));

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.settled()).isFalse();

        assertThat(result.personA().shouldPay()).isEqualByComparingTo("600.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("400.00");

        assertThat(result.personA().totalPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.personA().balance()).isEqualByComparingTo("400.00");

        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
        assertThat(result.amountOwed()).isEqualByComparingTo("400.00");
    }

    // =========================================================================
    // TC-S05 — PROPORTIONAL sem receitas individuais → pendente
    // =========================================================================

    @Test
    @DisplayName("TC-S05: PROPORTIONAL sem receitas individuais → pendingProportional=true, amountOwed=null")
    void proportional_noIndividualIncomes_returnsPending() {
        Transaction expense = cashExpense(new BigDecimal("500.00"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of(expense));
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END)).thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.pendingProportional()).isTrue();
        assertThat(result.amountOwed()).isNull();
        assertThat(result.debtor()).isNull();
        assertThat(result.creditor()).isNull();
        assertThat(result.settled()).isFalse();
        assertThat(result.pendingMessage()).isNotBlank();
        assertThat(result.pendingMessage()).containsIgnoringCase("receitas");
    }

    // =========================================================================
    // TC-S06 — MIX: FIFTY_FIFTY + PROPORTIONAL sem receita → acerto inteiro pendente
    // =========================================================================

    @Test
    @DisplayName("TC-S06: FIFTY_FIFTY + PROPORTIONAL sem receita → PROPORTIONAL bloqueia o acerto inteiro")
    void mix_fiftyFiftyAndProportionalWithoutIncome_entireSettlementIsPending() {
        Transaction fiftyFiftyExpense  = cashExpense(new BigDecimal("200.00"), SplitRule.FIFTY_FIFTY, personA);
        Transaction proportionalExpense = cashExpense(new BigDecimal("300.00"), SplitRule.PROPORTIONAL, personB);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END))
                .thenReturn(List.of(fiftyFiftyExpense, proportionalExpense));
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END)).thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.pendingProportional()).isTrue();
        assertThat(result.amountOwed()).isNull();
        assertThat(result.debtor()).isNull();
        assertThat(result.settled()).isFalse();
        assertThat(result.totalExpense()).isEqualByComparingTo("500.00");
    }

    // =========================================================================
    // TC-S07 — Mês sem despesas
    // =========================================================================

    @Test
    @DisplayName("TC-S07: mês sem despesas → settled=true, totalExpense=0, amountOwed=null")
    void noExpenses_returnsSettledWithZero() {
        stubNoExpensesMay();

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.settled()).isTrue();
        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(result.amountOwed()).isNull();
        assertThat(result.debtor()).isNull();
        assertThat(result.creditor()).isNull();
        assertThat(result.personA().totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("0.00");
        assertThat(result.personB().totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("0.00");
    }

    // =========================================================================
    // TC-S08 — Parcela de cartão entra no acerto, não o valor total da compra
    // =========================================================================

    @Test
    @DisplayName("TC-S08: parcela de R$100 (fev/2026) entra no acerto de fev, não o total da compra")
    void installment_onlyCurrentMonthInstallmentEntersSettlement() {
        Installment febInstallment = installment(
                new BigDecimal("100.00"), SplitRule.FIFTY_FIFTY, personA, FEB_START);

        when(installmentRepository.findExpenseInstallmentsByMonth(FEB_START)).thenReturn(List.of(febInstallment));
        when(transactionRepository.findCashExpensesByMonth(FEB_START, FEB_END)).thenReturn(List.of());
        when(transactionRepository.findIndividualIncomesByMonth(FEB_START, FEB_END)).thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(FEB_2026);

        assertThat(result.totalExpense()).isEqualByComparingTo("100.00");
        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.settled()).isFalse();
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
        assertThat(result.amountOwed()).isEqualByComparingTo("50.00");
    }

    // =========================================================================
    // TC-S09 — Receita FIFTY_FIFTY NÃO entra na proporção
    // =========================================================================

    @Test
    @DisplayName("TC-S09: receita FIFTY_FIFTY excluída da proporção — apenas receitas individuais contam")
    void fiftyFiftyIncome_doesNotAffectProportionalRatio() {
        Transaction incomeA = income(new BigDecimal("3000.00"), SplitRule.PERSON_A, personA);
        Transaction incomeB = income(new BigDecimal("3000.00"), SplitRule.PERSON_B, personB);
        // Receita compartilhada — NÃO deve entrar na proporção (o repositório já filtra)

        Transaction expense = cashExpense(new BigDecimal("1000.00"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of(expense));
        // findIndividualIncomesByMonth retorna apenas PERSON_A e PERSON_B → 50%/50%
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END))
                .thenReturn(List.of(incomeA, incomeB));

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
        assertThat(result.amountOwed()).isEqualByComparingTo("500.00");
    }

    // =========================================================================
    // TC-S10 — Arredondamento PROPORTIONAL (HALF_UP consistente)
    // =========================================================================

    @Test
    @DisplayName("TC-S10: arredondamento HALF_UP — R$100.01 com ratio 60%/40% → shareA=R$60.01, shareB=R$40.00")
    void proportional_rounding_halfUp_consistent() {
        Transaction incomeA = income(new BigDecimal("6000.00"), SplitRule.PERSON_A, personA);
        Transaction incomeB = income(new BigDecimal("4000.00"), SplitRule.PERSON_B, personB);
        Transaction expense  = cashExpense(new BigDecimal("100.01"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_START)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_START, MAY_END)).thenReturn(List.of(expense));
        when(transactionRepository.findIndividualIncomesByMonth(MAY_START, MAY_END))
                .thenReturn(List.of(incomeA, incomeB));

        SettlementResponse result = settlementService.calculate(MAY_2026);

        assertThat(result.pendingProportional()).isFalse();
        // shareA = 100.01 * 0.60 = 60.006 → HALF_UP → 60.01
        // shareB = 100.01 - 60.01 = 40.00 (sem perda de centavo)
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("60.01");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("40.00");

        BigDecimal totalShares = result.personA().shouldPay().add(result.personB().shouldPay());
        assertThat(totalShares).isEqualByComparingTo("100.01");
        assertThat(result.amountOwed()).isEqualByComparingTo("40.00");
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
    }
}
