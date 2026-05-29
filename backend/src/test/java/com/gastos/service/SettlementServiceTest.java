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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do SettlementService — lógica de acerto de contas.
 *
 * Nomenclatura dos cenários:
 *   TC-S01 … TC-S10
 *
 * PersonA = "Alice" (personAId)
 * PersonB = "Bob"   (personBId)
 *
 * Mês de referência padrão: maio/2026 (2026-05-01)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private SettlementService settlementService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final LocalDate MAY_2026      = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_2026_END  = LocalDate.of(2026, 5, 31);

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

        // Stub padrão das duas pessoas
        when(personRepository.findById(personAId)).thenReturn(Optional.of(personA));
        when(personRepository.findById(personBId)).thenReturn(Optional.of(personB));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Cria uma Transaction de despesa à vista (não-parcelada). */
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

    /** Cria uma Transaction de receita individual. */
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

    /** Cria uma Installment vinculada a uma Transaction de despesa parcelada. */
    private Installment installment(BigDecimal amount, SplitRule splitRule,
                                    Person paidBy, LocalDate referenceMonth) {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(amount.multiply(BigDecimal.valueOf(3))) // valor total (3 parcelas)
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

    /** Stub para mês sem parcelas de cartão e sem despesas à vista. */
    private void stubNoExpenses() {
        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026))
                .thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of());
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of());
    }

    // =========================================================================
    // TC-S01 — FIFTY_FIFTY simples
    // =========================================================================

    @Test
    @DisplayName("TC-S01: FIFTY_FIFTY — PersonA paga R$1000, PersonB paga R$0 → PersonB deve R$500")
    void fiftyFifty_personAPaysFull_personBOwes500() {
        // PersonA paga R$1000, despesa FIFTY_FIFTY (500 de cada)
        Transaction expense = cashExpense(new BigDecimal("1000.00"), SplitRule.FIFTY_FIFTY, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(expense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.settled()).isFalse();
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
        assertThat(result.amountOwed()).isEqualByComparingTo("500.00");
        assertThat(result.totalExpense()).isEqualByComparingTo("1000.00");

        // PersonA: pagou 1000, deveria 500 → saldo +500 (credora)
        assertThat(result.personA().totalPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personA().balance()).isEqualByComparingTo("500.00");

        // PersonB: pagou 0, deveria 500 → saldo -500 (devedora)
        assertThat(result.personB().totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personB().balance()).isEqualByComparingTo("-500.00");
    }

    // =========================================================================
    // TC-S02 — PERSON_A (despesa 100% atribuída a PersonA)
    // =========================================================================

    @Test
    @DisplayName("TC-S02: PERSON_A — PersonA paga R$500, despesa 100% dela → settled=true, amountOwed=null")
    void personA_paysFull_isSettled() {
        // Despesa PERSON_A: 100% é responsabilidade de PersonA
        // PersonA pagou exatamente o que deveria
        Transaction expense = cashExpense(new BigDecimal("500.00"), SplitRule.PERSON_A, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(expense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        assertThat(result.settled()).isTrue();
        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.amountOwed()).isNull();
        assertThat(result.debtor()).isNull();
        assertThat(result.creditor()).isNull();

        // PersonA: pagou 500, deveria 500 → saldo 0
        assertThat(result.personA().totalPaid()).isEqualByComparingTo("500.00");
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personA().balance()).isEqualByComparingTo("0.00");

        // PersonB: pagou 0, deveria 0 → saldo 0
        assertThat(result.personB().totalPaid()).isEqualByComparingTo("0.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("0.00");
    }

    // =========================================================================
    // TC-S03 — PERSON_B (despesa 100% atribuída a PersonB)
    // =========================================================================

    @Test
    @DisplayName("TC-S03: PERSON_B — PersonB paga R$800, despesa 100% dela → settled=true")
    void personB_paysFull_isSettled() {
        // Despesa PERSON_B: 100% responsabilidade de PersonB; PersonB pagou
        Transaction expense = cashExpense(new BigDecimal("800.00"), SplitRule.PERSON_B, personB);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(expense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        assertThat(result.settled()).isTrue();
        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.amountOwed()).isNull();

        // PersonB: pagou 800, deveria 800 → quitado
        assertThat(result.personB().totalPaid()).isEqualByComparingTo("800.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("800.00");
        assertThat(result.personB().balance()).isEqualByComparingTo("0.00");
    }

    // =========================================================================
    // TC-S04 — PROPORTIONAL com receitas lançadas
    // =========================================================================

    @Test
    @DisplayName("TC-S04: PROPORTIONAL — IncomeA=R$6000 (60%), IncomeB=R$4000 (40%), despesa R$1000 paga por PersonA → PersonB deve R$400")
    void proportional_withIncomes_personBOwes400() {
        // Receitas individuais: Alice 6000 (60%), Bob 4000 (40%)
        Transaction incomeA = income(new BigDecimal("6000.00"), SplitRule.PERSON_A, personA);
        Transaction incomeB = income(new BigDecimal("4000.00"), SplitRule.PERSON_B, personB);

        // Despesa R$1000 PROPORTIONAL, paga por PersonA
        Transaction expense = cashExpense(new BigDecimal("1000.00"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(expense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(incomeA, incomeB));

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.settled()).isFalse();

        // PersonA deveria pagar 60% = R$600; PersonB 40% = R$400
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("600.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("400.00");

        // PersonA pagou 1000, deveria 600 → balance +400 (credora)
        assertThat(result.personA().totalPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.personA().balance()).isEqualByComparingTo("400.00");

        // PersonB pagou 0, deveria 400 → deve R$400 a PersonA
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
        // Sem receitas individuais no mês
        Transaction expense = cashExpense(new BigDecimal("500.00"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(expense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of()); // sem receitas

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        assertThat(result.pendingProportional()).isTrue();
        assertThat(result.amountOwed()).isNull();
        assertThat(result.debtor()).isNull();
        assertThat(result.creditor()).isNull();
        assertThat(result.settled()).isFalse();
        assertThat(result.pendingMessage()).isNotBlank();
        // Mensagem deve orientar o usuário a cadastrar receitas
        assertThat(result.pendingMessage()).containsIgnoringCase("receitas");
    }

    // =========================================================================
    // TC-S06 — MIX: FIFTY_FIFTY + PROPORTIONAL sem receita → acerto inteiro pendente
    // =========================================================================

    @Test
    @DisplayName("TC-S06: FIFTY_FIFTY + PROPORTIONAL sem receita → acerto pendente (PROPORTIONAL bloqueia tudo)")
    void mix_fiftyFiftyAndProportionalWithoutIncome_entireSettlementIsPending() {
        // Despesa R$200 FIFTY_FIFTY paga por PersonA
        Transaction fiftyFiftyExpense = cashExpense(new BigDecimal("200.00"), SplitRule.FIFTY_FIFTY, personA);
        // Despesa R$300 PROPORTIONAL paga por PersonB (sem receitas)
        Transaction proportionalExpense = cashExpense(new BigDecimal("300.00"), SplitRule.PROPORTIONAL, personB);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(fiftyFiftyExpense, proportionalExpense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of()); // sem receitas individuais

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        // A presença de qualquer PROPORTIONAL pendente torna o acerto inteiro pendente
        assertThat(result.pendingProportional()).isTrue();
        assertThat(result.amountOwed()).isNull();
        assertThat(result.debtor()).isNull();
        assertThat(result.settled()).isFalse();
        // O totalExpense ainda deve ser computado (soma de todas as despesas)
        assertThat(result.totalExpense()).isEqualByComparingTo("500.00");
    }

    // =========================================================================
    // TC-S07 — Mês sem despesas
    // =========================================================================

    @Test
    @DisplayName("TC-S07: mês sem despesas → settled=true, totalExpense=0, amountOwed=null")
    void noExpenses_returnsSettledWithZero() {
        stubNoExpenses();

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

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
    @DisplayName("TC-S08: parcela de cartão (R$100 em fev/2026) entra no acerto de fev, não o total da compra")
    void installment_onlyCurrentMonthInstallmentEntersSettlement() {
        // Compra de R$300 em 3x; a parcela de fevereiro (R$100) cai em 2026-02
        LocalDate FEB_2026     = LocalDate.of(2026, 2, 1);
        LocalDate FEB_2026_END = LocalDate.of(2026, 2, 28);

        // Cria a Installment do mês de fevereiro (R$100), pagamento por PersonA
        Installment febInstallment = installment(
                new BigDecimal("100.00"), SplitRule.FIFTY_FIFTY, personA, FEB_2026);

        when(installmentRepository.findExpenseInstallmentsByMonth(FEB_2026))
                .thenReturn(List.of(febInstallment));
        when(transactionRepository.findCashExpensesByMonth(FEB_2026, FEB_2026_END))
                .thenReturn(List.of());
        when(transactionRepository.findIncomesByMonth(FEB_2026, FEB_2026_END))
                .thenReturn(List.of());

        SettlementResponse result = settlementService.calculate(FEB_2026, personAId, personBId);

        // totalExpense deve ser R$100 (somente a parcela de fevereiro)
        assertThat(result.totalExpense()).isEqualByComparingTo("100.00");
        assertThat(result.pendingProportional()).isFalse();
        assertThat(result.settled()).isFalse();

        // PersonA pagou R$100, deveria R$50 → PersonB deve R$50
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
        assertThat(result.amountOwed()).isEqualByComparingTo("50.00");
    }

    // =========================================================================
    // TC-S09 — Receita FIFTY_FIFTY NÃO entra na proporção
    // =========================================================================

    @Test
    @DisplayName("TC-S09: receita FIFTY_FIFTY não conta para a proporção — apenas receitas individuais")
    void fiftyFiftyIncome_doesNotAffectProportionalRatio() {
        // IncomeA R$3000 (PERSON_A), IncomeB R$3000 (PERSON_B), IncomeShared R$4000 (FIFTY_FIFTY)
        // Apenas IncomeA e IncomeB entram na proporção → 50%/50%
        Transaction incomeA = income(new BigDecimal("3000.00"), SplitRule.PERSON_A, personA);
        Transaction incomeB = income(new BigDecimal("3000.00"), SplitRule.PERSON_B, personB);
        Transaction incomeShared = income(new BigDecimal("4000.00"), SplitRule.FIFTY_FIFTY, personA);

        // Despesa R$1000 PROPORTIONAL, paga por PersonA
        Transaction expense = cashExpense(new BigDecimal("1000.00"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(expense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(incomeA, incomeB, incomeShared));

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        assertThat(result.pendingProportional()).isFalse();

        // Proporção deve ser 50%/50% (3000/(3000+3000) = 50%) — ignora a receita FIFTY_FIFTY
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("500.00");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("500.00");

        // PersonA pagou 1000, deveria 500 → PersonB deve 500
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
        assertThat(result.amountOwed()).isEqualByComparingTo("500.00");
    }

    // =========================================================================
    // TC-S10 — Arredondamento PROPORTIONAL (HALF_UP consistente)
    // =========================================================================

    @Test
    @DisplayName("TC-S10: PROPORTIONAL arredondamento — R$100.01 com ratio 60%/40% → shareA=R$60.01, shareB=R$40.00")
    void proportional_rounding_halfUp_consistent() {
        // Receitas: Alice R$6000 (60%), Bob R$4000 (40%)
        Transaction incomeA = income(new BigDecimal("6000.00"), SplitRule.PERSON_A, personA);
        Transaction incomeB = income(new BigDecimal("4000.00"), SplitRule.PERSON_B, personB);

        // Despesa R$100.01 PROPORTIONAL, paga por PersonA
        Transaction expense = cashExpense(new BigDecimal("100.01"), SplitRule.PROPORTIONAL, personA);

        when(installmentRepository.findExpenseInstallmentsByMonth(MAY_2026)).thenReturn(List.of());
        when(transactionRepository.findCashExpensesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(expense));
        when(transactionRepository.findIncomesByMonth(MAY_2026, MAY_2026_END))
                .thenReturn(List.of(incomeA, incomeB));

        SettlementResponse result = settlementService.calculate(MAY_2026, personAId, personBId);

        assertThat(result.pendingProportional()).isFalse();

        // shareA = 100.01 * 0.60 = 60.006 → HALF_UP → 60.01
        // shareB = 100.01 - 60.01 = 40.00
        assertThat(result.personA().shouldPay()).isEqualByComparingTo("60.01");
        assertThat(result.personB().shouldPay()).isEqualByComparingTo("40.00");

        // Soma das fatias = 60.01 + 40.00 = 100.01 (igual ao total da despesa)
        BigDecimal totalShares = result.personA().shouldPay().add(result.personB().shouldPay());
        assertThat(totalShares).isEqualByComparingTo("100.01");

        // PersonA pagou 100.01, deveria 60.01 → PersonB deve 40.00
        assertThat(result.amountOwed()).isEqualByComparingTo("40.00");
        assertThat(result.debtor()).isEqualTo("PERSON_B");
        assertThat(result.creditor()).isEqualTo("PERSON_A");
    }

    // =========================================================================
    // Testes unitários do método calculateShares (via acesso package-private)
    // =========================================================================

    @Test
    @DisplayName("calculateShares — PROPORTIONAL sem receita retorna null (sinaliza pendência)")
    void calculateShares_proportional_noIncome_returnsNull() {
        BigDecimal[] shares = settlementService.calculateShares(
                new BigDecimal("500.00"),
                SplitRule.PROPORTIONAL,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false // hasIndividualIncome = false
        );

        assertThat(shares).isNull();
    }

    @Test
    @DisplayName("calculateShares — FIFTY_FIFTY com valor ímpar distribui sem perder centavo")
    void calculateShares_fiftyFifty_oddAmount_noLostCent() {
        // R$1.01 / 2 = 0.505 → HALF_UP → 0.51 e 0.50 (soma = 1.01)
        BigDecimal[] shares = settlementService.calculateShares(
                new BigDecimal("1.01"),
                SplitRule.FIFTY_FIFTY,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false
        );

        assertThat(shares).isNotNull().hasSize(2);
        BigDecimal soma = shares[0].add(shares[1]);
        assertThat(soma).isEqualByComparingTo("1.01");
    }
}
