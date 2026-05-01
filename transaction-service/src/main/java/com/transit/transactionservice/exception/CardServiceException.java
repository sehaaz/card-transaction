package com.transit.transactionservice.exception;

public class CardServiceException extends RuntimeException {
    public CardServiceException(String message) {
        super("Card service error: " + message);
    }

    public CardServiceException(String message, Throwable cause) {
        super("Card service error: " + message, cause);
    }
}
