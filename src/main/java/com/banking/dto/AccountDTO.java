package com.banking.dto;

import java.math.BigDecimal;

public record AccountDTO(
    String accountNumber,
    BigDecimal balance,
    BigDecimal overdraftLimit
) {}