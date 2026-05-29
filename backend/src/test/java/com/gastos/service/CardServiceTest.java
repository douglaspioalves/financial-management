package com.gastos.service;

import com.gastos.domain.Card;
import com.gastos.domain.Person;
import com.gastos.dto.card.CardRequest;
import com.gastos.dto.card.CardResponse;
import com.gastos.repository.CardRepository;
import com.gastos.repository.PersonRepository;
import com.gastos.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CardService cardService;

    private Person personA;
    private Person personB;
    private Card cardA;
    private Card cardB;

    @BeforeEach
    void setUp() {
        personA = Person.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .color("#4a7fc4")
                .version(0L)
                .build();

        personB = Person.builder()
                .id(UUID.randomUUID())
                .name("Bob")
                .color("#e88a74")
                .version(0L)
                .build();

        cardA = Card.builder()
                .id(UUID.randomUUID())
                .name("Nubank Alice")
                .owner(personA)
                .closingDay(10)
                .dueDay(17)
                .version(0L)
                .build();

        cardB = Card.builder()
                .id(UUID.randomUUID())
                .name("XP Bob")
                .owner(personB)
                .closingDay(20)
                .dueDay(27)
                .version(0L)
                .build();
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAll returns list ordered by name as provided by repository")
    void findAll_returnsListOrderedByName() {
        when(cardRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cardA, cardB));

        List<CardResponse> result = cardService.findAll();

        assertThat(result).hasSize(2)
                .as("Deve retornar a lista completa de cartões");
        assertThat(result.get(0).name()).isEqualTo("Nubank Alice")
                .as("Primeiro cartão deve ser o retornado pelo repositório na posição 0");
        assertThat(result.get(1).name()).isEqualTo("XP Bob")
                .as("Segundo cartão deve ser o retornado pelo repositório na posição 1");
        verify(cardRepository).findAllByOrderByNameAsc();
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findById returns CardResponse with ownerPersonName when card exists")
    void findById_withValidId_returnsCard() {
        when(cardRepository.findById(cardA.getId())).thenReturn(Optional.of(cardA));

        CardResponse result = cardService.findById(cardA.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(cardA.getId());
        assertThat(result.name()).isEqualTo("Nubank Alice");
        assertThat(result.ownerPersonId()).isEqualTo(personA.getId());
        assertThat(result.ownerPersonName()).isEqualTo("Alice")
                .as("ownerPersonName deve vir populado a partir da entidade owner");
        assertThat(result.closingDay()).isEqualTo(10);
        assertThat(result.dueDay()).isEqualTo(17);
        assertThat(result.version()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findById throws EntityNotFoundException when card does not exist")
    void findById_withUnknownId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(cardRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.findById(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cartão não encontrado")
                .as("Deve lançar EntityNotFoundException quando cartão não existe");
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create saves card and returns CardResponse when person exists")
    void create_withValidData_returnsCardResponse() {
        CardRequest request = new CardRequest("Nubank Alice", personA.getId(), 10, 17);

        Card saved = Card.builder()
                .id(UUID.randomUUID())
                .name("Nubank Alice")
                .owner(personA)
                .closingDay(10)
                .dueDay(17)
                .version(0L)
                .build();

        when(personRepository.findById(personA.getId())).thenReturn(Optional.of(personA));
        when(cardRepository.save(any(Card.class))).thenReturn(saved);

        CardResponse result = cardService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Nubank Alice");
        assertThat(result.ownerPersonId()).isEqualTo(personA.getId());
        assertThat(result.ownerPersonName()).isEqualTo("Alice");
        assertThat(result.closingDay()).isEqualTo(10);
        assertThat(result.dueDay()).isEqualTo(17);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("create throws EntityNotFoundException when personId does not exist")
    void create_withUnknownPerson_throwsNotFound() {
        UUID unknownPersonId = UUID.randomUUID();
        CardRequest request = new CardRequest("Cartão X", unknownPersonId, 5, 12);

        when(personRepository.findById(unknownPersonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Participante não encontrado")
                .as("Deve lançar EntityNotFoundException quando o participante não existe");

        verify(cardRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update modifies card fields and returns updated CardResponse")
    void update_withValidData_returnsUpdatedCard() {
        UUID cardId = cardA.getId();
        CardRequest request = new CardRequest("Nubank Alice Atualizado", personB.getId(), 15, 22);

        Card updated = Card.builder()
                .id(cardId)
                .name("Nubank Alice Atualizado")
                .owner(personB)
                .closingDay(15)
                .dueDay(22)
                .version(1L)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardA));
        when(personRepository.findById(personB.getId())).thenReturn(Optional.of(personB));
        when(cardRepository.save(any(Card.class))).thenReturn(updated);

        CardResponse result = cardService.update(cardId, request);

        assertThat(result.name()).isEqualTo("Nubank Alice Atualizado")
                .as("Deve atualizar o nome do cartão");
        assertThat(result.ownerPersonId()).isEqualTo(personB.getId())
                .as("Deve atualizar o proprietário do cartão");
        assertThat(result.closingDay()).isEqualTo(15)
                .as("Deve atualizar o dia de fechamento");
        assertThat(result.dueDay()).isEqualTo(22)
                .as("Deve atualizar o dia de vencimento");
        verify(cardRepository).save(cardA);
    }

    @Test
    @DisplayName("update throws EntityNotFoundException when card does not exist")
    void update_withUnknownCard_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        CardRequest request = new CardRequest("Qualquer", personA.getId(), 10, 17);

        when(cardRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.update(unknownId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cartão não encontrado")
                .as("Deve lançar EntityNotFoundException ao atualizar cartão inexistente");

        verify(cardRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete calls repository.delete when card has no linked transactions")
    void delete_withNoTransactions_deletesCard() {
        UUID cardId = cardA.getId();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardA));
        when(transactionRepository.existsByCardId(cardId)).thenReturn(false);

        cardService.delete(cardId);

        verify(cardRepository).delete(cardA);
    }

    @Test
    @DisplayName("delete throws 409 ResponseStatusException when card has linked transactions")
    void delete_withLinkedTransactions_throwsConflict() {
        UUID cardId = cardA.getId();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardA));
        when(transactionRepository.existsByCardId(cardId)).thenReturn(true);

        assertThatThrownBy(() -> cardService.delete(cardId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
                            .as("Status HTTP deve ser 409 CONFLICT");
                })
                .as("Deve lançar ResponseStatusException 409 quando há lançamentos vinculados");

        verify(cardRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete throws EntityNotFoundException when card does not exist")
    void delete_withUnknownCard_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();

        when(cardRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.delete(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cartão não encontrado")
                .as("Deve lançar EntityNotFoundException ao excluir cartão inexistente");

        verify(cardRepository, never()).delete(any());
        verify(transactionRepository, never()).existsByCardId(any());
    }
}
