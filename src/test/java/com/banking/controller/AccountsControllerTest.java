package com.banking.controller;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.BankAccount;
import com.banking.model.Transaction;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountsControllerTest {

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountMapper accountMapper;

    private AccountsController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountsController(accountRepository, transactionRepository, accountMapper);
    }

    @Test
    void getAll_ReturnsEmptyList_WhenNoAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<AccountDTO>> result = controller.getAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    void create_ValidAccount_ReturnsCreated() {
        CreateAccountDTO model = new CreateAccountDTO("ACC001", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountRepository.exists("ACC001")).thenReturn(false);
        doNothing().when(accountRepository).save(any(BankAccount.class));
        when(accountMapper.toAccountDTO(any(BankAccount.class))).thenReturn(new AccountDTO("ACC001", new BigDecimal("1000"), new BigDecimal("500")));

        ResponseEntity<AccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("ACC001", result.getBody().accountNumber());
    }

    @Test
    void create_DuplicateAccount_ReturnsConflict() {
        CreateAccountDTO model = new CreateAccountDTO("ACC001", new BigDecimal("1000"), BigDecimal.ZERO);
        when(accountRepository.exists("ACC001")).thenReturn(true);

        ResponseEntity<AccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void get_ExistingAccount_ReturnsAccount() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("500"), BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));
        when(accountMapper.toAccountDTO(account)).thenReturn(new AccountDTO("ACC001", new BigDecimal("500"), BigDecimal.ZERO));

        ResponseEntity<AccountDTO> result = controller.get("ACC001");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("ACC001", result.getBody().accountNumber());
    }

    @Test
    void get_NonExistingAccount_ThrowsException() {
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> controller.get("NONEXISTENT"));
    }

    @Test
    void deposit_ValidAmount_IncreasesBalance() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("100"), BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));
        doNothing().when(accountRepository).update(any());
        doNothing().when(transactionRepository).save(any());
        when(accountMapper.toAccountDTO(any())).thenAnswer(inv -> {
            BankAccount acc = inv.getArgument(0);
            return new AccountDTO(acc.getAccountNumber(), acc.getBalance(), acc.getOverdraftLimit());
        });

        ResponseEntity<AccountDTO> result = controller.deposit("ACC001", new TransactionDTO(new BigDecimal("50")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("150"), result.getBody().balance());
    }

    @Test
    void deposit_NonExistingAccount_ThrowsException() {
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> controller.deposit("NONEXISTENT", new TransactionDTO(new BigDecimal("50"))));
    }

    @Test
    void withdraw_ValidAmount_DecreasesBalance() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("100"), BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));
        doNothing().when(accountRepository).update(any());
        doNothing().when(transactionRepository).save(any());
        when(accountMapper.toAccountDTO(any())).thenAnswer(inv -> {
            BankAccount acc = inv.getArgument(0);
            return new AccountDTO(acc.getAccountNumber(), acc.getBalance(), acc.getOverdraftLimit());
        });

        ResponseEntity<AccountDTO> result = controller.withdraw("ACC001", new TransactionDTO(new BigDecimal("30")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("70"), result.getBody().balance());
    }

    @Test
    void withdraw_InsufficientFunds_ThrowsException() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("100"), BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class, 
            () -> controller.withdraw("ACC001", new TransactionDTO(new BigDecimal("150"))));
    }

    @Test
    void setOverdraft_ValidLimit_UpdatesOverdraft() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("100"), BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));
        doNothing().when(accountRepository).update(any());
        when(accountMapper.toAccountDTO(any())).thenAnswer(inv -> {
            BankAccount acc = inv.getArgument(0);
            return new AccountDTO(acc.getAccountNumber(), acc.getBalance(), acc.getOverdraftLimit());
        });

        ResponseEntity<AccountDTO> result = controller.setOverdraft("ACC001", new OverdraftDTO(new BigDecimal("200")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("200"), result.getBody().overdraftLimit());
    }

    @Test
    void getStatement_ExistingAccount_ReturnsStatement() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("150"), BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountNumberInRange(any(), any(), any())).thenReturn(List.of());

        ResponseEntity<StatementDTO> result = controller.getStatement("ACC001", null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("ACC001", result.getBody().accountNumber());
        assertEquals(new BigDecimal("150"), result.getBody().balance());
    }
}