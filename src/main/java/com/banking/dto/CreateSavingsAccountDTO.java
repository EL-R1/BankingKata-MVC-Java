package com.banking.dto;

import java.math.BigDecimal;

public record CreateSavingsAccountDTO(
    String accountNumber,
    BigDecimal depositCeiling,
    BigDecimal initialBalance
) {}