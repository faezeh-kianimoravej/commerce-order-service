package com.faezeh.commerce.order.client;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.faezeh.commerce.order.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.order.exception.ProductNotFoundException;
import com.faezeh.commerce.order.exception.ProductServiceCommunicationException;
import com.faezeh.commerce.order.exception.ProductServiceException;
import com.faezeh.commerce.order.exception.ProductUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductAvailabilityResilience {

    static final String PRODUCT_SERVICE_RESILIENCE_NAME = "productService";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAvailabilityResilience.class);

    private final ProductAvailabilityOperation operation;

    public ProductAvailabilityResilience(ProductAvailabilityOperation operation, RetryRegistry retryRegistry) {
        this.operation = operation;
        retryRegistry.retry(PRODUCT_SERVICE_RESILIENCE_NAME).getEventPublisher()
                .onRetry(event -> LOGGER.warn("Retrying Product Service availability check: attempt={}, cause={}",
                        event.getNumberOfRetryAttempts(), rootCauseName(event.getLastThrowable())));
    }

    public ProductAvailabilityResponse execute(
            Long productId,
            Integer quantity,
            Supplier<ProductAvailabilityResponse> availabilityCall
    ) {
        try {
            return operation.execute(productId, quantity, availabilityCall).toCompletableFuture().join();
        } catch (CompletionException exception) {
            throw mapFailure(productId, unwrap(exception));
        } catch (RuntimeException exception) {
            throw mapFailure(productId, unwrap(exception));
        }
    }

    private RuntimeException mapFailure(Long productId, Throwable failure) {
        if (failure instanceof CallNotPermittedException) {
            LOGGER.warn("Product Service circuit breaker is open: productId={}", productId);
            return new ProductServiceException(
                    "Product Service circuit breaker is open while validating productId=" + productId, failure);
        }
        if (failure instanceof TimeoutException) {
            LOGGER.warn("Product Service availability check timed out: productId={}", productId);
            return new ProductServiceCommunicationException(
                    "Product Service timed out while validating productId=" + productId, failure);
        }
        if (failure instanceof ProductNotFoundException exception) {
            return exception;
        }
        if (failure instanceof ProductUnavailableException exception) {
            return exception;
        }
        if (failure instanceof ProductServiceException exception) {
            return exception;
        }

        LOGGER.error("Unexpected Product Service validation failure: productId={}", productId, failure);
        return new ProductServiceException("Product Service validation failed for productId=" + productId, failure);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String rootCauseName(Throwable failure) {
        if (failure == null) {
            return "unknown";
        }
        return unwrap(failure).getClass().getSimpleName();
    }
}
