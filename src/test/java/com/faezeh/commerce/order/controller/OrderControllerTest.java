package com.faezeh.commerce.order.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.faezeh.commerce.order.dto.OrderItemResponse;
import com.faezeh.commerce.order.dto.OrderResponse;
import com.faezeh.commerce.order.entity.OrderStatus;
import com.faezeh.commerce.order.exception.OrderNotFoundException;
import com.faezeh.commerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void getOrdersReturnsOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(orderResponse(OrderStatus.CREATED)));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-123456789ABC"))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        when(orderService.createOrder(any())).thenReturn(orderResponse(OrderStatus.CREATED));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateOrderJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456789ABC"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(59.98))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void createOrderWithInvalidRequestReturnsBadRequest() throws Exception {
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
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/orders"))
                .andExpect(jsonPath("$.fieldErrors['items[0].productId']").exists())
                .andExpect(jsonPath("$.fieldErrors['items[0].productSku']").exists())
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").exists())
                .andExpect(jsonPath("$.fieldErrors['items[0].unitPrice']").exists());
    }

    @Test
    void createOrderWithEmptyItemsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void getOrderByIdReturnsOrder() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse(OrderStatus.CREATED));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456789ABC"));
    }

    @Test
    void getMissingOrderReturnsNotFound() throws Exception {
        when(orderService.getOrderById(99L)).thenThrow(new OrderNotFoundException("id", 99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/orders/99"))
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
    }

    @Test
    void getOrderByNumberReturnsOrder() throws Exception {
        when(orderService.getOrderByNumber("ORD-123456789ABC")).thenReturn(orderResponse(OrderStatus.CREATED));

        mockMvc.perform(get("/api/orders/number/ORD-123456789ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-123456789ABC"));
    }

    @Test
    void updateOrderStatusReturnsUpdatedOrder() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), any())).thenReturn(orderResponse(OrderStatus.CONFIRMED));

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrderStatusWithMissingStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void updateOrderStatusWithInvalidEnumReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SHIPPED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void cancelOrderReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrder(1L);
    }

    private String validCreateOrderJson() {
        return """
                {
                  "items": [
                    {
                      "productId": 1,
                      "productSku": "TSHIRT-BLACK-L",
                      "quantity": 2,
                      "unitPrice": 29.99
                    }
                  ]
                }
                """;
    }

    private OrderResponse orderResponse(OrderStatus status) {
        return new OrderResponse(
                1L,
                "ORD-123456789ABC",
                status,
                new BigDecimal("59.98"),
                Instant.parse("2026-09-09T18:00:00Z"),
                Instant.parse("2026-09-09T18:00:00Z"),
                List.of(new OrderItemResponse(
                        1L,
                        1L,
                        "TSHIRT-BLACK-L",
                        2,
                        new BigDecimal("29.99"),
                        new BigDecimal("59.98")
                ))
        );
    }
}
