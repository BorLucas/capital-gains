package com.example.capitalgains.application;

import com.example.capitalgains.orders.OrderStatus;
import com.example.capitalgains.orders.SimulationOrder;
import com.example.capitalgains.orders.SimulationOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The worker that drains the order queue — the "matching engine". On each tick
 * it claims the oldest pending orders and hands each one to
 * {@link OrderExecution} for assessment.
 *
 * <p>Single instance, single thread: orders are processed in submission order.
 * Scaling this out would mean a real broker (queue with competing consumers).</p>
 */
@Component
@Profile("!cli")
public class OrderProcessor {

    private final SimulationOrderRepository orders;
    private final OrderExecution execution;
    private final int batchSize;

    public OrderProcessor(SimulationOrderRepository orders,
                          OrderExecution execution,
                          @Value("${orders.batch-size:10}") int batchSize) {
        this.orders = orders;
        this.execution = execution;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${orders.poll-interval-ms:200}")
    public void drainQueue() {
        List<UUID> pending = orders
                .findByStatusOrderBySubmittedAtAsc(OrderStatus.PENDING, PageRequest.of(0, batchSize))
                .stream()
                .map(SimulationOrder::getId)
                .toList();

        for (UUID id : pending) {
            execution.execute(id);
        }
    }
}
