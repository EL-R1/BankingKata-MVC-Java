package com.banking.controller;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.DepositLimitExceededException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.SavingsAccount;
import com.banking.service.SavingsService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/savings")
@Tag(name = "Savings Accounts", description = "API for managing savings accounts")
public class SavingsController {

    private static final Logger logger = LoggerFactory.getLogger(SavingsController.class);

    private final SavingsService savingsService;
    private final AccountMapper accountMapper;

    public SavingsController(SavingsService savingsService, AccountMapper accountMapper) {
        this.savingsService = savingsService;
        this.accountMapper = accountMapper;
    }

    @GetMapping
    @Operation(summary = "Get all savings accounts", description = "Retrieves a list of all savings accounts")
    @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")
    public ResponseEntity<List<SavingsAccountDTO>> getAll() {
        logger.info("Fetching all savings accounts");
        return ResponseEntity.ok(savingsService.findAll().stream()
                .map(accountMapper::toSavingsAccountDTO)
                .toList());
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get savings account by number", description = "Retrieves a specific savings account by its account number")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<SavingsAccountDTO> get(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber) {
        logger.info("Fetching savings account: {}", accountNumber);
        var account = savingsService.findAccount(accountNumber);
        return ResponseEntity.ok(accountMapper.toSavingsAccountDTO(account));
    }

    @PostMapping
    @Operation(summary = "Create a new savings account", description = "Creates a new savings account")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Account number already exists")
    public ResponseEntity<SavingsAccountDTO> create(@Valid @RequestBody CreateSavingsAccountDTO model) {
        try {
            var account = savingsService.createSavingsAccount(
                    model.accountNumber(),
                    model.depositCeiling(),
                    model.initialBalance()
            );

            logger.info("Savings account created: {} with initial balance: {}",
                    account.getAccountNumber(), account.getBalance());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(accountMapper.toSavingsAccountDTO(account));
        } catch (IllegalStateException ex) {
            logger.warn("Duplicate savings account creation attempted: {}", model.accountNumber());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{accountNumber}/deposit")
    @Operation(summary = "Deposit money", description = "Deposits money into a savings account")
    @ApiResponse(responseCode = "200", description = "Deposit successful")
    @ApiResponse(responseCode = "400", description = "Invalid amount or deposit ceiling exceeded")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<SavingsAccountDTO> deposit(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionDTO model) {
        logger.info("Processing savings deposit for account: {}, amount: {}", accountNumber, model.amount());

        var account = savingsService.deposit(accountNumber, model.amount());

        return ResponseEntity.ok(accountMapper.toSavingsAccountDTO(account));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @Operation(summary = "Withdraw money", description = "Withdraws money from a savings account")
    @ApiResponse(responseCode = "200", description = "Withdrawal successful")
    @ApiResponse(responseCode = "400", description = "Invalid amount or insufficient funds")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<SavingsAccountDTO> withdraw(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionDTO model) {
        logger.info("Processing savings withdrawal for account: {}, amount: {}", accountNumber, model.amount());

        var account = savingsService.withdraw(accountNumber, model.amount());

        return ResponseEntity.ok(accountMapper.toSavingsAccountDTO(account));
    }
}