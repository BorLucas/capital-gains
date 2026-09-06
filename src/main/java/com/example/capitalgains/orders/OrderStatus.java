package com.example.capitalgains.orders;

/**
 * Lifecycle of a {@link SimulationOrder}, mirroring how a broker order moves
 * through the book.
 *
 * <pre>
 *   PENDING ──► PROCESSING ──► COMPLETED   (tax assessed, simulation stored)
 *                          └─► FAILED      (rejected by a business rule)
 * </pre>
 */
public enum OrderStatus {

    /** Accepted and sitting in the queue, not yet picked up by the worker. */
    PENDING,

    /** Claimed by the worker; being assessed. */
    PROCESSING,

    /** Assessed successfully; the resulting simulation is available. */
    COMPLETED,

    /** Rejected while being assessed (e.g. selling more than the position). */
    FAILED
}
