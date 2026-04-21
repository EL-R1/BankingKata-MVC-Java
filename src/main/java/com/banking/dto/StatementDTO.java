package com.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StatementDTO(
    String accountNumber,
    String accountType,
    BigDecimal balance,
    Instant statementDate,
    List<OperationDTO> transactions
) {}