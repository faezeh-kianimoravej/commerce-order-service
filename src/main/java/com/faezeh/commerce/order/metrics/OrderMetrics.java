package com.faezeh.commerce.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter orderCreatedCounter;
    private final Counter orderStatusUpdatedCounter;
    private final Counter orderCancelledCounter;
    private final Counter orderLookupCounter;
    private final Counter orderListCounter;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.orderCreatedCounter = meterRegistry.counter("order.created.count");
        this.orderStatusUpdatedCounter = meterRegistry.counter("order.status.updated.count");
        this.orderCancelledCounter = meterRegistry.counter("order.cancelled.count");
        this.orderLookupCounter = meterRegistry.counter("order.lookup.count");
        this.orderListCounter = meterRegistry.counter("order.list.count");
    }

    public void orderCreated() {
        orderCreatedCounter.increment();
    }

    public void orderStatusUpdated() {
        orderStatusUpdatedCounter.increment();
    }

    public void orderCancelled() {
        orderCancelledCounter.increment();
    }

    public void orderLookup() {
        orderLookupCounter.increment();
    }

    public void orderList() {
        orderListCounter.increment();
    }
}
