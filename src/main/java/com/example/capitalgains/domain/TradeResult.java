package com.example.capitalgains.domain;

import java.util.Objects;

/**
 * A trade and the tax it generated. Spares the upper layers from having to
 * "zip" two parallel lists (trades x taxes) by index.
 */
public record TradeResult(Trade trade, Tax tax) {

    public TradeResult {
        Objects.requireNonNull(trade, "trade");
        Objects.requireNonNull(tax, "tax");
    }
}
