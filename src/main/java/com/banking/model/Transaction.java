package com.banking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final String accountNumber;
    private final BigDecimal amount;
    private final TransactionType type;
    private final Instant date;
    private final BigDecimal balanceAfterTransaction;

    public Transaction(String accountNumber, BigDecimal amount, TransactionType type, BigDecimal balanceAfterTransaction) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        this.id = UUID.randomUUID();
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.type = type;
        this.date = Instant.now();
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public Instant getDate() {
        return date;
    }

    public BigDecimal getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }
}