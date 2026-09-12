package com.faezeh.commerce.order.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.faezeh.commerce.order.client.ProductClient;
import com.faezeh.commerce.order.dto.CreateOrderItemRequest;
import com.faezeh.commerce.order.dto.CreateOrderRequest;
import com.faezeh.commerce.order.dto.OrderItemResponse;
import com.faezeh.commerce.order.dto.OrderResponse;
import com.faezeh.commerce.order.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.order.dto.UpdateOrderStatusRequest;
import com.faezeh.commerce.order.entity.Order;
import com.faezeh.commerce.order.entity.OrderItem;
import com.faezeh.commerce.order.entity.OrderStatus;
import com.faezeh.commerce.order.exception.OrderNotFoundException;
import com.faezeh.commerce.order.exception.ProductNotFoundException;
import com.faezeh.commerce.order.exception.ProductServiceException;
import com.faezeh.commerce.order.exception.ProductUnavailableException;
import com.faezeh.commerce.order.metrics.OrderMetrics;
import com.faezeh.commerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDER_NUMBER_PREFIX = "ORD-";

    private final OrderRepository orderRepository;
    private final OrderMetrics orderMetrics;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, OrderMetrics orderMetrics, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.orderMetrics = orderMetrics;
        this.productClient = productClient;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        validateProducts(request.items());

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.CREATED);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            OrderItem item = toOrderItem(itemRequest);
            totalAmount = totalAmount.add(item.getLineTotal());
            order.addItem(item);
        }
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        orderMetrics.orderCreated();
        LOGGER.info("Order created: id={}, orderNumber={}", savedOrder.getId(), savedOrder.getOrderNumber());
        return toResponse(savedOrder);
    }

    private void validateProducts(List<CreateOrderItemRequest> items) {
        for (CreateOrderItemRequest item : items) {
            ProductAvailabilityResponse availability =
                    productClient.getAvailability(item.productId(), item.quantity());

            if (availability.productId() != null && !availability.productId().equals(item.productId())) {
                LOGGER.error("Product Service returned mismatched productId: requestedProductId={}, responseProductId={}",
                        item.productId(), availability.productId());
                throw new ProductServiceException(
                        "Product Service returned mismatched availability response for productId=" + item.productId());
            }

            if (!Boolean.TRUE.equals(availability.existsAndActive())) {
                LOGGER.warn("Order rejected because product is missing or inactive: productId={}", item.productId());
                throw new ProductNotFoundException(item.productId());
            }

            if (!Boolean.TRUE.equals(availability.available())) {
                LOGGER.warn(
                        "Order rejected because product has insufficient stock: productId={}, requestedQuantity={}, availableQuantity={}",
                        item.productId(), item.quantity(), availability.availableQuantity());
                throw new ProductUnavailableException(
                        item.productId(), item.quantity(), availability.availableQuantity());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<OrderResponse> orders = orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        orderMetrics.orderList();
        return orders;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderById(id);
        orderMetrics.orderLookup();
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> {
                    LOGGER.warn("Order not found: orderNumber={}", orderNumber);
                    return new OrderNotFoundException("orderNumber", orderNumber);
                });
        orderMetrics.orderLookup();
        return toResponse(order);
    }

    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = findOrderById(id);
        order.setStatus(request.status());

        Order savedOrder = orderRepository.save(order);
        orderMetrics.orderStatusUpdated();
        LOGGER.info("Order status updated: id={}, orderNumber={}, status={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), savedOrder.getStatus());
        return toResponse(savedOrder);
    }

    public void cancelOrder(Long id) {
        Order order = findOrderById(id);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        orderMetrics.orderCancelled();
        LOGGER.info("Order cancelled: id={}, orderNumber={}", order.getId(), order.getOrderNumber());
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.warn("Order not found: id={}", id);
                    return new OrderNotFoundException("id", id);
                });
    }

    private OrderItem toOrderItem(CreateOrderItemRequest request) {
        BigDecimal lineTotal = request.unitPrice().multiply(BigDecimal.valueOf(request.quantity()));

        OrderItem item = new OrderItem();
        item.setProductId(request.productId());
        item.setProductSku(request.productSku());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setLineTotal(lineTotal);
        return item;
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = ORDER_NUMBER_PREFIX + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(this::toResponse).toList()
        );
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
