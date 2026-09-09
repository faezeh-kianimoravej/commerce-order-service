package com.faezeh.commerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.faezeh.commerce.order.dto.CreateOrderItemRequest;
import com.faezeh.commerce.order.dto.CreateOrderRequest;
import com.faezeh.commerce.order.dto.OrderResponse;
import com.faezeh.commerce.order.dto.UpdateOrderStatusRequest;
import com.faezeh.commerce.order.entity.Order;
import com.faezeh.commerce.order.entity.OrderItem;
import com.faezeh.commerce.order.entity.OrderStatus;
import com.faezeh.commerce.order.exception.OrderNotFoundException;
import com.faezeh.commerce.order.metrics.OrderMetrics;
import com.faezeh.commerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMetrics orderMetrics;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderCalculatesTotalsGeneratesOrderNumberAndReturnsCreatedOrder() {
        CreateOrderRequest request = createOrderRequest();
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(Instant.parse("2026-09-09T18:00:00Z"));
            order.setUpdatedAt(Instant.parse("2026-09-09T18:00:00Z"));
            long nextItemId = 1L;
            for (OrderItem item : order.getItems()) {
                item.setId(nextItemId++);
            }
            return order;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.orderNumber()).startsWith("ORD-").hasSize(16);
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.totalAmount()).isEqualByComparingTo("69.97");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("59.98");
        assertThat(response.items().get(1).lineTotal()).isEqualByComparingTo("9.99");
        verify(orderMetrics).orderCreated();
    }

    @Test
    void createOrderRetriesGeneratedOrderNumberWhenCollisionOccurs() {
        when(orderRepository.existsByOrderNumber(any())).thenReturn(true, false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(createOrderRequest());

        verify(orderRepository).save(any(Order.class));
        verify(orderMetrics).orderCreated();
    }

    @Test
    void createOrderDoesNotIncrementCounterWhenSaveFails() {
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(orderMetrics, never()).orderCreated();
    }

    @Test
    void getAllOrdersReturnsOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(order()));

        List<OrderResponse> orders = orderService.getAllOrders();

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().orderNumber()).isEqualTo("ORD-123456789ABC");
        verify(orderMetrics).orderList();
    }

    @Test
    void getAllOrdersDoesNotIncrementCounterWhenRepositoryFails() {
        when(orderRepository.findAll()).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> orderService.getAllOrders())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(orderMetrics, never()).orderList();
    }

    @Test
    void getOrderByIdReturnsExistingOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order()));

        OrderResponse response = orderService.getOrderById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.orderNumber()).isEqualTo("ORD-123456789ABC");
        verify(orderMetrics).orderLookup();
    }

    @Test
    void getOrderByIdThrowsWhenOrderIsMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("id: 99");
        verify(orderMetrics, never()).orderLookup();
    }

    @Test
    void getOrderByNumberReturnsExistingOrder() {
        when(orderRepository.findByOrderNumber("ORD-123456789ABC")).thenReturn(Optional.of(order()));

        OrderResponse response = orderService.getOrderByNumber("ORD-123456789ABC");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.orderNumber()).isEqualTo("ORD-123456789ABC");
        verify(orderMetrics).orderLookup();
    }

    @Test
    void getOrderByNumberThrowsWhenOrderIsMissing() {
        when(orderRepository.findByOrderNumber("ORD-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByNumber("ORD-MISSING"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("orderNumber: ORD-MISSING");
        verify(orderMetrics, never()).orderLookup();
    }

    @Test
    void updateOrderStatusReturnsUpdatedOrder() {
        Order order = order();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(
                1L,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)
        );

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderMetrics).orderStatusUpdated();
    }

    @Test
    void updateOrderStatusDoesNotIncrementCounterWhenOrderIsMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("id: 1");
        verify(orderMetrics, never()).orderStatusUpdated();
    }

    @Test
    void updateOrderStatusDoesNotIncrementCounterWhenSaveFails() {
        Order order = order();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(orderMetrics, never()).orderStatusUpdated();
    }

    @Test
    void cancelOrderSetsStatusToCancelled() {
        Order order = order();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
        verify(orderRepository, never()).delete(order);
        verify(orderMetrics).orderCancelled();
    }

    @Test
    void cancelOrderDoesNotIncrementCounterWhenOrderIsMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("id: 1");
        verify(orderMetrics, never()).orderCancelled();
    }

    @Test
    void cancelOrderDoesNotIncrementCounterWhenSaveFails() {
        Order order = order();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(orderMetrics, never()).orderCancelled();
    }

    private CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(1L, "TSHIRT-BLACK-L", 2, new BigDecimal("29.99")),
                new CreateOrderItemRequest(2L, "SOCKS-WHITE", 1, new BigDecimal("9.99"))
        ));
    }

    private Order order() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-123456789ABC");
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("59.98"));
        order.setCreatedAt(Instant.parse("2026-09-09T18:00:00Z"));
        order.setUpdatedAt(Instant.parse("2026-09-09T18:00:00Z"));
        order.setItems(new ArrayList<>());
        order.addItem(orderItem());
        return order;
    }

    private OrderItem orderItem() {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(1L);
        item.setProductSku("TSHIRT-BLACK-L");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("29.99"));
        item.setLineTotal(new BigDecimal("59.98"));
        return item;
    }
}
