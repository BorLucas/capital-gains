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
 * @param fee      brokerage fee charged for the whole order (&gt;= 0); the
 *                 challenge format has none, so it defaults to zero
 * @param rules    tax regime in force when the trade was made (only a sell uses
 *                 it); defaults to the challenge's {@link TaxRules#CHALLENGE}
 */
public record Trade(TradeType type, Money unitCost, long quantity, Money fee, TaxRules rules) {

    public Trade {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(unitCost, "unitCost");
        Objects.requireNonNull(fee, "fee");
        Objects.requireNonNull(rules, "rules");
        if (fee.isNegative()) {
            throw new InvalidTradeException("fee cannot be negative");
        }
        if (unitCost.isNegative()) {
            throw new InvalidTradeException("unit-cost cannot be negative");
        }
        if (quantity <= 0) {
            throw new InvalidTradeException("quantity must be greater than zero");
        }
    }

    /** A trade in the challenge format: no brokerage fee, the challenge's tax rules. */
    public Trade(TradeType type, Money unitCost, long quantity) {
        this(type, unitCost, quantity, Money.ZERO, TaxRules.CHALLENGE);
    }

    public Trade withFee(Money fee) {
        return new Trade(type, unitCost, quantity, fee, rules);
    }

    public Trade withRules(TaxRules rules) {
        return new Trade(type, unitCost, quantity, fee, rules);
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

    /**
     * Cash that actually moves, fee included: a buy costs {@code totalValue + fee},
     * a sell yields {@code totalValue - fee}.
     */
    public Money netValue() {
        return isBuy() ? totalValue().plus(fee) : totalValue().minus(fee);
    }

    public boolean isBuy() {
        return type == TradeType.BUY;
    }

    public boolean isSell() {
        return type == TradeType.SELL;
    }
}
