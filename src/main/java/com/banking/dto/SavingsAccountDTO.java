package com.banking.dto;

import java.math.BigDecimal;

public record SavingsAccountDTO(
    String accountNumber,
    BigDecimal balance,
    BigDecimal depositCeiling
) {}