package com.transit.cardservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for creating a new transit card")
public class CreateCardRequest {

    @Schema(description = "Full name of the card owner", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerName;
}
