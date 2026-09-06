package com.example.capitalgains.history;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for the history listing: Spring Data fills these fields straight
 * from the query, without materializing the entity or loading the collection of
 * items.
 */
public interface SimulationSummary {

    Long getId();

    Instant getCreatedAt();

    int getTradeCount();

    BigDecimal getTotalTax();
}
