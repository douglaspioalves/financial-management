package com.gastos.service;

import com.gastos.domain.Card;
import com.gastos.domain.Person;
import com.gastos.dto.card.CardRequest;
import com.gastos.dto.card.CardResponse;
import com.gastos.repository.CardRepository;
import com.gastos.repository.PersonRepository;
import com.gastos.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final PersonRepository personRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CardResponse> findAll() {
        return cardRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse findById(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));
        return toResponse(card);
    }

    @Transactional
    public CardResponse create(CardRequest request) {
        Person owner = personRepository.findById(request.ownerPersonId())
                .orElseThrow(() -> new EntityNotFoundException("Participante não encontrado."));

        Card card = Card.builder()
                .name(request.name())
                .owner(owner)
                .closingDay(request.closingDay())
                .dueDay(request.dueDay())
                .build();

        Card saved = cardRepository.save(card);
        return toResponse(saved);
    }

    @Transactional
    public CardResponse update(UUID id, CardRequest request) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));

        Person owner = personRepository.findById(request.ownerPersonId())
                .orElseThrow(() -> new EntityNotFoundException("Participante não encontrado."));

        card.setName(request.name());
        card.setOwner(owner);
        card.setClosingDay(request.closingDay());
        card.setDueDay(request.dueDay());

        Card saved = cardRepository.save(card);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));

        if (transactionRepository.existsByCardId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este cartão possui lançamentos vinculados e não pode ser excluído. Remova os lançamentos antes de excluir o cartão.");
        }

        cardRepository.delete(card);
    }

    private CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getId(),
                card.getName(),
                card.getOwner().getId(),
                card.getOwner().getName(),
                card.getClosingDay(),
                card.getDueDay(),
                card.getVersion()
        );
    }
}
