package com.banking.controller;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.DepositLimitExceededException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.SavingsAccount;
import com.banking.service.SavingsService;
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
class SavingsControllerTest {

    @Mock
    private SavingsService savingsService;

    @Mock
    private AccountMapper accountMapper;

    private SavingsController controller;

    @BeforeEach
    void setUp() {
        controller = new SavingsController(savingsService, accountMapper);
    }

    @Test
    void getAll_ReturnsEmptyList_WhenNoAccounts() {
        when(savingsService.findAll()).thenReturn(List.of());

        ResponseEntity<List<SavingsAccountDTO>> result = controller.getAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    void create_ValidSavingsAccount_ReturnsCreated() {
        CreateSavingsAccountDTO model = new CreateSavingsAccountDTO("SAV001", new BigDecimal("10000"), new BigDecimal("500"));
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("10000"), new BigDecimal("500"));
        when(savingsService.createSavingsAccount(anyString(), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(account);
        when(accountMapper.toSavingsAccountDTO(any(SavingsAccount.class))).thenReturn(new SavingsAccountDTO("SAV001", new BigDecimal("500"), new BigDecimal("10000")));

        ResponseEntity<SavingsAccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("SAV001", result.getBody().accountNumber());
    }

    @Test
    void create_DuplicateAccount_ReturnsConflict() {
        CreateSavingsAccountDTO model = new CreateSavingsAccountDTO("SAV001", new BigDecimal("10000"), BigDecimal.ZERO);
        when(savingsService.createSavingsAccount(anyString(), any(BigDecimal.class), any(BigDecimal.class)))
                .thenThrow(new IllegalStateException("Account number already exists: SAV001"));

        ResponseEntity<SavingsAccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void get_ExistingAccount_ReturnsAccount() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("10000"), new BigDecimal("500"));
        when(savingsService.findAccount("SAV001")).thenReturn(account);
        when(accountMapper.toSavingsAccountDTO(account)).thenReturn(new SavingsAccountDTO("SAV001", new BigDecimal("500"), new BigDecimal("10000")));

        ResponseEntity<SavingsAccountDTO> result = controller.get("SAV001");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("SAV001", result.getBody().accountNumber());
    }

    @Test
    void get_NonExistingAccount_ThrowsException() {
        when(savingsService.findAccount("NONEXISTENT")).thenThrow(new AccountNotFoundException("Savings account not found: NONEXISTENT"));

        assertThrows(AccountNotFoundException.class, () -> controller.get("NONEXISTENT"));
    }

    @Test
    void deposit_ValidAmount_IncreasesBalance() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("150"), BigDecimal.ZERO);
        when(savingsService.deposit("SAV001", new BigDecimal("50"))).thenReturn(account);
        when(accountMapper.toSavingsAccountDTO(account)).thenReturn(new SavingsAccountDTO("SAV001", new BigDecimal("150"), BigDecimal.ZERO));

        ResponseEntity<SavingsAccountDTO> result = controller.deposit("SAV001", new TransactionDTO(new BigDecimal("50")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("150"), result.getBody().balance());
    }

    @Test
    void deposit_ExceedsCeiling_ThrowsException() {
        when(savingsService.deposit("SAV001", new BigDecimal("60"))).thenThrow(new DepositLimitExceededException("Deposit would exceed the ceiling of 100"));

        assertThrows(DepositLimitExceededException.class, 
                () -> controller.deposit("SAV001", new TransactionDTO(new BigDecimal("60"))));
    }

    @Test
    void withdraw_ValidAmount_DecreasesBalance() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("70"), BigDecimal.ZERO);
        when(savingsService.withdraw("SAV001", new BigDecimal("30"))).thenReturn(account);
        when(accountMapper.toSavingsAccountDTO(account)).thenReturn(new SavingsAccountDTO("SAV001", new BigDecimal("70"), BigDecimal.ZERO));

        ResponseEntity<SavingsAccountDTO> result = controller.withdraw("SAV001", new TransactionDTO(new BigDecimal("30")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("70"), result.getBody().balance());
    }

    @Test
    void withdraw_InsufficientFunds_ThrowsException() {
        when(savingsService.withdraw("SAV001", new BigDecimal("100"))).thenThrow(new InsufficientFundsException("Insufficient funds for withdrawal"));

        assertThrows(InsufficientFundsException.class, 
                () -> controller.withdraw("SAV001", new TransactionDTO(new BigDecimal("100"))));
    }

    @Test
    void deposit_NonExistingAccount_ThrowsException() {
        when(savingsService.deposit("NONEXISTENT", new BigDecimal("50"))).thenThrow(new AccountNotFoundException("Savings account not found: NONEXISTENT"));

        assertThrows(AccountNotFoundException.class, 
                () -> controller.deposit("NONEXISTENT", new TransactionDTO(new BigDecimal("50"))));
    }
}