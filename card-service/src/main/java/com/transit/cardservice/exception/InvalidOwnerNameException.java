package com.transit.cardservice.exception;

public class InvalidOwnerNameException extends RuntimeException {
    public InvalidOwnerNameException() {
        super("Owner name must not be null or empty");
    }
}
