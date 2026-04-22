package com.banking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OverdraftDTO(
    @NotNull(message = "Overdraft limit is required")
    @Positive(message = "Overdraft limit must be positive")
    BigDecimal overdraftLimit
) {}