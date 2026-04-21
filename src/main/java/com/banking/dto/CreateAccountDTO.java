package com.banking.dto;

import java.math.BigDecimal;

public record CreateAccountDTO(
    String accountNumber,
    BigDecimal initialBalance,
    BigDecimal overdraftLimit
) {}