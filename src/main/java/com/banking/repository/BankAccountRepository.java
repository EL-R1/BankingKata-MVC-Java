package com.banking.repository;

import com.banking.model.BankAccount;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository {
    boolean exists(String accountNumber);
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findAll();
    void save(BankAccount account);
    void update(BankAccount account);
}