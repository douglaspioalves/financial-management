package com.gastos.service;

import com.gastos.domain.Card;
import com.gastos.domain.Category;
import com.gastos.domain.CategoryType;
import com.gastos.domain.PaymentMethod;
import com.gastos.domain.Person;
import com.gastos.domain.Transaction;
import com.gastos.domain.TransactionType;
import com.gastos.dto.installment.InstallmentResponse;
import com.gastos.dto.transaction.CreateTransactionRequest;
import com.gastos.dto.transaction.TransactionResponse;
import com.gastos.dto.transaction.UpdateTransactionRequest;
import com.gastos.repository.CardRepository;
import com.gastos.repository.CategoryRepository;
import com.gastos.repository.InstallmentRepository;
import com.gastos.repository.PersonRepository;
import com.gastos.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final PersonRepository personRepository;
    private final CardRepository cardRepository;
    private final InstallmentRepository installmentRepository;
    private final InstallmentService installmentService;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        // Validações adicionais para parcelamento
        int installmentsTotal = request.installmentsTotal() != null ? request.installmentsTotal() : 1;
        validateInstallmentsRule(installmentsTotal, request.paymentMethod(), request.cardId());

        // Valida e busca categoria ativa
        Category category = categoryRepository.findByIdAndInactiveFalse(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        // Valida compatibilidade de tipo entre lançamento e categoria
        validateCategoryTypeCompatibility(request.type(), category.getType());

        // Valida e busca participante pagador
        Person paidByPerson = personRepository.findById(request.paidByPersonId())
                .orElseThrow(() -> new EntityNotFoundException("Participante não encontrado."));

        // Valida regra de cartão de crédito
        validateCardRule(request.paymentMethod(), request.cardId());

        Transaction transaction = Transaction.builder()
                .type(request.type())
                .amount(request.amount())
                .date(request.date())
                .description(request.description())
                .category(category)
                .paidByPerson(paidByPerson)
                .paymentMethod(request.paymentMethod())
                .cardId(request.cardId())
                .splitRule(request.splitRule())
                .installmentsTotal(installmentsTotal)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // Gera parcelas automaticamente para compras parceladas no crédito
        if (installmentsTotal > 1 && request.paymentMethod() == PaymentMethod.CREDIT) {
            Card card = cardRepository.findById(saved.getCardId())
                    .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));
            installmentService.generateInstallments(saved, card);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByMonth(String month) {
        YearMonth yearMonth = parseYearMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        return transactionRepository.findByDateBetweenOrderByDateAscCreatedAtAsc(start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse update(UUID id, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));

        // Optimistic locking: verifica versão manualmente
        if (!transaction.getVersion().equals(request.version())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Transaction.class, id);
        }

        // Lançamentos parcelados não podem ser editados
        if (transaction.getInstallmentsTotal() > 1) {
            throw new IllegalStateException(
                    "Lançamentos parcelados não podem ser editados. Exclua e recrie se necessário.");
        }

        // Valida e busca categoria ativa
        Category category = categoryRepository.findByIdAndInactiveFalse(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        // Valida compatibilidade de tipo entre lançamento e categoria
        validateCategoryTypeCompatibility(request.type(), category.getType());

        // Valida e busca participante pagador
        Person paidByPerson = personRepository.findById(request.paidByPersonId())
                .orElseThrow(() -> new EntityNotFoundException("Participante não encontrado."));

        // Valida regra de cartão de crédito
        validateCardRule(request.paymentMethod(), request.cardId());

        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setDescription(request.description());
        transaction.setCategory(category);
        transaction.setPaidByPerson(paidByPerson);
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setCardId(request.cardId());
        transaction.setSplitRule(request.splitRule());

        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));

        // Cascade ON DELETE CASCADE no banco cuida das installments automaticamente
        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> findInstallments(UUID transactionId) {
        // Verifica se a transação existe (404 se não)
        if (!transactionRepository.existsById(transactionId)) {
            throw new EntityNotFoundException("Lançamento não encontrado.");
        }

        return installmentRepository.findByTransactionIdOrderByNumberAsc(transactionId)
                .stream()
                .map(i -> new InstallmentResponse(
                        i.getId(),
                        i.getNumber(),
                        i.getAmount(),
                        i.getReferenceMonth()))
                .toList();
    }

    // --- métodos auxiliares ---

    private void validateInstallmentsRule(int installmentsTotal, PaymentMethod paymentMethod, UUID cardId) {
        if (installmentsTotal < 1) {
            throw new IllegalArgumentException("O número de parcelas deve ser pelo menos 1.");
        }
        if (installmentsTotal > 48) {
            throw new IllegalArgumentException("O número máximo de parcelas é 48.");
        }
        if (installmentsTotal >= 2 && paymentMethod != PaymentMethod.CREDIT) {
            throw new IllegalArgumentException(
                    "Compras parceladas só podem ser feitas com cartão de crédito.");
        }
        if (installmentsTotal >= 2 && cardId == null) {
            throw new IllegalArgumentException(
                    "Pagamento parcelado exige a seleção de um cartão de crédito.");
        }
    }

    private void validateCategoryTypeCompatibility(TransactionType transactionType, CategoryType categoryType) {
        boolean compatible = switch (transactionType) {
            case EXPENSE -> categoryType == CategoryType.EXPENSE || categoryType == CategoryType.BOTH;
            case INCOME  -> categoryType == CategoryType.INCOME  || categoryType == CategoryType.BOTH;
        };
        if (!compatible) {
            throw new IllegalStateException(
                    "A categoria selecionada não é compatível com o tipo do lançamento.");
        }
    }

    private void validateCardRule(PaymentMethod paymentMethod, UUID cardId) {
        if (paymentMethod == PaymentMethod.CREDIT && cardId == null) {
            throw new IllegalArgumentException(
                    "Pagamento com cartão de crédito exige a seleção de um cartão.");
        }
        if (paymentMethod != PaymentMethod.CREDIT && cardId != null) {
            throw new IllegalArgumentException(
                    "Cartão só pode ser informado para pagamentos com cartão de crédito.");
        }
    }

    private YearMonth parseYearMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException(
                    "Parâmetro 'month' é obrigatório no formato yyyy-MM.");
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Formato de mês inválido. Use yyyy-MM (ex.: 2026-05).");
        }
    }

    TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse.CategorySummary categorySummary = new TransactionResponse.CategorySummary(
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getCategory().getType(),
                transaction.getCategory().getColor()
        );

        TransactionResponse.PersonSummary personSummary = new TransactionResponse.PersonSummary(
                transaction.getPaidByPerson().getId(),
                transaction.getPaidByPerson().getName(),
                transaction.getPaidByPerson().getColor()
        );

        // Resolve o objeto card a partir do cardId armazenado na transaction
        TransactionResponse.CardSummary cardSummary = null;
        if (transaction.getCardId() != null) {
            cardSummary = cardRepository.findById(transaction.getCardId())
                    .map(c -> new TransactionResponse.CardSummary(c.getId(), c.getName()))
                    .orElse(null);
        }

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getDescription(),
                categorySummary,
                personSummary,
                transaction.getPaymentMethod(),
                cardSummary,
                transaction.getSplitRule(),
                transaction.getInstallmentsTotal(),
                transaction.getCreatedAt(),
                transaction.getVersion()
        );
    }
}
