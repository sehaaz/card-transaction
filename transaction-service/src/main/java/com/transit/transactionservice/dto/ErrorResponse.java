package com.transit.transactionservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Standard error response returned for all API errors")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2026-05-01T14:30:00")
    private String timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error type identifier", example = "INVALID_TRANSACTION_TYPE")
    private String error;

    @Schema(description = "Human-readable error message", example = "Invalid transaction type: 'UNKNOWN'. Allowed values are TOPUP and PAYMENT")
    private String message;
}
