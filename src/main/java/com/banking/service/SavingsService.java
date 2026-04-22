package com.banking.service;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.DepositLimitExceededException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.SavingsAccount;
import com.banking.repository.SavingsAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SavingsService {

    private static final Logger logger = LoggerFactory.getLogger(SavingsService.class);

    private final SavingsAccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public SavingsService(SavingsAccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public SavingsAccount findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Savings account not found: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public List<SavingsAccount> findAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public SavingsAccount createSavingsAccount(String accountNumber, BigDecimal depositCeiling, BigDecimal initialBalance) {
        if (accountRepository.exists(accountNumber)) {
            throw new IllegalStateException("Account number already exists: " + accountNumber);
        }

        var account = new SavingsAccount(accountNumber, depositCeiling, initialBalance);
        accountRepository.save(account);

        logger.info("Savings account created: {} with initial balance: {}", account.getAccountNumber(), account.getBalance());
        return account;
    }

    @Transactional
    public SavingsAccount deposit(String accountNumber, BigDecimal amount) {
        var account = findAccount(accountNumber);

        BigDecimal newBalance = account.getBalance().add(amount);
        if (newBalance.compareTo(account.getDepositCeiling()) > 0) {
            throw new DepositLimitExceededException(
                    "Deposit would exceed the ceiling of " + account.getDepositCeiling());
        }

        account.deposit(amount);
        accountRepository.update(account);

        logger.info("Savings deposit successful - Account: {}, New balance: {}", accountNumber, account.getBalance());
        return account;
    }

    @Transactional
    public SavingsAccount withdraw(String accountNumber, BigDecimal amount) {
        var account = findAccount(accountNumber);

        if (amount.compareTo(account.getBalance()) > 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }

        account.withdraw(amount);
        accountRepository.update(account);

        logger.info("Savings withdrawal successful - Account: {}, New balance: {}", accountNumber, account.getBalance());
        return account;
    }
}