package com.transit.transactionservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Transit card transaction entity")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the transaction", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "ID of the card associated with this transaction", example = "1")
    private Long cardId;

    @Column(nullable = false)
    @Schema(description = "Transaction amount", example = "25.50")
    private BigDecimal amount;

    @Column(nullable = false)
    @Schema(description = "Transaction type", example = "TOPUP", allowableValues = {"TOPUP", "PAYMENT"})
    private String type;

    @Schema(description = "Timestamp when the transaction was recorded", example = "2026-04-30T10:15:30", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
