package com.faezeh.commerce.order.exception;

public class ProductUnavailableException extends RuntimeException {

    public ProductUnavailableException(Long productId, Integer requestedQuantity, Integer availableQuantity) {
        super("Insufficient product quantity: productId=" + productId
                + ", requestedQuantity=" + requestedQuantity
                + ", availableQuantity=" + availableQuantity);
    }
}
