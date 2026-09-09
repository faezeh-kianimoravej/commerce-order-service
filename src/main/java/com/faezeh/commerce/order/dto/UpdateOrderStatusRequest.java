package com.faezeh.commerce.order.dto;

import com.faezeh.commerce.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for updating an order status")
public record UpdateOrderStatusRequest(
        @Schema(description = "New order status", example = "CONFIRMED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        OrderStatus status
) {
}
