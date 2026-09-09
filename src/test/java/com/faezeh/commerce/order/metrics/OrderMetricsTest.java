package com.faezeh.commerce.order.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private OrderMetrics orderMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderMetrics = new OrderMetrics(meterRegistry);
    }

    @Test
    void incrementsOrderCountersWithExpectedMetricNames() {
        orderMetrics.orderCreated();
        orderMetrics.orderStatusUpdated();
        orderMetrics.orderCancelled();
        orderMetrics.orderLookup();
        orderMetrics.orderList();

        assertThat(counterCount("order.created.count")).isEqualTo(1.0);
        assertThat(counterCount("order.status.updated.count")).isEqualTo(1.0);
        assertThat(counterCount("order.cancelled.count")).isEqualTo(1.0);
        assertThat(counterCount("order.lookup.count")).isEqualTo(1.0);
        assertThat(counterCount("order.list.count")).isEqualTo(1.0);
    }

    private double counterCount(String counterName) {
        return meterRegistry.counter(counterName).count();
    }
}
