package com.faezeh.commerce.order.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.faezeh.commerce.order.dto.ProductAvailabilityResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ProductAvailabilityOperation {

    private final ExecutorService productServiceExecutor;

    ProductAvailabilityOperation(@Qualifier("productServiceExecutor") ExecutorService productServiceExecutor) {
        this.productServiceExecutor = productServiceExecutor;
    }

    @CircuitBreaker(name = ProductAvailabilityResilience.PRODUCT_SERVICE_RESILIENCE_NAME)
    @Retry(name = ProductAvailabilityResilience.PRODUCT_SERVICE_RESILIENCE_NAME)
    @TimeLimiter(name = ProductAvailabilityResilience.PRODUCT_SERVICE_RESILIENCE_NAME)
    public CompletionStage<ProductAvailabilityResponse> execute(
            Long productId,
            Integer quantity,
            Supplier<ProductAvailabilityResponse> availabilityCall
    ) {
        return CompletableFuture.supplyAsync(availabilityCall, productServiceExecutor);
    }
}
