package com.banking.service;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.BankAccount;
import com.banking.model.Transaction;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;

    public AccountService(BankAccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public BankAccount findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    @Transactional
    public BankAccount createAccount(String accountNumber, BigDecimal initialBalance, BigDecimal overdraftLimit) {
        if (accountRepository.exists(accountNumber)) {
            throw new IllegalStateException("Account number already exists: " + accountNumber);
        }

        var account = new BankAccount(accountNumber, initialBalance, overdraftLimit);
        accountRepository.save(account);

        logger.info("Account created: {} with initial balance: {}", account.getAccountNumber(), account.getBalance());
        return account;
    }

    @Transactional
    public BankAccount deposit(String accountNumber, BigDecimal amount) {
        var account = findAccount(accountNumber);
        account.deposit(amount);
        accountRepository.update(account);

        var transaction = new Transaction(
                accountNumber,
                amount,
                Transaction.TransactionType.DEPOSIT,
                account.getBalance()
        );
        transactionRepository.save(transaction);

        logger.info("Deposit successful - Account: {}, New balance: {}", accountNumber, account.getBalance());
        return account;
    }

    @Transactional
    public BankAccount withdraw(String accountNumber, BigDecimal amount) {
        var account = findAccount(accountNumber);

        BigDecimal available = account.getBalance().add(account.getOverdraftLimit());
        if (amount.compareTo(available) > 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }

        account.withdraw(amount);
        accountRepository.update(account);

        var transaction = new Transaction(
                accountNumber,
                amount,
                Transaction.TransactionType.WITHDRAWAL,
                account.getBalance()
        );
        transactionRepository.save(transaction);

        logger.info("Withdrawal successful - Account: {}, New balance: {}", accountNumber, account.getBalance());
        return account;
    }

    @Transactional
    public BankAccount setOverdraftLimit(String accountNumber, BigDecimal limit) {
        var account = findAccount(accountNumber);
        account.setOverdraftLimit(limit);
        accountRepository.update(account);

        logger.info("Overdraft limit updated for account: {}", accountNumber);
        return account;
    }

    @Transactional(readOnly = true)
    public List<BankAccount> findAll() {
        return accountRepository.findAll();
    }
}