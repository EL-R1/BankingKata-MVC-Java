package com.banking.controller;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.BankAccount;
import com.banking.model.Transaction;
import com.banking.repository.TransactionRepository;
import com.banking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Current Accounts", description = "API for managing current bank accounts")
public class AccountsController {

    private static final Logger logger = LoggerFactory.getLogger(AccountsController.class);

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;

    public AccountsController(AccountService accountService,
                              TransactionRepository transactionRepository,
                              AccountMapper accountMapper) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.accountMapper = accountMapper;
    }

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieves a list of all current accounts")
    @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")
    public ResponseEntity<List<AccountDTO>> getAll() {
        logger.info("Fetching all accounts");
        return ResponseEntity.ok(accountService.findAll().stream()
                .map(accountMapper::toAccountDTO)
                .toList());
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account by number", description = "Retrieves a specific account by its account number")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountDTO> get(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber) {
        logger.info("Fetching account: {}", accountNumber);
        var account = accountService.findAccount(accountNumber);
        return ResponseEntity.ok(accountMapper.toAccountDTO(account));
    }

    @PostMapping
    @Operation(summary = "Create a new account", description = "Creates a new current bank account")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Account number already exists")
    public ResponseEntity<AccountDTO> create(@Valid @RequestBody CreateAccountDTO model) {
        try {
            var account = accountService.createAccount(
                    model.accountNumber(),
                    model.initialBalance(),
                    model.overdraftLimit()
            );

            logger.info("Account created: {} with initial balance: {}",
                    account.getAccountNumber(), account.getBalance());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(accountMapper.toAccountDTO(account));
        } catch (IllegalStateException ex) {
            logger.warn("Duplicate account creation attempted: {}", model.accountNumber());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{accountNumber}/deposit")
    @Operation(summary = "Deposit money", description = "Deposits money into a current account")
    @ApiResponse(responseCode = "200", description = "Deposit successful")
    @ApiResponse(responseCode = "400", description = "Invalid amount")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountDTO> deposit(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionDTO model) {
        logger.info("Processing deposit for account: {}, amount: {}", accountNumber, model.amount());

        var account = accountService.deposit(accountNumber, model.amount());

        return ResponseEntity.ok(accountMapper.toAccountDTO(account));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @Operation(summary = "Withdraw money", description = "Withdraws money from a current account")
    @ApiResponse(responseCode = "200", description = "Withdrawal successful")
    @ApiResponse(responseCode = "400", description = "Invalid amount or insufficient funds")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountDTO> withdraw(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionDTO model) {
        logger.info("Processing withdrawal for account: {}, amount: {}", accountNumber, model.amount());

        var account = accountService.withdraw(accountNumber, model.amount());

        return ResponseEntity.ok(accountMapper.toAccountDTO(account));
    }

    @PostMapping("/{accountNumber}/overdraft")
    @Operation(summary = "Set overdraft limit", description = "Sets or updates the overdraft limit for an account")
    @ApiResponse(responseCode = "200", description = "Overdraft limit updated")
    @ApiResponse(responseCode = "400", description = "Invalid overdraft limit")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountDTO> setOverdraft(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber,
            @Valid @RequestBody OverdraftDTO model) {
        logger.info("Setting overdraft limit for account: {}, limit: {}", accountNumber, model.overdraftLimit());

        var account = accountService.setOverdraftLimit(accountNumber, model.overdraftLimit());

        return ResponseEntity.ok(accountMapper.toAccountDTO(account));
    }

    @GetMapping("/{accountNumber}/statement")
    @Operation(summary = "Get account statement", description = "Retrieves the transaction statement for an account")
    @ApiResponse(responseCode = "200", description = "Statement retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<StatementDTO> getStatement(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber,
            @Parameter(description = "Start date for the statement")
            @RequestParam(required = false) Instant fromDate,
            @Parameter(description = "End date for the statement")
            @RequestParam(required = false) Instant toDate) {
        logger.info("Fetching statement for account: {}", accountNumber);

        var account = accountService.findAccount(accountNumber);

        Instant to = toDate != null ? toDate : Instant.now();
        Instant from = fromDate != null ? fromDate : to.minus(30, ChronoUnit.DAYS);

        var transactions = transactionRepository.findByAccountNumberInRange(accountNumber, from, to);

        return ResponseEntity.ok(new StatementDTO(
                account.getAccountNumber(),
                "Current Account",
                account.getBalance(),
                to,
                accountMapper.toOperationDTOList(transactions)
        ));
    }
}