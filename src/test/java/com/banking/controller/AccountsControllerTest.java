package com.banking.controller;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.BankAccount;
import com.banking.service.AccountService;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountsControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountMapper accountMapper;

    private AccountsController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountsController(accountService, transactionRepository, accountMapper);
    }

    @SuppressWarnings("null")
    @Test
    void getAll_ReturnsEmptyList_WhenNoAccounts() {
        when(accountService.findAll()).thenReturn(List.of());

        ResponseEntity<List<AccountDTO>> result = controller.getAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @SuppressWarnings("null")
    @Test
    void create_ValidAccount_ReturnsCreated() {
        CreateAccountDTO model = new CreateAccountDTO("ACC001", new BigDecimal("1000"), new BigDecimal("500"));
        BankAccount account = new BankAccount("ACC001", new BigDecimal("1000"), new BigDecimal("500"));
        when(accountService.createAccount(anyString(), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(account);
        when(accountMapper.toAccountDTO(any(BankAccount.class))).thenReturn(new AccountDTO("ACC001", new BigDecimal("1000"), new BigDecimal("500")));

        ResponseEntity<AccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("ACC001", result.getBody().accountNumber());
    }

    @Test
    void create_DuplicateAccount_ReturnsConflict() {
        CreateAccountDTO model = new CreateAccountDTO("ACC001", new BigDecimal("1000"), BigDecimal.ZERO);
        when(accountService.createAccount(anyString(), any(BigDecimal.class), any(BigDecimal.class)))
                .thenThrow(new IllegalStateException("Account number already exists: ACC001"));

        ResponseEntity<AccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @SuppressWarnings("null")
    @Test
    void get_ExistingAccount_ReturnsAccount() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("500"), BigDecimal.ZERO);
        when(accountService.findAccount("ACC001")).thenReturn(account);
        when(accountMapper.toAccountDTO(account)).thenReturn(new AccountDTO("ACC001", new BigDecimal("500"), BigDecimal.ZERO));

        ResponseEntity<AccountDTO> result = controller.get("ACC001");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("ACC001", result.getBody().accountNumber());
    }

    @Test
    void get_NonExistingAccount_ThrowsException() {
        when(accountService.findAccount("NONEXISTENT")).thenThrow(new AccountNotFoundException("Account not found: NONEXISTENT"));

        assertThrows(AccountNotFoundException.class, () -> controller.get("NONEXISTENT"));
    }

    @SuppressWarnings("null")
    @Test
    void deposit_ValidAmount_IncreasesBalance() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("150"), BigDecimal.ZERO);
        when(accountService.deposit("ACC001", new BigDecimal("50"))).thenReturn(account);
        when(accountMapper.toAccountDTO(account)).thenReturn(new AccountDTO("ACC001", new BigDecimal("150"), BigDecimal.ZERO));

        ResponseEntity<AccountDTO> result = controller.deposit("ACC001", new TransactionDTO(new BigDecimal("50")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("150"), result.getBody().balance());
    }

    @Test
    void deposit_NonExistingAccount_ThrowsException() {
        when(accountService.deposit("NONEXISTENT", new BigDecimal("50"))).thenThrow(new AccountNotFoundException("Account not found: NONEXISTENT"));

        assertThrows(AccountNotFoundException.class, 
                () -> controller.deposit("NONEXISTENT", new TransactionDTO(new BigDecimal("50"))));
    }

    @SuppressWarnings("null")
    @Test
    void withdraw_ValidAmount_DecreasesBalance() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("70"), BigDecimal.ZERO);
        when(accountService.withdraw("ACC001", new BigDecimal("30"))).thenReturn(account);
        when(accountMapper.toAccountDTO(account)).thenReturn(new AccountDTO("ACC001", new BigDecimal("70"), BigDecimal.ZERO));

        ResponseEntity<AccountDTO> result = controller.withdraw("ACC001", new TransactionDTO(new BigDecimal("30")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("70"), result.getBody().balance());
    }

    @Test
    void withdraw_InsufficientFunds_ThrowsException() {
        when(accountService.withdraw("ACC001", new BigDecimal("150"))).thenThrow(new InsufficientFundsException("Insufficient funds for withdrawal"));

        assertThrows(InsufficientFundsException.class, 
                () -> controller.withdraw("ACC001", new TransactionDTO(new BigDecimal("150"))));
    }

    @SuppressWarnings("null")
    @Test
    void setOverdraft_ValidLimit_UpdatesOverdraft() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("100"), new BigDecimal("200"));
        when(accountService.setOverdraftLimit("ACC001", new BigDecimal("200"))).thenReturn(account);
        when(accountMapper.toAccountDTO(any())).thenAnswer(inv -> {
            BankAccount acc = inv.getArgument(0);
            return new AccountDTO(acc.getAccountNumber(), acc.getBalance(), acc.getOverdraftLimit());
        });

        ResponseEntity<AccountDTO> result = controller.setOverdraft("ACC001", new OverdraftDTO(new BigDecimal("200")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("200"), result.getBody().overdraftLimit());
    }

    @SuppressWarnings("null")
    @Test
    void getStatement_ExistingAccount_ReturnsStatement() {
        BankAccount account = new BankAccount("ACC001", new BigDecimal("150"), BigDecimal.ZERO);
        when(accountService.findAccount("ACC001")).thenReturn(account);
        when(transactionRepository.findByAccountNumberInRange(any(), any(), any())).thenReturn(List.of());

        ResponseEntity<StatementDTO> result = controller.getStatement("ACC001", null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("ACC001", result.getBody().accountNumber());
        assertEquals(new BigDecimal("150"), result.getBody().balance());
    }
}