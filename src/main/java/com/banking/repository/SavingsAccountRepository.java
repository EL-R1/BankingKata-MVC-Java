package com.banking.repository;

import com.banking.model.SavingsAccount;

import java.util.List;
import java.util.Optional;

public interface SavingsAccountRepository {
    boolean exists(String accountNumber);
    Optional<SavingsAccount> findByAccountNumber(String accountNumber);
    List<SavingsAccount> findAll();
    void save(SavingsAccount account);
    void update(SavingsAccount account);
}