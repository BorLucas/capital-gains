package com.example.capitalgains.application;

import com.example.capitalgains.domain.TradeBreakdown;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One trade as the broker UI shows it, with the full tax walk-through.
 * {@code id} and {@code executedAt} are null on a quote (nothing was executed).
 */
public record TradeView(
        Long id,
        String ticker,
        String operation,
        BigDecimal unitCost,
        long quantity,
        BigDecimal totalValue,
        BigDecimal averagePriceBefore,
        BigDecimal result,
        boolean exempt,
        BigDecimal accumulatedLossBefore,
        BigDecimal accumulatedLossAfter,
        BigDecimal tax,
        BigDecimal netResult,
        long positionAfter,
        BigDecimal averagePriceAfter,
        Instant executedAt) {

    static TradeView of(Long id, String ticker, TradeBreakdown b, Instant executedAt) {
        return new TradeView(
                id,
                ticker,
                b.trade().type().code(),
                b.trade().unitCost().amount(),
                b.trade().quantity(),
                b.trade().totalValue().amount(),
                b.averagePriceBefore().amount(),
                b.result().amount(),
                b.exempt(),
                b.accumulatedLossBefore().amount(),
                b.accumulatedLossAfter().amount(),
                b.tax().toBigDecimal(),
                b.netResult().amount(),
                b.positionAfter(),
                b.averagePriceAfter().amount(),
                executedAt);
    }
}
