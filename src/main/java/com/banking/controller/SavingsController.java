package com.banking.controller;

import com.banking.dto.*;
import com.banking.mapper.AccountMapper;
import com.banking.model.SavingsAccount;
import com.banking.repository.SavingsAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings")
public class SavingsController {
    private static final Logger logger = LoggerFactory.getLogger(SavingsController.class);

    private final SavingsAccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public SavingsController(SavingsAccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @GetMapping
    public ResponseEntity<List<SavingsAccountDTO>> getAll() {
        return ResponseEntity.ok(accountRepository.findAll().stream()
                .map(accountMapper::toSavingsAccountDTO)
                .toList());
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<SavingsAccountDTO> get(@PathVariable String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> ResponseEntity.ok(accountMapper.toSavingsAccountDTO(account)))
                .orElseGet(() -> {
                    logger.warn("Savings account not found: {}", accountNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<SavingsAccountDTO> create(@RequestBody CreateSavingsAccountDTO model) {
        try {
            if (accountRepository.exists(model.accountNumber())) {
                logger.warn("Duplicate savings account creation attempted: {}", model.accountNumber());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            var account = new SavingsAccount(
                    model.accountNumber(),
                    model.depositCeiling(),
                    model.initialBalance()
            );
            accountRepository.save(account);

            logger.info("Savings account created: {} with initial balance: {}",
                    account.getAccountNumber(), account.getBalance());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(accountMapper.toSavingsAccountDTO(account));
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid savings account creation request", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<SavingsAccountDTO> deposit(@PathVariable String accountNumber, @RequestBody TransactionDTO model) {
        try {
            var account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalStateException("Savings account " + accountNumber + " not found"));

            account.deposit(model.amount());
            accountRepository.update(account);

            logger.info("Savings deposit successful: {} - {} -> New balance: {}",
                    accountNumber, model.amount(), account.getBalance());

            return ResponseEntity.ok(accountMapper.toSavingsAccountDTO(account));
        } catch (IllegalStateException ex) {
            if (ex.getMessage().contains("exceed")) {
                logger.warn("Savings deposit ceiling exceeded: {} - attempted: {}", accountNumber, model.amount());
                return ResponseEntity.badRequest().build();
            }
            if (ex.getMessage().contains("not found")) {
                logger.warn("Savings deposit failed - account not found: {}", accountNumber);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException ex) {
            logger.warn("Savings deposit failed - invalid amount: {}", accountNumber, ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<SavingsAccountDTO> withdraw(@PathVariable String accountNumber, @RequestBody TransactionDTO model) {
        try {
            var account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalStateException("Savings account " + accountNumber + " not found"));

            account.withdraw(model.amount());
            accountRepository.update(account);

            logger.info("Savings withdrawal successful: {} - {} -> New balance: {}",
                    accountNumber, model.amount(), account.getBalance());

            return ResponseEntity.ok(accountMapper.toSavingsAccountDTO(account));
        } catch (IllegalStateException ex) {
            if (ex.getMessage().contains("Insufficient funds")) {
                logger.warn("Insufficient funds for savings withdrawal: {} - attempted: {}", accountNumber, model.amount());
                return ResponseEntity.badRequest().build();
            }
            if (ex.getMessage().contains("not found")) {
                logger.warn("Savings withdrawal failed - account not found: {}", accountNumber);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException ex) {
            logger.warn("Savings withdrawal failed - invalid amount: {}", accountNumber, ex);
            return ResponseEntity.badRequest().build();
        }
    }
}