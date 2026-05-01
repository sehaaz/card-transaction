package com.transit.cardservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Transit card entity")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the card", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(unique = true, nullable = false)
    @Schema(description = "Auto-generated UUID card number", example = "550e8400-e29b-41d4-a716-446655440000", accessMode = Schema.AccessMode.READ_ONLY)
    private String cardNumber;

    @Column(nullable = false)
    @Schema(description = "Full name of the card owner", example = "John Doe")
    private String ownerName;

    @Column(nullable = false)
    @Schema(description = "Current balance on the card", example = "150.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal balance;

    @Schema(description = "Timestamp when the card was created", example = "2026-04-30T10:15:30", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.cardNumber = UUID.randomUUID().toString();
        this.balance = this.balance == null ? BigDecimal.ZERO : this.balance;
        this.createdAt = LocalDateTime.now();
    }
}
