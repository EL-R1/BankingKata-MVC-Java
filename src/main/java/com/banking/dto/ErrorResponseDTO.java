package com.banking.dto;

import java.time.Instant;

public record ErrorResponseDTO(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp
) {
    public ErrorResponseDTO(int status, String error, String message, String path) {
        this(status, error, message, path, Instant.now());
    }
}