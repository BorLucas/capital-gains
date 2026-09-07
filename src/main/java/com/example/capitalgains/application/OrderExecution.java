package com.example.capitalgains.application;

import com.example.capitalgains.domain.TaxCalculator;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeResult;
import com.example.capitalgains.history.Simulation;
import com.example.capitalgains.orders.OrderStatus;
import com.example.capitalgains.orders.RequestedTrade;
import com.example.capitalgains.orders.SimulationOrder;
import com.example.capitalgains.orders.SimulationOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The steps of assessing one order, each in its <b>own</b> transaction so the
 * order's status is committed — and therefore observable to a polling client —
 * between steps:
 *
 * <ol>
 *   <li>{@link #claim(UUID)} — {@code PENDING -> PROCESSING};</li>
 *   <li>{@link #assess(UUID)} — run the calculator, store the {@link Simulation},
 *       {@code PROCESSING -> COMPLETED}; exceptions propagate so this transaction
 *       rolls back cleanly;</li>
 *   <li>{@link #fail(UUID, String)} — {@code PROCESSING -> FAILED}, invoked by the
 *       caller when {@code assess} threw, in a fresh transaction;</li>
 *   <li>{@link #releaseStuck(UUID, java.time.Instant, int)} — reaper path for an
 *       order whose worker never finished.</li>
 * </ol>
 *
 * <p>Every method is called from {@link OrderProcessor} (a different bean) so the
 * {@code @Transactional} proxy actually applies.</p>
 */
@Service
public class OrderExecution {

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
    public boolean claim(UUID id) {
        SimulationOrder order = orders.findById(id).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return false; // already claimed or gone: idempotent skip
        }
        order.markProcessing(Instant.now(clock));
        return true;
    }

    @Transactional
    public void assess(UUID id) {
        SimulationOrder order = orders.findById(id).orElseThrow();
        if (order.getStatus() != OrderStatus.PROCESSING) {
            return;
        }
        List<Trade> trades = order.getTrades().stream().map(RequestedTrade::toDomain).toList();
        List<TradeResult> results = CALCULATOR.calculate(trades);
        Simulation simulation = history.record(results);
        order.markCompleted(simulation.getId(), Instant.now(clock));
    }

    @Transactional
    public void fail(UUID id, String reason) {
        orders.findById(id).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PROCESSING) {
                order.markFailed(reason, Instant.now(clock));
            }
        });
    }

    @Transactional
    public void releaseStuck(UUID id, Instant cutoff, int maxAttempts) {
        orders.findById(id).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.PROCESSING
                    || order.getStartedAt() == null
                    || !order.getStartedAt().isBefore(cutoff)) {
                return;
            }
            if (order.getAttempts() >= maxAttempts) {
                order.markFailed("gave up after " + order.getAttempts() + " attempts", Instant.now(clock));
            } else {
                order.releaseToPending();
            }
        });
    }
}
