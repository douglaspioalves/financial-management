package com.gastos.service;

import com.gastos.domain.Card;
import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.domain.Installment;
import com.gastos.domain.PaymentMethod;
import com.gastos.domain.Person;
import com.gastos.domain.SplitRule;
import com.gastos.domain.Transaction;
import com.gastos.domain.TransactionType;
import com.gastos.repository.InstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do InstallmentService — algoritmo de geração de parcelas.
 *
 * Algoritmo esperado:
 *
 *   firstReferenceMonth:
 *     SE purchaseDate.day < closingDay  → mesmo mês da compra (dia 1)
 *     SE purchaseDate.day >= closingDay → mês seguinte (dia 1)
 *
 *   Para parcela i (1-based): referenceMonth = firstReferenceMonth + (i-1) meses
 *
 *   Arredondamento:
 *     - (N-1) parcelas = FLOOR(total / N, 2 casas decimais)
 *     - última parcela = total - sum(primeiras N-1)
 */
@ExtendWith(MockitoExtension.class)
class InstallmentServiceTest {

    @Mock
    private InstallmentRepository installmentRepository;

    @InjectMocks
    private InstallmentService installmentService;

    // Fixtures compartilhadas
    private Person person;
    private Category category;

    @BeforeEach
    void setUp() {
        person = Person.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .color("#4a7fc4")
                .version(0L)
                .build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Compras")
                .type(CategoryType.EXPENSE)
                .color("#e88a74")
                .version(0L)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Transaction buildTransaction(LocalDate date, int installmentsTotal, BigDecimal amount, Card card) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .date(date)
                .category(category)
                .paidByPerson(person)
                .paymentMethod(PaymentMethod.CREDIT)
                .cardId(card.getId())
                .splitRule(SplitRule.FIFTY_FIFTY)
                .installmentsTotal(installmentsTotal)
                .version(0L)
                .build();
    }

    private Card buildCard(int closingDay) {
        return Card.builder()
                .id(UUID.randomUUID())
                .name("Cartão Teste")
                .owner(person)
                .closingDay(closingDay)
                .dueDay(closingDay + 7 > 28 ? 7 : closingDay + 7)
                .version(0L)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Installment> captureInstallments() {
        ArgumentCaptor<List<Installment>> captor = ArgumentCaptor.forClass(List.class);
        verify(installmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // -------------------------------------------------------------------------
    // TC-01: compra antes do fechamento → primeiro mês = mesmo mês da compra
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-01: generateInstallments — before closing day → first reference month is same month")
    void generateInstallments_beforeClosingDay_firstMonthIsSameMonth() {
        // Compra: 2026-05-15, closingDay=20, 3x, R$1500.00
        // dia 15 < closingDay 20 → firstMonth = 2026-05-01
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 5, 15), 3, new BigDecimal("1500.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(3)
                .as("Deve gerar exatamente 3 parcelas");

        assertThat(result.get(0).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 5, 1))
                .as("1a parcela: mesmo mês da compra (dia 15 < fechamento 20)");
        assertThat(result.get(1).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 6, 1))
                .as("2a parcela: mês seguinte");
        assertThat(result.get(2).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 7, 1))
                .as("3a parcela: dois meses depois");

        assertThat(result.get(0).getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.get(1).getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.get(2).getAmount()).isEqualByComparingTo("500.00");

        // Soma das parcelas deve ser igual ao total
        BigDecimal soma = result.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("1500.00")
                .as("Soma das parcelas deve ser igual ao valor total da compra");
    }

    // -------------------------------------------------------------------------
    // TC-02: compra após o fechamento → primeiro mês = mês seguinte
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-02: generateInstallments — on or after closing day → first reference month is next month")
    void generateInstallments_onOrAfterClosingDay_firstMonthIsNextMonth() {
        // Compra: 2026-05-25, closingDay=20, 3x, R$1500.00
        // dia 25 >= closingDay 20 → firstMonth = 2026-06-01
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 5, 25), 3, new BigDecimal("1500.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 6, 1))
                .as("1a parcela: mês seguinte (dia 25 >= fechamento 20)");
        assertThat(result.get(1).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.get(2).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 8, 1));
    }

    // -------------------------------------------------------------------------
    // TC-03: compra exatamente no dia de fechamento → mês seguinte
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-03: generateInstallments — exactly on closing day → first reference month is next month")
    void generateInstallments_exactlyOnClosingDay_firstMonthIsNextMonth() {
        // Compra: 2026-05-20, closingDay=20, 2x, R$200.00
        // dia 20 == closingDay 20 → firstMonth = 2026-06-01
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 5, 20), 2, new BigDecimal("200.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 6, 1))
                .as("1a parcela: mês seguinte (dia == fechamento)");
        assertThat(result.get(1).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 7, 1));
    }

    // -------------------------------------------------------------------------
    // TC-04: virada de ano — compra em novembro após fechamento (dez/jan/fev)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-04: generateInstallments — year rollover — November purchase after closing crosses December to January")
    void generateInstallments_yearRollover_crossesDecemberToJanuary() {
        // Compra: 2026-11-25, closingDay=20, 3x, R$300.00
        // dia 25 >= 20 → firstMonth = 2026-12-01
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 11, 25), 3, new BigDecimal("300.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 12, 1))
                .as("1a parcela: dezembro 2026");
        assertThat(result.get(1).getReferenceMonth())
                .isEqualTo(LocalDate.of(2027, 1, 1))
                .as("2a parcela: janeiro 2027 (virada de ano)");
        assertThat(result.get(2).getReferenceMonth())
                .isEqualTo(LocalDate.of(2027, 2, 1))
                .as("3a parcela: fevereiro 2027");
    }

    // -------------------------------------------------------------------------
    // TC-05: virada de ano — compra em novembro antes do fechamento (nov/dez/jan)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-05: generateInstallments — year rollover — November purchase before closing spans November to January")
    void generateInstallments_yearRollover_november_before_closing() {
        // Compra: 2026-11-10, closingDay=20, 3x, R$300.00
        // dia 10 < 20 → firstMonth = 2026-11-01
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 11, 10), 3, new BigDecimal("300.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 11, 1))
                .as("1a parcela: novembro 2026");
        assertThat(result.get(1).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 12, 1))
                .as("2a parcela: dezembro 2026");
        assertThat(result.get(2).getReferenceMonth())
                .isEqualTo(LocalDate.of(2027, 1, 1))
                .as("3a parcela: janeiro 2027 (virada de ano)");
    }

    // -------------------------------------------------------------------------
    // TC-06: arredondamento — 3 parcelas de valor não divisível exato
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-06: generateInstallments — rounding — last installment absorbs the difference (3 installments)")
    void generateInstallments_rounding_lastInstallmentAbsorbsDifference() {
        // Compra: 2026-05-01, closingDay=20, 3x, R$100.00
        // 100.00 / 3 = 33.333... → floor(2 casas) = 33.33
        // Parcelas: [33.33, 33.33, 33.34]  (última = 100.00 - 33.33 - 33.33 = 33.34)
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 5, 1), 3, new BigDecimal("100.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAmount())
                .isEqualByComparingTo("33.33")
                .as("1a parcela: floor(100.00/3, 2 decimais) = 33.33");
        assertThat(result.get(1).getAmount())
                .isEqualByComparingTo("33.33")
                .as("2a parcela: floor(100.00/3, 2 decimais) = 33.33");
        assertThat(result.get(2).getAmount())
                .isEqualByComparingTo("33.34")
                .as("3a parcela: 100.00 - 33.33 - 33.33 = 33.34 (absorve diferença)");

        BigDecimal soma = result.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("100.00")
                .as("Soma das parcelas deve ser exatamente 100.00");
    }

    // -------------------------------------------------------------------------
    // TC-07: arredondamento — 2 parcelas onde floor resulta em perda de 1 centavo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-07: generateInstallments — rounding — two installments where floor leaves 1 cent in last")
    void generateInstallments_rounding_twoInstallments() {
        // Compra: 2026-05-01, closingDay=20, 2x, R$99.99
        // 99.99 / 2 = 49.995 → floor(2 casas) = 49.99
        // Parcelas: [49.99, 50.00]  (última = 99.99 - 49.99 = 50.00)
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 5, 1), 2, new BigDecimal("99.99"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAmount())
                .isEqualByComparingTo("49.99")
                .as("1a parcela: floor(99.99/2, 2 decimais) = 49.99");
        assertThat(result.get(1).getAmount())
                .isEqualByComparingTo("50.00")
                .as("2a (última) parcela: 99.99 - 49.99 = 50.00");

        BigDecimal soma = result.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("99.99")
                .as("Soma das parcelas deve ser exatamente 99.99");
    }

    // -------------------------------------------------------------------------
    // TC-08: parcela única — sem divisão, amount = valor total
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-08: generateInstallments — single installment — amount equals total, no splitting")
    void generateInstallments_singleInstallment_noSplitting() {
        // Compra: 2026-05-01, closingDay=20, 1x, R$500.00
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 5, 1), 1, new BigDecimal("500.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(1)
                .as("Deve gerar exatamente 1 parcela");
        assertThat(result.get(0).getAmount())
                .isEqualByComparingTo("500.00")
                .as("Parcela única deve ter o valor total da compra");
        assertThat(result.get(0).getNumber())
                .isEqualTo(1)
                .as("Número da parcela deve ser 1");
    }

    // -------------------------------------------------------------------------
    // TC-09: closingDay=31 em mês curto — não deve lançar exceção
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-09: generateInstallments — closing day 31 in short month — does not break")
    void generateInstallments_closingDay31_shortMonth() {
        // Compra: 2026-03-15, closingDay=31, 2x
        // dia 15 < 31 → firstMonth = 2026-03-01
        // Valida que não lança exceção para fechamento=31 em meses com menos dias
        Card card = buildCard(31);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 3, 15), 2, new BigDecimal("200.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(2)
                .as("Deve gerar 2 parcelas sem exceção mesmo com closingDay=31");
        assertThat(result.get(0).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 3, 1))
                .as("1a parcela: março 2026 (dia 15 < fechamento 31)");
        assertThat(result.get(1).getReferenceMonth())
                .isEqualTo(LocalDate.of(2026, 4, 1))
                .as("2a parcela: abril 2026");
    }

    // -------------------------------------------------------------------------
    // TC-10: contagem total — 12 parcelas geradas corretamente
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-10: generateInstallments — 12 installments — correct count and saveAll called with full list")
    void generateInstallments_totalInstallmentsCount() {
        // Compra: 2026-01-01, closingDay=20, 12x, R$1200.00
        // dia 1 < 20 → firstMonth = 2026-01-01
        // 1200.00 / 12 = 100.00 exato
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1200.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(12)
                .as("Deve gerar exatamente 12 parcelas");

        // Verifica que todas as parcelas têm amount correto
        result.forEach(installment ->
                assertThat(installment.getAmount())
                        .isEqualByComparingTo("100.00")
                        .as("Cada parcela deve ter R$100.00 (divisão exata)")
        );

        // Verifica numeração sequencial de 1 a 12
        for (int i = 0; i < 12; i++) {
            assertThat(result.get(i).getNumber())
                    .isEqualTo(i + 1)
                    .as("Parcela %d deve ter número %d", i + 1, i + 1);
        }

        // Verifica meses sequenciais: jan/2026 a dez/2026
        for (int i = 0; i < 12; i++) {
            assertThat(result.get(i).getReferenceMonth())
                    .isEqualTo(LocalDate.of(2026, i + 1, 1))
                    .as("Parcela %d deve ter referenceMonth %d/2026", i + 1, i + 1);
        }

        // Verifica que saveAll foi chamado com lista de 12 elementos
        List<Installment> captured = captureInstallments();
        assertThat(captured).hasSize(12)
                .as("installmentRepository.saveAll deve ser chamado com lista de 12 elementos");

        // Verifica soma total
        BigDecimal soma = result.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("1200.00")
                .as("Soma das 12 parcelas deve ser exatamente R$1200.00");
    }

    // -------------------------------------------------------------------------
    // TC-EXTRA-01: vínculo transaction → installment (número e referência ao pai)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-EXTRA-01: generateInstallments — each installment references the parent transaction")
    void generateInstallments_eachInstallmentReferencesParentTransaction() {
        Card card = buildCard(20);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 5, 1), 3, new BigDecimal("300.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        assertThat(result).hasSize(3);
        result.forEach(installment ->
                assertThat(installment.getTransaction())
                        .isSameAs(transaction)
                        .as("Cada Installment deve referenciar a Transaction pai")
        );
    }

    // -------------------------------------------------------------------------
    // TC-EXTRA-02: referenceMonth sempre cai no dia 1 do mês
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-EXTRA-02: generateInstallments — referenceMonth is always the first day of the month")
    void generateInstallments_referenceMonthAlwaysFirstDay() {
        Card card = buildCard(15);
        Transaction transaction = buildTransaction(
                LocalDate.of(2026, 6, 20), 4, new BigDecimal("400.00"), card);

        when(installmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Installment> result = installmentService.generateInstallments(transaction, card);

        result.forEach(installment ->
                assertThat(installment.getReferenceMonth().getDayOfMonth())
                        .isEqualTo(1)
                        .as("referenceMonth deve ser sempre o dia 1 do mês")
        );
    }
}
