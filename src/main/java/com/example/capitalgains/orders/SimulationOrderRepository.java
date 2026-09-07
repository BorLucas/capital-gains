package com.example.capitalgains.orders;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SimulationOrderRepository extends JpaRepository<SimulationOrder, UUID> {

    /**
     * The backlog: ids of the oldest pending orders, capped by the page size.
     * Selects only the id so the poll does not drag in each order's trade
     * collection every tick.
     */
    @Query("""
            select o.id from SimulationOrder o
            where o.status = com.example.capitalgains.orders.OrderStatus.PENDING
            order by o.submittedAt asc, o.id asc
            """)
    List<UUID> findPendingIds(Pageable page);

    /**
     * Ids of orders that have been {@code PROCESSING} since before {@code cutoff}
     * — their worker never finished (e.g. the process died mid-assessment).
     */
    @Query("""
            select o.id from SimulationOrder o
            where o.status = com.example.capitalgains.orders.OrderStatus.PROCESSING
              and o.startedAt < :cutoff
            order by o.startedAt asc
            """)
    List<UUID> findStuckProcessingIds(@Param("cutoff") Instant cutoff, Pageable page);
}
