package com.example.capitalgains.application;

import com.example.capitalgains.domain.Trade;
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
 * Accepts simulation requests for asynchronous processing. It only enqueues:
 * the trades were already validated at the edge, and the actual assessment
 * happens later in the {@link OrderProcessor}.
 */
@Service
public class SubmitOrderService {

    private final SimulationOrderRepository repository;
    private final Clock clock;

    public SubmitOrderService(SimulationOrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public UUID submit(List<Trade> trades) {
        return repository.save(newOrder(trades)).getId();
    }

    /** Places a basket of orders in a single transaction: all or nothing. */
    @Transactional
    public List<UUID> submitBatch(List<List<Trade>> simulations) {
        Instant now = Instant.now(clock);
        return simulations.stream()
                .map(trades -> repository.save(order(trades, now)).getId())
                .toList();
    }

    private SimulationOrder newOrder(List<Trade> trades) {
        return order(trades, Instant.now(clock));
    }

    private SimulationOrder order(List<Trade> trades, Instant now) {
        List<RequestedTrade> requested = trades.stream().map(RequestedTrade::of).toList();
        return SimulationOrder.pending(requested, now);
    }
}
