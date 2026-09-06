package com.example.capitalgains.application;

import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.orders.OrderStatus;
import com.example.capitalgains.orders.OrderView;
import com.example.capitalgains.orders.RequestedTrade;
import com.example.capitalgains.orders.SimulationOrder;
import com.example.capitalgains.orders.SimulationOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the worker by hand: the schedulers are pushed out to an hour so
 * {@code drainQueue} / {@code reapStuckOrders} only run when the test calls them.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "orders.poll-interval-ms=3600000",
        "orders.reaper-interval-ms=3600000",
        "orders.processing-timeout=PT1M",
        "orders.max-attempts=3"
})
class OrderProcessorTest {

    @Autowired SubmitOrderService submit;
    @Autowired OrderExecution execution;
    @Autowired OrderProcessor processor;
    @Autowired OrderQueryService query;
    @Autowired SimulationOrderRepository repository;

    private static final List<Trade> PROFITABLE = List.of(
            Trade.buy("10.00", 10000),
            Trade.sell("20.00", 5000));

    @Test
    void claimMakesTheProcessingStateAndStartedAtObservable() {
        UUID id = submit.submit(PROFITABLE);

        assertThat(execution.claim(id)).isTrue();

        OrderView view = query.view(id);
        assertThat(view.status()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(view.startedAt()).isNotNull();
        assertThat(view.finishedAt()).isNull();
    }

    @Test
    void drainQueueAssessesAPendingOrderToCompleted() {
        UUID id = submit.submit(PROFITABLE);

        processor.drainQueue();

        OrderView view = query.view(id);
        assertThat(view.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(view.simulationId()).isNotNull();
        assertThat(view.simulationUrl()).isEqualTo("/api/simulations/" + view.simulationId());
    }

    @Test
    void anOrderThatBreaksABusinessRuleEndsUpFailedWithoutBlockingTheQueue() {
        UUID bad = submit.submit(List.of(Trade.buy("10.00", 100), Trade.sell("20.00", 200)));
        UUID good = submit.submit(PROFITABLE);

        processor.drainQueue();

        assertThat(query.view(bad).status()).isEqualTo(OrderStatus.FAILED);
        assertThat(query.view(bad).failureReason()).isEqualTo("selling 200 shares, but only 100 are held");
        assertThat(query.view(good).status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void theReaperReleasesAnOrderStuckInProcessing() {
        UUID id = persistStuckOrder(Instant.now().minus(2, ChronoUnit.HOURS));

        processor.reapStuckOrders();

        assertThat(query.view(id).status()).isEqualTo(OrderStatus.PENDING);
        assertThat(query.view(id).startedAt()).isNull();

        processor.drainQueue();
        assertThat(query.view(id).status()).isEqualTo(OrderStatus.COMPLETED);
    }

    UUID persistStuckOrder(Instant startedAt) {
        List<RequestedTrade> trades = PROFITABLE.stream().map(RequestedTrade::of).toList();
        SimulationOrder order = SimulationOrder.pending(trades, startedAt.minusSeconds(1));
        order.markProcessing(startedAt);
        return repository.save(order).getId();
    }
}
