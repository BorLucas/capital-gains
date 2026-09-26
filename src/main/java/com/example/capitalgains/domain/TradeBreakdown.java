package com.example.capitalgains.domain;

import java.util.Objects;

/**
 * A trade and everything the {@link com.example.capitalgains.domain.portfolio.Portfolio}
 * did with it: the average price it was measured against, the gross result,
 * whether the exemption applied, the tax, and the position left afterwards.
 * Lets a user see <i>why</i> a sell paid what it paid, not just the tax.
 *
 * @param averagePriceBefore   average price of the position before the trade
 * @param result               gross profit (&gt; 0) or loss (&lt; 0) of a sell; zero for a buy
 * @param exempt               a sell within the exemption limit (always false for a buy)
 * @param accumulatedLossBefore loss carried forward before the trade
 * @param accumulatedLossAfter  loss carried forward after the trade
 */
public record TradeBreakdown(
        Trade trade,
        Money averagePriceBefore,
        Money result,
        boolean exempt,
        Tax tax,
        long positionAfter,
        Money averagePriceAfter,
        Money accumulatedLossBefore,
        Money accumulatedLossAfter) {

    public TradeBreakdown {
        Objects.requireNonNull(trade, "trade");
        Objects.requireNonNull(averagePriceBefore, "averagePriceBefore");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(tax, "tax");
        Objects.requireNonNull(averagePriceAfter, "averagePriceAfter");
        Objects.requireNonNull(accumulatedLossBefore, "accumulatedLossBefore");
        Objects.requireNonNull(accumulatedLossAfter, "accumulatedLossAfter");
    }

    /** What the sell left in the pocket: gross result minus tax. */
    public Money netResult() {
        return result.minus(tax.amount());
    }
}
