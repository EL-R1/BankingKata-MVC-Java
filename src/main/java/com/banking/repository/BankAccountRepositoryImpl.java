package com.banking.repository;

import com.banking.model.BankAccount;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BankAccountRepositoryImpl implements BankAccountRepository {
    private final Map<String, BankAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    @Override
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    @Override
    public List<BankAccount> findAll() {
        return accounts.values().stream().toList();
    }

    @Override
    public void save(BankAccount account) {
        accounts.put(account.getAccountNumber(), account);
    }

    @Override
    public void update(BankAccount account) {
        accounts.put(account.getAccountNumber(), account);
    }
}