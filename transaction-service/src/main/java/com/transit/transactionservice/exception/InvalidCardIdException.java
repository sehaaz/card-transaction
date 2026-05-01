package com.transit.transactionservice.exception;

public class InvalidCardIdException extends RuntimeException {
    public InvalidCardIdException() {
        super("Card ID must not be null");
    }
}
