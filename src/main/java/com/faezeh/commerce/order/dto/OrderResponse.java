package com.faezeh.commerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.faezeh.commerce.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order response")
public record OrderResponse(
        @Schema(description = "Order identifier", example = "1")
        Long id,

        @Schema(description = "Generated order number", example = "ORD-A1B2C3D4E5F6")
        String orderNumber,

        @Schema(description = "Current order status", example = "CREATED")
        OrderStatus status,

        @Schema(description = "Order total amount", example = "89.97")
        BigDecimal totalAmount,

        @Schema(description = "Creation timestamp", example = "2026-09-09T18:00:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp", example = "2026-09-09T18:00:00Z")
        Instant updatedAt,

        @ArraySchema(schema = @Schema(implementation = OrderItemResponse.class))
        List<OrderItemResponse> items
) {
}
