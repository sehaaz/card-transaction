package com.transit.cardservice.service;

import com.transit.cardservice.entity.Card;
import com.transit.cardservice.exception.CardNotFoundException;
import com.transit.cardservice.exception.InvalidAmountException;
import com.transit.cardservice.exception.InvalidOwnerNameException;
import com.transit.cardservice.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    @Transactional
    public Card createCard(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new InvalidOwnerNameException();
        }
        Card card = Card.builder()
                .ownerName(ownerName.trim())
                .build();
        return cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public Card getCardById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    @Transactional
    public Card topUp(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidAmountException(amount);
        }
        Card card = getCardById(id);
        card.setBalance(card.getBalance().add(amount));
        return cardRepository.save(card);
    }
}
