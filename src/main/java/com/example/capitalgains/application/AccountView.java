package com.example.capitalgains.application;

import java.math.BigDecimal;
import java.util.List;

/** Everything the broker dashboard shows: totals, positions, and the trade log (newest first). */
public record AccountView(
        BigDecimal investedValue,
        BigDecimal realizedResult,
        BigDecimal taxPaid,
        BigDecimal netResult,
        List<PositionView> positions,
        List<TradeView> trades) {
}
