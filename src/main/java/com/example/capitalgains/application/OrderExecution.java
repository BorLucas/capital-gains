package com.example.capitalgains.application;

import com.example.capitalgains.domain.TaxCalculator;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeResult;
import com.example.capitalgains.domain.error.CapitalGainsException;
import com.example.capitalgains.history.Simulation;
import com.example.capitalgains.orders.OrderStatus;
import com.example.capitalgains.orders.RequestedTrade;
import com.example.capitalgains.orders.SimulationOrder;
import com.example.capitalgains.orders.SimulationOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Assesses a single order: mark it {@code PROCESSING}, run the calculator,
 * store the resulting {@link Simulation}, and mark the order
 * {@code COMPLETED}/{@code FAILED}. All in one transaction, so an order fills
 * or is rejected atomically.
 *
 * <p>Separate bean from {@link OrderProcessor} on purpose: the {@code @Transactional}
 * boundary only applies when the method is called through the Spring proxy, not
 * from another method of the same bean.</p>
 */
@Service
public class OrderExecution {

    private static final Logger log = LoggerFactory.getLogger(OrderExecution.class);
    private static final TaxCalculator CALCULATOR = new TaxCalculator();

    private final SimulationOrderRepository orders;
    private final HistoryService history;
    private final Clock clock;

    public OrderExecution(SimulationOrderRepository orders, HistoryService history, Clock clock) {
        this.orders = orders;
        this.history = history;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID id) {
        SimulationOrder order = orders.findById(id).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return; // already claimed or gone: at-least-once delivery, idempotent skip
        }

        order.markProcessing(Instant.now(clock));
        try {
            List<Trade> trades = order.getTrades().stream().map(RequestedTrade::toDomain).toList();
            List<TradeResult> results = CALCULATOR.calculate(trades);
            Simulation simulation = history.record(results);
            order.markCompleted(simulation.getId(), Instant.now(clock));
        } catch (CapitalGainsException ex) {
            order.markFailed(ex.getMessage(), Instant.now(clock));
        } catch (RuntimeException ex) {
            log.error("unexpected failure assessing order {}", id, ex);
            order.markFailed("internal error", Instant.now(clock));
        }
    }
}
