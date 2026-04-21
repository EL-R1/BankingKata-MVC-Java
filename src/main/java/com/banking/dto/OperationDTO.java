package com.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OperationDTO(
    UUID id,
    String accountNumber,
    BigDecimal amount,
    String type,
    Instant date,
    BigDecimal balanceAfterTransaction
) {}