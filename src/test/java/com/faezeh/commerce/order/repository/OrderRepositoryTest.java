package com.faezeh.commerce.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.faezeh.commerce.order.entity.Order;
import com.faezeh.commerce.order.entity.OrderItem;
import com.faezeh.commerce.order.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByOrderNumberReturnsOrderWithItems() {
        Order savedOrder = orderRepository.saveAndFlush(order("ORD-123456789ABC"));

        assertThat(orderRepository.findByOrderNumber("ORD-123456789ABC"))
                .isPresent()
                .get()
                .satisfies(order -> {
                    assertThat(order.getId()).isEqualTo(savedOrder.getId());
                    assertThat(order.getItems()).hasSize(1);
                });
    }

    @Test
    void existsByOrderNumberReturnsTrueWhenOrderExists() {
        orderRepository.saveAndFlush(order("ORD-123456789ABC"));

        assertThat(orderRepository.existsByOrderNumber("ORD-123456789ABC")).isTrue();
        assertThat(orderRepository.existsByOrderNumber("ORD-MISSING")).isFalse();
    }

    @Test
    void persistsOrderItemsThroughAggregate() {
        Order savedOrder = orderRepository.saveAndFlush(order("ORD-123456789ABC"));
        entityManager.clear();

        Order loadedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();

        assertThat(loadedOrder.getItems()).hasSize(1);
        assertThat(loadedOrder.getItems().getFirst().getOrder().getId()).isEqualTo(savedOrder.getId());
        assertThat(loadedOrder.getTotalAmount()).isEqualByComparingTo("59.98");
    }

    @Test
    void timestampsUseInstant() {
        Order savedOrder = orderRepository.saveAndFlush(order("ORD-123456789ABC"));

        assertThat(savedOrder.getCreatedAt()).isNotNull();
        assertThat(savedOrder.getUpdatedAt()).isNotNull();
    }

    @Test
    void cancelledOrderRemainsPersisted() {
        Order order = order("ORD-123456789ABC");
        order.setStatus(OrderStatus.CANCELLED);

        Order savedOrder = orderRepository.saveAndFlush(order);

        assertThat(orderRepository.findById(savedOrder.getId()))
                .isPresent()
                .get()
                .extracting(Order::getStatus)
                .isEqualTo(OrderStatus.CANCELLED);
    }

    private Order order(String orderNumber) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("59.98"));
        order.addItem(orderItem());
        return order;
    }

    private OrderItem orderItem() {
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setProductSku("TSHIRT-BLACK-L");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("29.99"));
        item.setLineTotal(new BigDecimal("59.98"));
        return item;
    }
}
