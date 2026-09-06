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
import java.util.stream.IntStream;

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
        return repository.save(order(trades, Instant.now(clock))).getId();
    }

    /** Places a basket of orders in a single transaction: all or nothing. */
    @Transactional
    public List<UUID> submitBatch(List<List<Trade>> simulations) {
        Instant base = Instant.now(clock);
        return IntStream.range(0, simulations.size())
                // spread the timestamps by a microsecond each so the worker keeps basket order
                .mapToObj(i -> repository.save(order(simulations.get(i), base.plusNanos(i * 1_000L))).getId())
                .toList();
    }

    private SimulationOrder order(List<Trade> trades, Instant submittedAt) {
        List<RequestedTrade> requested = trades.stream().map(RequestedTrade::of).toList();
        return SimulationOrder.pending(requested, submittedAt);
    }
}
