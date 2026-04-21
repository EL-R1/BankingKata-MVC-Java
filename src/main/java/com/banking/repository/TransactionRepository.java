package com.banking.repository;

import com.banking.model.Transaction;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository {
    void save(Transaction transaction);
    List<Transaction> findByAccountNumberInRange(String accountNumber, Instant fromDate, Instant toDate);
}