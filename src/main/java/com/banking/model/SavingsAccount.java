package com.banking.model;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

public class SavingsAccount {
    private final ReentrantLock lock = new ReentrantLock();

    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal depositCeiling;

    public SavingsAccount(String accountNumber, BigDecimal depositCeiling, BigDecimal initialBalance) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }
        if (depositCeiling == null || depositCeiling.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit ceiling must be positive");
        }
        if (initialBalance != null && initialBalance.compareTo(depositCeiling) > 0) {
            throw new IllegalArgumentException("Initial balance cannot exceed deposit ceiling");
        }

        this.accountNumber = accountNumber;
        this.depositCeiling = depositCeiling;
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getDepositCeiling() {
        return depositCeiling;
    }

    public void deposit(BigDecimal amount) {
        lock.lock();
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Deposit amount must be positive");
            }

            if (balance.add(amount).compareTo(depositCeiling) > 0) {
                throw new IllegalStateException("Deposit would exceed the ceiling of " + depositCeiling);
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

            if (amount.compareTo(balance) > 0) {
                throw new IllegalStateException("Insufficient funds for withdrawal");
            }

            balance = balance.subtract(amount);
        } finally {
            lock.unlock();
        }
    }
}