package com.faezeh.commerce.order.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Product not found or inactive: productId=" + productId);
    }
}
