package com.faezeh.commerce.order.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.faezeh.commerce.order.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.order.exception.ProductNotFoundException;
import com.faezeh.commerce.order.exception.ProductServiceCommunicationException;
import com.faezeh.commerce.order.exception.ProductServiceException;
import com.faezeh.commerce.order.exception.ProductUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        classes = ProductAvailabilityResilienceTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "resilience4j.circuitbreaker.circuit-breaker-aspect-order=1",
                "resilience4j.retry.retry-aspect-order=2",
                "resilience4j.timelimiter.time-limiter-aspect-order=3",
                "resilience4j.circuitbreaker.instances.productService.sliding-window-type=count_based",
                "resilience4j.circuitbreaker.instances.productService.sliding-window-size=2",
                "resilience4j.circuitbreaker.instances.productService.minimum-number-of-calls=2",
                "resilience4j.circuitbreaker.instances.productService.failure-rate-threshold=50",
                "resilience4j.circuitbreaker.instances.productService.wait-duration-in-open-state=30s",
                "resilience4j.circuitbreaker.instances.productService.record-exceptions[0]=com.faezeh.commerce.order.exception.ProductServiceException",
                "resilience4j.circuitbreaker.instances.productService.record-exceptions[1]=java.util.concurrent.TimeoutException",
                "resilience4j.circuitbreaker.instances.productService.ignore-exceptions[0]=com.faezeh.commerce.order.exception.ProductNotFoundException",
                "resilience4j.circuitbreaker.instances.productService.ignore-exceptions[1]=com.faezeh.commerce.order.exception.ProductUnavailableException",
                "resilience4j.retry.instances.productService.max-attempts=3",
                "resilience4j.retry.instances.productService.wait-duration=1ms",
                "resilience4j.retry.instances.productService.retry-exceptions[0]=com.faezeh.commerce.order.exception.ProductServiceCommunicationException",
                "resilience4j.retry.instances.productService.retry-exceptions[1]=java.util.concurrent.TimeoutException",
                "resilience4j.retry.instances.productService.ignore-exceptions[0]=com.faezeh.commerce.order.exception.ProductNotFoundException",
                "resilience4j.retry.instances.productService.ignore-exceptions[1]=com.faezeh.commerce.order.exception.ProductUnavailableException",
                "resilience4j.timelimiter.instances.productService.timeout-duration=50ms",
                "resilience4j.timelimiter.instances.productService.cancel-running-future=true"
        }
)
class ProductAvailabilityResilienceTest {

    @jakarta.annotation.Resource
    private ProductAvailabilityResilience resilience;

    @jakarta.annotation.Resource
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker(ProductAvailabilityResilience.PRODUCT_SERVICE_RESILIENCE_NAME).reset();
    }

    @Test
    void executeReturnsSuccessfulResponse() {
        ProductAvailabilityResponse response = resilience.execute(1L, 2, () -> availableResponse(1L));

        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.existsAndActive()).isTrue();
        assertThat(response.available()).isTrue();
    }

    @Test
    void executeRetriesTransientFailureAndReturnsSuccess() {
        AtomicInteger attempts = new AtomicInteger();

        ProductAvailabilityResponse response = resilience.execute(1L, 2, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new ProductServiceCommunicationException("temporary Product Service failure");
            }
            return availableResponse(1L);
        });

        assertThat(response.available()).isTrue();
        assertThat(attempts).hasValue(2);
    }

    @Test
    void executeTimesOutSlowCall() {
        assertThatThrownBy(() -> resilience.execute(1L, 2, () -> {
            sleep(250);
            return availableResponse(1L);
        }))
                .isInstanceOf(ProductServiceCommunicationException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void executeFailsFastWhenCircuitBreakerIsOpen() {
        AtomicInteger attempts = new AtomicInteger();
        circuitBreakerRegistry.circuitBreaker(ProductAvailabilityResilience.PRODUCT_SERVICE_RESILIENCE_NAME)
                .transitionToOpenState();

        assertThatThrownBy(() -> resilience.execute(1L, 2, () -> {
            attempts.incrementAndGet();
            return availableResponse(1L);
        }))
                .isInstanceOf(ProductServiceException.class)
                .hasMessageContaining("circuit breaker is open");
        assertThat(attempts).hasValue(0);
    }

    @Test
    void executeDoesNotRetryBusinessValidationErrors() {
        AtomicInteger notFoundAttempts = new AtomicInteger();

        assertThatThrownBy(() -> resilience.execute(1L, 2, () -> {
            notFoundAttempts.incrementAndGet();
            throw new ProductNotFoundException(1L);
        }))
                .isInstanceOf(ProductNotFoundException.class);
        assertThat(notFoundAttempts).hasValue(1);

        AtomicInteger unavailableAttempts = new AtomicInteger();

        assertThatThrownBy(() -> resilience.execute(1L, 2, () -> {
            unavailableAttempts.incrementAndGet();
            throw new ProductUnavailableException(1L, 2, 1);
        }))
                .isInstanceOf(ProductUnavailableException.class);
        assertThat(unavailableAttempts).hasValue(1);
    }

    private static ProductAvailabilityResponse availableResponse(Long productId) {
        return new ProductAvailabilityResponse(productId, true, true, 25);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import({
            ProductAvailabilityResilience.class,
            ProductAvailabilityOperation.class,
            TestConfig.class
    })
    static class TestApplication {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean(destroyMethod = "shutdown")
        ExecutorService productServiceExecutor() {
            return Executors.newVirtualThreadPerTaskExecutor();
        }
    }
}
