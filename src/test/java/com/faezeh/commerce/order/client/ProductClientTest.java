package com.faezeh.commerce.order.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.faezeh.commerce.order.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.order.exception.ProductNotFoundException;
import com.faezeh.commerce.order.exception.ProductServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProductClientTest {

    private MockRestServiceServer server;
    private ProductClient productClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("http://product-service");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        productClient = new ProductClient(restClientBuilder.build());
    }

    @Test
    void getAvailabilityMapsProductServiceResponse() {
        server.expect(once(), requestTo("http://product-service/api/products/1/availability?quantity=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "productId": 1,
                          "existsAndActive": true,
                          "available": true,
                          "availableQuantity": 25
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductAvailabilityResponse response = productClient.getAvailability(1L, 2);

        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.existsAndActive()).isTrue();
        assertThat(response.available()).isTrue();
        assertThat(response.availableQuantity()).isEqualTo(25);
        server.verify();
    }

    @Test
    void getAvailabilityMapsNotFoundResponseToDomainException() {
        server.expect(once(), requestTo("http://product-service/api/products/1/availability?quantity=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> productClient.getAvailability(1L, 2))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("productId=1");
        server.verify();
    }

    @Test
    void getAvailabilityMapsProductServiceHttpErrorToDomainException() {
        server.expect(once(), requestTo("http://product-service/api/products/1/availability?quantity=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> productClient.getAvailability(1L, 2))
                .isInstanceOf(ProductServiceException.class)
                .hasMessageContaining("Product Service error");
        server.verify();
    }
}
