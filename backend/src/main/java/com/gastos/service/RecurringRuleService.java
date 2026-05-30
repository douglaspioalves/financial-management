package com.gastos.service;

import com.gastos.domain.Category;
import com.gastos.domain.Person;
import com.gastos.domain.RecurringFrequency;
import com.gastos.domain.RecurringRule;
import com.gastos.domain.Transaction;
import com.gastos.dto.recurring.RecurringRuleRequest;
import com.gastos.dto.recurring.RecurringRuleResponse;
import com.gastos.repository.CategoryRepository;
import com.gastos.repository.PersonRepository;
import com.gastos.repository.RecurringRuleRepository;
import com.gastos.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringRuleService {

    private final RecurringRuleRepository recurringRuleRepository;
    private final CategoryRepository categoryRepository;
    private final PersonRepository personRepository;
    private final TransactionRepository transactionRepository;

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RecurringRuleResponse> getAll() {
        return recurringRuleRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Criação
    // -------------------------------------------------------------------------

    @Transactional
    public RecurringRuleResponse create(RecurringRuleRequest request) {
        Category category = categoryRepository.findByIdAndInactiveFalse(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));

        Person paidByPerson = personRepository.findById(request.paidByPersonId())
                .orElseThrow(() -> new EntityNotFoundException("Participante não encontrado."));

        RecurringRule rule = RecurringRule.builder()
                .type(request.type())
                .amount(request.amount())
                .description(request.description())
                .category(category)
                .paidByPerson(paidByPerson)
                .paymentMethod(request.paymentMethod())
                .splitRule(request.splitRule())
                .frequency(request.frequency())
                .nextDate(request.nextDate())
                .build();

        RecurringRule saved = recurringRuleRepository.save(rule);
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Desativação (soft delete)
    // -------------------------------------------------------------------------

    @Transactional
    public void deactivate(UUID id) {
        RecurringRule rule = recurringRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Regra recorrente não encontrada."));
        rule.setActive(false);
        recurringRuleRepository.save(rule);
    }

    // -------------------------------------------------------------------------
    // Job de geração de lançamentos
    // -------------------------------------------------------------------------

    /**
     * Executa diariamente às 06:00 AM.
     * Para cada regra ativa com nextDate <= hoje, cria um Transaction
     * correspondente e avança nextDate conforme a frequência.
     * Idempotência: verifica se já existe transação com mesma descrição, valor,
     * data e categoria criada hoje para evitar duplicatas caso o job rode mais de uma vez.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void generateDueTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringRule> dueRules = recurringRuleRepository.findByActiveTrueAndNextDateLessThanEqual(today);

        log.info("Job de recorrência: {} regra(s) vencida(s) para processar.", dueRules.size());

        for (RecurringRule rule : dueRules) {
            try {
                processRule(rule, today);
            } catch (Exception e) {
                log.error("Erro ao processar regra recorrente id={}: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void processRule(RecurringRule rule, LocalDate today) {
        LocalDate transactionDate = rule.getNextDate();

        // Idempotência: evita duplicar se o job rodar mais de uma vez no mesmo dia
        boolean alreadyExists = transactionRepository.existsByDescriptionAndAmountAndDateAndCategoryId(
                rule.getDescription(),
                rule.getAmount(),
                transactionDate,
                rule.getCategory().getId()
        );

        if (alreadyExists) {
            log.info("Lançamento recorrente já existe para regra id={} na data {}. Pulando.", rule.getId(), transactionDate);
        } else {
            Transaction transaction = Transaction.builder()
                    .type(rule.getType())
                    .amount(rule.getAmount())
                    .date(transactionDate)
                    .description(rule.getDescription())
                    .category(rule.getCategory())
                    .paidByPerson(rule.getPaidByPerson())
                    .paymentMethod(rule.getPaymentMethod())
                    .splitRule(rule.getSplitRule())
                    .installmentsTotal(1)
                    .build();

            transactionRepository.save(transaction);
            log.info("Lançamento recorrente criado para regra id={} na data {}.", rule.getId(), transactionDate);
        }

        // Avança nextDate independente de criação (evita reprocessamento infinito)
        rule.setNextDate(advanceDate(transactionDate, rule.getFrequency()));
        recurringRuleRepository.save(rule);
    }

    private LocalDate advanceDate(LocalDate date, RecurringFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> date.plusMonths(1);
            case WEEKLY -> date.plusWeeks(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    private RecurringRuleResponse toResponse(RecurringRule rule) {
        return new RecurringRuleResponse(
                rule.getId(),
                rule.getType(),
                rule.getAmount(),
                rule.getDescription(),
                rule.getCategory().getId(),
                rule.getCategory().getName(),
                rule.getPaidByPerson().getId(),
                rule.getPaidByPerson().getName(),
                rule.getPaymentMethod(),
                rule.getSplitRule(),
                rule.getFrequency(),
                rule.getNextDate(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getVersion()
        );
    }
}
