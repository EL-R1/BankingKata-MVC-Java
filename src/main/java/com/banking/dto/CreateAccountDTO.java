package com.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateAccountDTO(
    @NotBlank(message = "Account number is required")
    String accountNumber,

    @Positive(message = "Initial balance must be positive")
    BigDecimal initialBalance,

    @Positive(message = "Overdraft limit must be positive")
    BigDecimal overdraftLimit
) {}