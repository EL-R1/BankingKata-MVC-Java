package com.banking.repository;

import com.banking.model.Transaction;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {
    private final List<Transaction> transactions = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void save(Transaction transaction) {
        transactions.add(transaction);
    }

    @Override
    public List<Transaction> findByAccountNumberInRange(String accountNumber, Instant fromDate, Instant toDate) {
        return transactions.stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber)
                        && !t.getDate().isBefore(fromDate)
                        && !t.getDate().isAfter(toDate))
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                .toList();
    }
}