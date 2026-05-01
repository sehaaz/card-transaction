package com.transit.transactionservice.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Long cardId, BigDecimal amount) {
        super("Insufficient balance on card " + cardId + " for payment of " + amount);
    }
}
