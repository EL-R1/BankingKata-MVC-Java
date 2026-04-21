package com.banking.model;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

public class BankAccount {
    private final ReentrantLock lock = new ReentrantLock();

    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal overdraftLimit;

    public BankAccount(String accountNumber, BigDecimal initialBalance, BigDecimal overdraftLimit) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }
        if (overdraftLimit != null && overdraftLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative");
        }

        this.accountNumber = accountNumber;
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.overdraftLimit = overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void deposit(BigDecimal amount) {
        lock.lock();
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Deposit amount must be positive");
            }
            balance = balance.add(amount);
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(BigDecimal amount) {
        lock.lock();
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be positive");
            }

            BigDecimal available = balance.add(overdraftLimit);
            if (amount.compareTo(available) > 0) {
                throw new IllegalStateException("Insufficient funds for withdrawal");
            }

            balance = balance.subtract(amount);
        } finally {
            lock.unlock();
        }
    }

    public void setOverdraftLimit(BigDecimal limit) {
        if (limit != null && limit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative");
        }
        this.overdraftLimit = limit != null ? limit : BigDecimal.ZERO;
    }
}