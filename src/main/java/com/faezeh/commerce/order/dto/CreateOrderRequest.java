package com.faezeh.commerce.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for creating an order")
public record CreateOrderRequest(
        @ArraySchema(schema = @Schema(implementation = CreateOrderItemRequest.class))
        @NotEmpty
        List<@Valid CreateOrderItemRequest> items
) {
}
