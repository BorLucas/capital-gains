package com.example.capitalgains.orders;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationOrderRepository extends JpaRepository<SimulationOrder, UUID> {

    /** The backlog: oldest pending orders first, capped by the page size. */
    List<SimulationOrder> findByStatusOrderBySubmittedAtAsc(OrderStatus status, Pageable page);
}
