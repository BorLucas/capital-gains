package com.example.capitalgains.application;

import com.example.capitalgains.orders.OrderNotFoundException;
import com.example.capitalgains.orders.OrderView;
import com.example.capitalgains.orders.SimulationOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reads the current state of an order for clients that are polling.
 */
@Service
public class OrderQueryService {

    private final SimulationOrderRepository repository;

    public OrderQueryService(SimulationOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public OrderView view(UUID id) {
        return repository.findById(id)
                .map(OrderView::of)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
