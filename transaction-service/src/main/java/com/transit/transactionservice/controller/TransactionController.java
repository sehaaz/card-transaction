package com.transit.transactionservice.controller;

import com.transit.transactionservice.dto.ErrorResponse;
import com.transit.transactionservice.dto.TransactionRequest;
import com.transit.transactionservice.entity.Transaction;
import com.transit.transactionservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "Endpoints for recording and retrieving transit card transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(
            summary = "Record a new transaction",
            description = "Records a TOPUP or PAYMENT transaction for a given card. "
                    + "After saving the transaction, the service calls card-service to update the card balance: "
                    + "TOPUP adds the amount, PAYMENT subtracts it. "
                    + "For PAYMENT transactions, the card balance is checked before proceeding."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transaction recorded successfully",
                    content = @Content(schema = @Schema(implementation = Transaction.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Missing card ID",
                                            summary = "Card ID is null",
                                            value = """
                                                    {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_CARD_ID","message":"Card ID must not be null"}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid amount",
                                            summary = "Amount is null, zero, or negative",
                                            value = """
                                                    {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_AMOUNT","message":"Invalid amount: -5. Amount must be greater than zero"}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid transaction type",
                                            summary = "Type is not TOPUP or PAYMENT",
                                            value = """
                                                    {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_TRANSACTION_TYPE","message":"Invalid transaction type: 'REFUND'. Allowed values are TOPUP and PAYMENT"}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Malformed JSON",
                                            summary = "Request body is not valid JSON",
                                            value = """
                                                    {"timestamp":"2026-05-01T14:30:00","status":400,"error":"MALFORMED_REQUEST","message":"Request body is missing or contains invalid JSON"}
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Insufficient balance for payment",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Insufficient balance",
                                    summary = "Card does not have enough balance for the payment",
                                    value = """
                                            {"timestamp":"2026-05-01T14:30:00","status":422,"error":"INSUFFICIENT_BALANCE","message":"Insufficient balance on card 1 for payment of 500.00"}
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Card service communication failure",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Card not found",
                                            summary = "Card service returned 404 for the given card ID",
                                            value = """
                                                    {"timestamp":"2026-05-01T14:30:00","status":502,"error":"CARD_SERVICE_ERROR","message":"Card service error: Card not found with id: 99"}
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Service unavailable",
                                            summary = "Card service is down or unreachable",
                                            value = """
                                                    {"timestamp":"2026-05-01T14:30:00","status":502,"error":"CARD_SERVICE_ERROR","message":"Card service error: Card service is unavailable"}
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Server error",
                                    value = """
                                            {"timestamp":"2026-05-01T14:30:00","status":500,"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Transaction> recordTransaction(@RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.recordTransaction(
                request.getCardId(), request.getAmount(), request.getType());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @GetMapping
    @Operation(
            summary = "Get all transactions",
            description = "Retrieves all transactions across all cards."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully (empty list if none exist)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Transaction.class)))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Server error",
                                    value = """
                                            {"timestamp":"2026-05-01T14:30:00","status":500,"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{cardId}")
    @Operation(
            summary = "Get transactions by card ID",
            description = "Retrieves all transactions (both TOPUP and PAYMENT) associated with the given card ID. "
                    + "Returns an empty list if no transactions exist for the card."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully (empty list if none exist)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Transaction.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path parameter",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid card ID format",
                                    summary = "Non-numeric card ID in path",
                                    value = """
                                            {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_PARAMETER","message":"Invalid value for parameter 'cardId': abc"}
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Server error",
                                    value = """
                                            {"timestamp":"2026-05-01T14:30:00","status":500,"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<List<Transaction>> getTransactionsByCardId(
            @Parameter(description = "ID of the card to retrieve transactions for", example = "1", required = true)
            @PathVariable Long cardId) {
        return ResponseEntity.ok(transactionService.getTransactionsByCardId(cardId));
    }
}
