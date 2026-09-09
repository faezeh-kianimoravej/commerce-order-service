package com.faezeh.commerce.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String field, Object value) {
        super("Order not found with " + field + ": " + value);
    }
}
