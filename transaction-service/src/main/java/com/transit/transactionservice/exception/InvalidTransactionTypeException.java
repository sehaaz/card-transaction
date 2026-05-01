package com.transit.transactionservice.exception;

public class InvalidTransactionTypeException extends RuntimeException {
    public InvalidTransactionTypeException(String type) {
        super("Invalid transaction type: '" + type + "'. Allowed values are TOPUP and PAYMENT");
    }
}
