package com.faezeh.commerce.order.dto;

public record ProductAvailabilityResponse(
        Long productId,
        Boolean existsAndActive,
        Boolean available,
        Integer availableQuantity
) {
}
