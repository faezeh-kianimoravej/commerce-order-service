package com.faezeh.commerce.order.client;

import com.faezeh.commerce.order.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.order.exception.ProductNotFoundException;
import com.faezeh.commerce.order.exception.ProductServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductClient.class);

    private final RestClient productRestClient;

    public ProductClient(@Qualifier("productRestClient") RestClient productRestClient) {
        this.productRestClient = productRestClient;
    }

    public ProductAvailabilityResponse getAvailability(Long productId, Integer quantity) {
        LOGGER.info("Checking product availability: productId={}, requestedQuantity={}", productId, quantity);

        try {
            ProductAvailabilityResponse response = productRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/products/{productId}/availability")
                            .queryParam("quantity", quantity)
                            .build(productId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                        LOGGER.warn("Product Service rejected availability check: productId={}, status={}",
                                productId, clientResponse.getStatusCode().value());
                        if (clientResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                            throw new ProductNotFoundException(productId);
                        }
                        throw new ProductServiceException(
                                "Product Service rejected availability request for productId=" + productId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                        LOGGER.error("Product Service error during availability check: productId={}, status={}",
                                productId, clientResponse.getStatusCode().value());
                        throw new ProductServiceException(
                                "Product Service error while validating productId=" + productId);
                    })
                    .body(ProductAvailabilityResponse.class);

            if (response == null) {
                throw new ProductServiceException(
                        "Product Service returned an empty availability response for productId=" + productId);
            }

            LOGGER.info(
                    "Product availability checked: productId={}, existsAndActive={}, available={}, availableQuantity={}",
                    response.productId(), response.existsAndActive(), response.available(), response.availableQuantity());
            return response;
        } catch (ProductNotFoundException | ProductServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            LOGGER.error("Product Service unavailable during availability check: productId={}", productId, exception);
            throw new ProductServiceException(
                    "Product Service unavailable while validating productId=" + productId, exception);
        }
    }
}
