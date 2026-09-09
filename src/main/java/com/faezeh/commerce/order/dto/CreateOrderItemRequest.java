package com.faezeh.commerce.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for an order line item")
public record CreateOrderItemRequest(
        @Schema(description = "Product identifier", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long productId,

        @Schema(description = "Product SKU", example = "TSHIRT-BLACK-L", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String productSku,

        @Schema(description = "Quantity ordered", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Integer quantity,

        @Schema(description = "Unit price at order time", example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        BigDecimal unitPrice
) {
}
