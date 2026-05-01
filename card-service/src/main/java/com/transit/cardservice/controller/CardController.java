package com.transit.cardservice.controller;

import com.transit.cardservice.dto.CreateCardRequest;
import com.transit.cardservice.dto.ErrorResponse;
import com.transit.cardservice.dto.TopUpRequest;
import com.transit.cardservice.entity.Card;
import com.transit.cardservice.service.CardService;
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
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "Endpoints for creating, retrieving, and topping up transit cards")
public class CardController {

        private final CardService cardService;

        @PostMapping("/createCard")
        @Operation(summary = "Create a new transit card", description = "Creates a new transit card with the given owner name. A unique card number (UUID) is auto-generated and the initial balance is set to 0.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Card created successfully", content = @Content(schema = @Schema(implementation = Card.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                                        @ExampleObject(name = "Missing owner name", summary = "Owner name is null or empty", value = """
                                                        {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_OWNER_NAME","message":"Owner name must not be null or empty"}
                                                        """),
                                        @ExampleObject(name = "Malformed JSON", summary = "Request body is not valid JSON", value = """
                                                        {"timestamp":"2026-05-01T14:30:00","status":400,"error":"MALFORMED_REQUEST","message":"Request body is missing or contains invalid JSON"}
                                                        """)
                        })),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Server error", value = """
                                        {"timestamp":"2026-05-01T14:30:00","status":500,"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
                                        """)))
        })
        public ResponseEntity<Card> createCard(@RequestBody CreateCardRequest request) {
                Card card = cardService.createCard(request.getOwnerName());
                return ResponseEntity.status(HttpStatus.CREATED).body(card);
        }

        @GetMapping
        @Operation(summary = "Get all cards", description = "Retrieves a list of all transit cards in the system.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully (empty list if none exist)", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Card.class)))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Server error", value = """
                                        {"timestamp":"2026-05-01T14:30:00","status":500,"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
                                        """)))
        })
        public ResponseEntity<List<Card>> getAllCards() {
                return ResponseEntity.ok(cardService.getAllCards());
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get card by ID", description = "Retrieves a transit card's details including card number, owner name, balance, and creation timestamp.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Card found", content = @Content(schema = @Schema(implementation = Card.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Invalid ID format", summary = "Non-numeric card ID", value = """
                                        {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_PARAMETER","message":"Invalid value for parameter 'id': abc"}
                                        """))),
                        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Card not found", summary = "No card exists with the given ID", value = """
                                        {"timestamp":"2026-05-01T14:30:00","status":404,"error":"CARD_NOT_FOUND","message":"Card not found with id: 99"}
                                        """))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Server error", value = """
                                        {"timestamp":"2026-05-01T14:30:00","status":500,"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
                                        """)))
        })
        public ResponseEntity<Card> getCardById(
                        @Parameter(description = "ID of the card to retrieve", example = "1", required = true) @PathVariable Long id) {
                return ResponseEntity.ok(cardService.getCardById(id));
        }

        @PutMapping("/{id}/topup")
        @Operation(summary = "Top up card balance", description = "Adds the specified amount to the card's current balance. Use a negative amount to subtract (used internally by transaction-service for payments).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Balance updated successfully", content = @Content(schema = @Schema(implementation = Card.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                                        @ExampleObject(name = "Invalid amount", summary = "Amount is null or zero", value = """
                                                        {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_AMOUNT","message":"Invalid amount: null. Amount must not be null or zero"}
                                                        """),
                                        @ExampleObject(name = "Invalid ID format", summary = "Non-numeric card ID in path", value = """
                                                        {"timestamp":"2026-05-01T14:30:00","status":400,"error":"INVALID_PARAMETER","message":"Invalid value for parameter 'id': abc"}
                                                        """),
                                        @ExampleObject(name = "Malformed JSON", summary = "Request body is not valid JSON", value = """
                                                        {"timestamp":"2026-05-01T14:30:00","status":400,"error":"MALFORMED_REQUEST","message":"Request body is missing or contains invalid JSON"}
                                                        """)
                        })),
                        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Card not found", value = """
                                        {"timestamp":"2026-05-01T14:30:00","status":404,"error":"CARD_NOT_FOUND","message":"Card not found with id: 99"}
                                        """))),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(name = "Server error", value = """
                                        {"timestamp":"2026-05-01T14:30:00","status":500,"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
                                        """)))
        })
        public ResponseEntity<Card> topUp(
                        @Parameter(description = "ID of the card to top up", example = "1", required = true) @PathVariable Long id,
                        @RequestBody TopUpRequest request) {
                Card card = cardService.topUp(id, request.getAmount());
                return ResponseEntity.ok(card);
        }
}
