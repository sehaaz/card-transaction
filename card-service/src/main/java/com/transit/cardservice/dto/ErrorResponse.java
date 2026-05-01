package com.transit.cardservice.dto;

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

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Error type identifier", example = "CARD_NOT_FOUND")
    private String error;

    @Schema(description = "Human-readable error message", example = "Card not found with id: 99")
    private String message;
}
