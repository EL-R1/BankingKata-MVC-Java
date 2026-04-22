package com.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateSavingsAccountDTO(
    @NotBlank(message = "Account number is required")
    String accountNumber,

    @Positive(message = "Deposit ceiling must be positive")
    BigDecimal depositCeiling,

    BigDecimal initialBalance
) {}