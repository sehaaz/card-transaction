package com.transit.cardservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Request body for topping up a card balance")
public class TopUpRequest {

    @Schema(description = "Amount to add to the card balance", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
