package com.example.capitalgains.orders;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model returned to clients polling an order. Built inside the read
 * transaction so the web edge never touches the entity.
 *
 * <p>When {@link #status()} is {@code COMPLETED}, {@link #simulationId()} and
 * {@link #simulationUrl()} point at the stored result (the "fill"). When it is
 * {@code FAILED}, {@link #failureReason()} explains why.</p>
 */
public record OrderView(
        UUID id,
        OrderStatus status,
        Instant submittedAt,
        Instant startedAt,
        Instant finishedAt,
        Long simulationId,
        String simulationUrl,
        String failureReason
) {

    public static OrderView of(SimulationOrder order) {
        Long simulationId = order.getSimulationId();
        String simulationUrl = simulationId == null ? null : "/api/simulations/" + simulationId;
        return new OrderView(
                order.getId(),
                order.getStatus(),
                order.getSubmittedAt(),
                order.getStartedAt(),
                order.getFinishedAt(),
                simulationId,
                simulationUrl,
                order.getFailureReason());
    }
}
