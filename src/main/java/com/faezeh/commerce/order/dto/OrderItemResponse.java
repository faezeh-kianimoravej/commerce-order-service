package com.faezeh.commerce.order.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order line item response")
public record OrderItemResponse(
        @Schema(description = "Order item identifier", example = "1")
        Long id,

        @Schema(description = "Product identifier", example = "1")
        Long productId,

        @Schema(description = "Product SKU", example = "TSHIRT-BLACK-L")
        String productSku,

        @Schema(description = "Quantity ordered", example = "2")
        Integer quantity,

        @Schema(description = "Unit price at order time", example = "29.99")
        BigDecimal unitPrice,

        @Schema(description = "Line total", example = "59.98")
        BigDecimal lineTotal
) {
}
