package com.example.capitalgains.orders;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A request to assess a simulation, accepted for asynchronous processing. The
 * table doubles as the work queue: rows in {@link OrderStatus#PENDING} are the
 * backlog the worker drains.
 *
 * <p>State transitions are guarded here (fail-fast). The happy path is
 * {@code PENDING -> PROCESSING -> COMPLETED | FAILED}; a stuck {@code PROCESSING}
 * order can be released back to {@code PENDING} by the reaper.</p>
 */
@Entity
@Table(name = "simulation_order")
public class SimulationOrder {

    static final int FAILURE_REASON_MAX = 500;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** Boxed on purpose: {@code null} tells Spring Data the entity is new (persist, not merge). */
    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "simulation_id")
    private Long simulationId;

    @Column(name = "failure_reason", length = FAILURE_REASON_MAX)
    private String failureReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "simulation_order_trade", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "position")
    private List<RequestedTrade> trades = new ArrayList<>();

    protected SimulationOrder() {
        // required by JPA
    }

    private SimulationOrder(List<RequestedTrade> trades, Instant submittedAt) {
        this.id = UUID.randomUUID();
        this.status = OrderStatus.PENDING;
        this.trades = new ArrayList<>(trades);
        this.submittedAt = submittedAt;
    }

    public static SimulationOrder pending(List<RequestedTrade> trades, Instant submittedAt) {
        return new SimulationOrder(trades, submittedAt);
    }

    public void markProcessing(Instant now) {
        requireStatus(OrderStatus.PENDING);
        this.status = OrderStatus.PROCESSING;
        this.startedAt = now;
        this.attempts++;
    }

    public void markCompleted(Long simulationId, Instant now) {
        requireStatus(OrderStatus.PROCESSING);
        this.status = OrderStatus.COMPLETED;
        this.simulationId = simulationId;
        this.finishedAt = now;
    }

    public void markFailed(String reason, Instant now) {
        requireStatus(OrderStatus.PROCESSING);
        this.status = OrderStatus.FAILED;
        this.failureReason = clamp(reason);
        this.finishedAt = now;
    }

    /** Reaper path: a {@code PROCESSING} order whose worker never finished goes back to the queue. */
    public void releaseToPending() {
        requireStatus(OrderStatus.PROCESSING);
        this.status = OrderStatus.PENDING;
        this.startedAt = null;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "order %s is %s, expected %s".formatted(id, status, expected));
        }
    }

    private static String clamp(String reason) {
        if (reason == null || reason.length() <= FAILURE_REASON_MAX) {
            return reason;
        }
        return reason.substring(0, FAILURE_REASON_MAX);
    }

    public UUID getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getSimulationId() {
        return simulationId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public List<RequestedTrade> getTrades() {
        return List.copyOf(trades);
    }
}
