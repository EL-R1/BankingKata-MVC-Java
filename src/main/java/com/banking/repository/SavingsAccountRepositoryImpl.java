package com.banking.repository;

import com.banking.model.SavingsAccount;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SavingsAccountRepositoryImpl implements SavingsAccountRepository {
    private final Map<String, SavingsAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    @Override
    public Optional<SavingsAccount> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    @Override
    public List<SavingsAccount> findAll() {
        return accounts.values().stream().toList();
    }

    @Override
    public void save(SavingsAccount account) {
        accounts.put(account.getAccountNumber(), account);
    }

    @Override
    public void update(SavingsAccount account) {
        accounts.put(account.getAccountNumber(), account);
    }
}