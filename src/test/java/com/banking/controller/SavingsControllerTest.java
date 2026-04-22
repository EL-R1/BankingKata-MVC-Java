package com.banking.controller;

import com.banking.dto.*;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.DepositLimitExceededException;
import com.banking.exception.InsufficientFundsException;
import com.banking.mapper.AccountMapper;
import com.banking.model.SavingsAccount;
import com.banking.repository.SavingsAccountRepository;
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
    private SavingsAccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    private SavingsController controller;

    @BeforeEach
    void setUp() {
        controller = new SavingsController(accountRepository, accountMapper);
    }

    @Test
    void getAll_ReturnsEmptyList_WhenNoAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<SavingsAccountDTO>> result = controller.getAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    void create_ValidSavingsAccount_ReturnsCreated() {
        CreateSavingsAccountDTO model = new CreateSavingsAccountDTO("SAV001", new BigDecimal("10000"), new BigDecimal("500"));
        when(accountRepository.exists("SAV001")).thenReturn(false);
        doNothing().when(accountRepository).save(any(SavingsAccount.class));
        when(accountMapper.toSavingsAccountDTO(any(SavingsAccount.class))).thenReturn(new SavingsAccountDTO("SAV001", new BigDecimal("500"), new BigDecimal("10000")));

        ResponseEntity<SavingsAccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("SAV001", result.getBody().accountNumber());
    }

    @Test
    void create_DuplicateAccount_ReturnsConflict() {
        CreateSavingsAccountDTO model = new CreateSavingsAccountDTO("SAV001", new BigDecimal("10000"), BigDecimal.ZERO);
        when(accountRepository.exists("SAV001")).thenReturn(true);

        ResponseEntity<SavingsAccountDTO> result = controller.create(model);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void get_ExistingAccount_ReturnsAccount() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("10000"), new BigDecimal("500"));
        when(accountRepository.findByAccountNumber("SAV001")).thenReturn(Optional.of(account));
        when(accountMapper.toSavingsAccountDTO(account)).thenReturn(new SavingsAccountDTO("SAV001", new BigDecimal("500"), new BigDecimal("10000")));

        ResponseEntity<SavingsAccountDTO> result = controller.get("SAV001");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("SAV001", result.getBody().accountNumber());
    }

    @Test
    void get_NonExistingAccount_ThrowsException() {
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> controller.get("NONEXISTENT"));
    }

    @Test
    void deposit_ValidAmount_IncreasesBalance() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("10000"), new BigDecimal("100"));
        when(accountRepository.findByAccountNumber("SAV001")).thenReturn(Optional.of(account));
        doNothing().when(accountRepository).update(any());
        when(accountMapper.toSavingsAccountDTO(any())).thenAnswer(inv -> {
            SavingsAccount acc = inv.getArgument(0);
            return new SavingsAccountDTO(acc.getAccountNumber(), acc.getBalance(), acc.getDepositCeiling());
        });

        ResponseEntity<SavingsAccountDTO> result = controller.deposit("SAV001", new TransactionDTO(new BigDecimal("50")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("150"), result.getBody().balance());
    }

    @Test
    void deposit_ExceedsCeiling_ThrowsException() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("100"), new BigDecimal("50"));
        when(accountRepository.findByAccountNumber("SAV001")).thenReturn(Optional.of(account));

        assertThrows(DepositLimitExceededException.class, 
            () -> controller.deposit("SAV001", new TransactionDTO(new BigDecimal("60"))));
    }

    @Test
    void withdraw_ValidAmount_DecreasesBalance() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("10000"), new BigDecimal("100"));
        when(accountRepository.findByAccountNumber("SAV001")).thenReturn(Optional.of(account));
        doNothing().when(accountRepository).update(any());
        when(accountMapper.toSavingsAccountDTO(any())).thenAnswer(inv -> {
            SavingsAccount acc = inv.getArgument(0);
            return new SavingsAccountDTO(acc.getAccountNumber(), acc.getBalance(), acc.getDepositCeiling());
        });

        ResponseEntity<SavingsAccountDTO> result = controller.withdraw("SAV001", new TransactionDTO(new BigDecimal("30")));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(new BigDecimal("70"), result.getBody().balance());
    }

    @Test
    void withdraw_InsufficientFunds_ThrowsException() {
        SavingsAccount account = new SavingsAccount("SAV001", new BigDecimal("10000"), new BigDecimal("50"));
        when(accountRepository.findByAccountNumber("SAV001")).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class, 
            () -> controller.withdraw("SAV001", new TransactionDTO(new BigDecimal("100"))));
    }

    @Test
    void deposit_NonExistingAccount_ThrowsException() {
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> controller.deposit("NONEXISTENT", new TransactionDTO(new BigDecimal("50"))));
    }
}