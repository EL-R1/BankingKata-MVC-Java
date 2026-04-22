package com.banking.exception;

public class DepositLimitExceededException extends RuntimeException {
    public DepositLimitExceededException(String message) {
        super(message);
    }
}