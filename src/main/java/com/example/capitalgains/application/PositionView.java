package com.example.capitalgains.application;

import java.math.BigDecimal;

/**
 * A user's position in one ticker. A fully sold position stays listed
 * ({@code quantity == 0}) because its realized result and tax still count.
 */
public record PositionView(
        String ticker,
        long quantity,
        BigDecimal averagePrice,
        BigDecimal investedValue,
        BigDecimal accumulatedLoss,
        BigDecimal realizedResult,
        BigDecimal taxPaid) {
}
