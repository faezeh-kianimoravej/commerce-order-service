package com.faezeh.commerce.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faezeh.commerce.order.client.ProductClient;
import com.faezeh.commerce.order.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.order.entity.Order;
import com.faezeh.commerce.order.entity.OrderStatus;
import com.faezeh.commerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductClient productClient;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        orderRepository.flush();
        when(productClient.getAvailability(anyLong(), anyInt()))
                .thenAnswer(invocation -> new ProductAvailabilityResponse(
                        invocation.getArgument(0), true, true, 100));
    }

    @Test
    void createAndRetrieveOrderByIdAndNumber() throws Exception {
        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(69.97))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].lineTotal").value(59.98))
                .andExpect(jsonPath("$.items[1].lineTotal").value(9.99))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdOrder = objectMapper.readTree(createResponse);
        long orderId = createdOrder.get("id").asLong();
        String orderNumber = createdOrder.get("orderNumber").asText();

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.totalAmount").value(69.97));

        mockMvc.perform(get("/api/orders/number/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));
    }

    @Test
    void invalidCreateOrderReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productSku": "",
                                      "quantity": 0,
                                      "unitPrice": -1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/orders"))
                .andExpect(jsonPath("$.fieldErrors['items[0].productId']").exists())
                .andExpect(jsonPath("$.fieldErrors['items[0].productSku']").exists())
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").exists())
                .andExpect(jsonPath("$.fieldErrors['items[0].unitPrice']").exists());
    }

    @Test
    void updateStatusAndCancelOrder() throws Exception {
        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long orderId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(delete("/api/orders/{id}", orderId))
                .andExpect(status().isNoContent());

        assertThat(orderRepository.findById(orderId))
                .isPresent()
                .get()
                .extracting(Order::getStatus)
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void missingOrderReturnsStructuredNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/api/orders/99"));
    }

    private String orderJson() {
        return """
                {
                  "items": [
                    {
                      "productId": 1,
                      "productSku": "TSHIRT-BLACK-L",
                      "quantity": 2,
                      "unitPrice": 29.99
                    },
                    {
                      "productId": 2,
                      "productSku": "SOCKS-WHITE",
                      "quantity": 1,
                      "unitPrice": 9.99
                    }
                  ]
                }
                """;
    }
}
