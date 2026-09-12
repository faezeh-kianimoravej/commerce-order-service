package com.faezeh.commerce.order.exception;

public class ProductServiceCommunicationException extends ProductServiceException {

    public ProductServiceCommunicationException(String message) {
        super(message);
    }

    public ProductServiceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
