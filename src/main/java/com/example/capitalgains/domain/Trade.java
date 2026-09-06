package com.example.capitalgains.domain;

import com.example.capitalgains.domain.error.InvalidTradeException;

import java.util.Objects;

/**
 * A stock buy or sell trade. Invariants are checked in the compact constructor
 * (fail-fast): no invalid {@code Trade} ever comes to exist.
 *
 * @param type     buy or sell
 * @param unitCost per-share price of the trade (&gt;= 0)
 * @param quantity number of shares traded (&gt; 0)
 */
public record Trade(TradeType type, Money unitCost, long quantity) {

    public Trade {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(unitCost, "unitCost");
        if (unitCost.isNegative()) {
            throw new InvalidTradeException("unit-cost cannot be negative");
        }
        if (quantity <= 0) {
            throw new InvalidTradeException("quantity must be greater than zero");
        }
    }

    public static Trade buy(String unitCost, long quantity) {
        return new Trade(TradeType.BUY, Money.of(unitCost), quantity);
    }

    public static Trade sell(String unitCost, long quantity) {
        return new Trade(TradeType.SELL, Money.of(unitCost), quantity);
    }

    /** Total financial value of the trade: unit cost x quantity. */
    public Money totalValue() {
        return unitCost.times(quantity);
    }

    public boolean isBuy() {
        return type == TradeType.BUY;
    }

    public boolean isSell() {
        return type == TradeType.SELL;
    }
}
