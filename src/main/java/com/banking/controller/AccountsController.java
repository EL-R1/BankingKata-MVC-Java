package com.banking.controller;

import com.banking.dto.*;
import com.banking.mapper.AccountMapper;
import com.banking.model.BankAccount;
import com.banking.model.Transaction;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountsController {
    private static final Logger logger = LoggerFactory.getLogger(AccountsController.class);

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;

    public AccountsController(
            BankAccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountMapper = accountMapper;
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAll() {
        return ResponseEntity.ok(accountRepository.findAll().stream()
                .map(accountMapper::toAccountDTO)
                .toList());
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDTO> get(@PathVariable String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> ResponseEntity.ok(accountMapper.toAccountDTO(account)))
                .orElseGet(() -> {
                    logger.warn("Account not found: {}", accountNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<AccountDTO> create(@RequestBody CreateAccountDTO model) {
        try {
            if (accountRepository.exists(model.accountNumber())) {
                logger.warn("Duplicate account creation attempted: {}", model.accountNumber());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            var account = new BankAccount(
                    model.accountNumber(),
                    model.initialBalance(),
                    model.overdraftLimit()
            );
            accountRepository.save(account);

            logger.info("Account created: {} with initial balance: {}",
                    account.getAccountNumber(), account.getBalance());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(accountMapper.toAccountDTO(account));
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid account creation request", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<AccountDTO> deposit(@PathVariable String accountNumber, @RequestBody TransactionDTO model) {
        try {
            var account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalStateException("Account " + accountNumber + " not found"));

            account.deposit(model.amount());
            accountRepository.update(account);

            var transaction = new Transaction(
                    accountNumber,
                    model.amount(),
                    Transaction.TransactionType.DEPOSIT,
                    account.getBalance()
            );
            transactionRepository.save(transaction);

            logger.info("Deposit successful: {} - {} -> New balance: {}",
                    accountNumber, model.amount(), account.getBalance());

            return ResponseEntity.ok(accountMapper.toAccountDTO(account));
        } catch (IllegalStateException ex) {
            if (ex.getMessage().contains("not found")) {
                logger.warn("Deposit failed - account not found: {}", accountNumber);
                return ResponseEntity.notFound().build();
            }
            logger.warn("Deposit failed - invalid amount: {}", accountNumber, ex);
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException ex) {
            logger.warn("Deposit failed - invalid amount: {}", accountNumber, ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<AccountDTO> withdraw(@PathVariable String accountNumber, @RequestBody TransactionDTO model) {
        try {
            var account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalStateException("Account " + accountNumber + " not found"));

            account.withdraw(model.amount());
            accountRepository.update(account);

            var transaction = new Transaction(
                    accountNumber,
                    model.amount(),
                    Transaction.TransactionType.WITHDRAWAL,
                    account.getBalance()
            );
            transactionRepository.save(transaction);

            logger.info("Withdrawal successful: {} - {} -> New balance: {}",
                    accountNumber, model.amount(), account.getBalance());

            return ResponseEntity.ok(accountMapper.toAccountDTO(account));
        } catch (IllegalStateException ex) {
            if (ex.getMessage().contains("Insufficient funds")) {
                logger.warn("Insufficient funds: {} - attempted: {}", accountNumber, model.amount());
                return ResponseEntity.badRequest().build();
            }
            if (ex.getMessage().contains("not found")) {
                logger.warn("Withdrawal failed - account not found: {}", accountNumber);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException ex) {
            logger.warn("Withdrawal failed - invalid amount: {}", accountNumber, ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountNumber}/overdraft")
    public ResponseEntity<AccountDTO> setOverdraft(@PathVariable String accountNumber, @RequestBody OverdraftDTO model) {
        try {
            var account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalStateException("Account " + accountNumber + " not found"));

            account.setOverdraftLimit(model.overdraftLimit());
            accountRepository.update(account);

            logger.info("Overdraft limit updated: {} -> {}", accountNumber, model.overdraftLimit());

            return ResponseEntity.ok(accountMapper.toAccountDTO(account));
        } catch (IllegalStateException ex) {
            logger.warn("Set overdraft failed - account not found: {}", accountNumber);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException ex) {
            logger.warn("Set overdraft failed - invalid limit: {}", accountNumber, ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{accountNumber}/statement")
    public ResponseEntity<StatementDTO> getStatement(
            @PathVariable String accountNumber,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {
        try {
            var account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalStateException("Account " + accountNumber + " not found"));

            Instant to = toDate != null ? toDate : Instant.now();
            Instant from = fromDate != null ? fromDate : to.minus(30, ChronoUnit.DAYS);

            var transactions = transactionRepository.findByAccountNumberInRange(accountNumber, from, to);

            return ResponseEntity.ok(new StatementDTO(
                    account.getAccountNumber(),
                    "Compte Courant",
                    account.getBalance(),
                    to,
                    accountMapper.toOperationDTOList(transactions)
            ));
        } catch (IllegalStateException ex) {
            logger.warn("Statement failed - account not found: {}", accountNumber);
            return ResponseEntity.notFound().build();
        }
    }
}