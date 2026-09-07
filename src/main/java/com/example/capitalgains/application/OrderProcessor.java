package com.example.capitalgains.application;

import com.example.capitalgains.domain.error.CapitalGainsException;
import com.example.capitalgains.orders.SimulationOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The worker that drains the order queue — the "matching engine". On each tick
 * it claims the oldest pending orders and assesses each one, then sweeps up any
 * order left {@code PROCESSING} by a worker that never finished.
 *
 * <p>Single instance, single thread: orders are processed in submission order.
 * Scaling this out would mean a real broker (queue with competing consumers).</p>
 */
@Component
@Profile("!cli")
public class OrderProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    private final SimulationOrderRepository orders;
    private final OrderExecution execution;
    private final Clock clock;
    private final int batchSize;
    private final Duration processingTimeout;
    private final int maxAttempts;

    public OrderProcessor(SimulationOrderRepository orders,
                          OrderExecution execution,
                          Clock clock,
                          @Value("${orders.batch-size:10}") int batchSize,
                          @Value("${orders.processing-timeout:PT1M}") Duration processingTimeout,
                          @Value("${orders.max-attempts:3}") int maxAttempts) {
        this.orders = orders;
        this.execution = execution;
        this.clock = clock;
        this.batchSize = batchSize;
        this.processingTimeout = processingTimeout;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${orders.poll-interval-ms:200}")
    public void drainQueue() {
        for (UUID id : orders.findPendingIds(PageRequest.of(0, batchSize))) {
            if (execution.claim(id)) {
                assess(id);
            }
        }
    }

    @Scheduled(fixedDelayString = "${orders.reaper-interval-ms:5000}")
    public void reapStuckOrders() {
        Instant cutoff = Instant.now(clock).minus(processingTimeout);
        for (UUID id : orders.findStuckProcessingIds(cutoff, PageRequest.of(0, batchSize))) {
            log.warn("order {} stuck in PROCESSING since before {}, releasing", id, cutoff);
            execution.releaseStuck(id, cutoff, maxAttempts);
        }
    }

    private void assess(UUID id) {
        try {
            execution.assess(id);
        } catch (CapitalGainsException ex) {
            execution.fail(id, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("unexpected failure assessing order {}", id, ex);
            execution.fail(id, "internal error");
        }
    }
}
