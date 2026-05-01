package com.transit.transactionservice.service;

import com.transit.transactionservice.entity.Transaction;
import com.transit.transactionservice.exception.*;
import com.transit.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Set<String> VALID_TYPES = Set.of("TOPUP", "PAYMENT");

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    @Value("${card.service.url}")
    private String cardServiceUrl;

    @Transactional
    public Transaction recordTransaction(Long cardId, BigDecimal amount, String type) {
        if (cardId == null) {
            throw new InvalidCardIdException();
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (type == null || !VALID_TYPES.contains(type.toUpperCase())) {
            throw new InvalidTransactionTypeException(type);
        }

        String normalizedType = type.toUpperCase();

        if ("PAYMENT".equals(normalizedType)) {
            BigDecimal currentBalance = getCardBalance(cardId);
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientBalanceException(cardId, amount);
            }
        }

        Transaction transaction = Transaction.builder()
                .cardId(cardId)
                .amount(amount)
                .type(normalizedType)
                .build();
        Transaction saved = transactionRepository.save(transaction);

        BigDecimal topUpAmount = "TOPUP".equals(normalizedType) ? amount : amount.negate();
        updateCardBalance(cardId, topUpAmount);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByCardId(Long cardId) {
        return transactionRepository.findByCardId(cardId);
    }

    private BigDecimal getCardBalance(Long cardId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    cardServiceUrl + "/api/cards/{cardId}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {},
                    cardId);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new CardServiceException("Empty response when fetching card " + cardId);
            }
            return new BigDecimal(body.get("balance").toString());
        } catch (HttpClientErrorException.NotFound e) {
            throw new CardServiceException("Card not found with id: " + cardId);
        } catch (ResourceAccessException e) {
            throw new CardServiceException("Card service is unavailable", e);
        }
    }

    private void updateCardBalance(Long cardId, BigDecimal amount) {
        try {
            restTemplate.put(
                    cardServiceUrl + "/api/cards/{cardId}/topup",
                    Map.of("amount", amount),
                    cardId
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new CardServiceException("Card not found with id: " + cardId);
        } catch (ResourceAccessException e) {
            throw new CardServiceException("Card service is unavailable", e);
        } catch (HttpClientErrorException e) {
            throw new CardServiceException("Failed to update card balance: " + e.getStatusCode());
        }
    }
}
