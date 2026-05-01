package com.transit.transactionservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Request body for recording a new transaction")
public class TransactionRequest {

    @Schema(description = "ID of the card associated with this transaction", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cardId;

    @Schema(description = "Transaction amount", example = "25.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Transaction type — TOPUP adds balance, PAYMENT subtracts balance", example = "TOPUP", allowableValues = {"TOPUP", "PAYMENT"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;
}
