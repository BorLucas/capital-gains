package com.example.capitalgains.domain;

import com.example.capitalgains.domain.portfolio.Portfolio;
import com.example.capitalgains.domain.portfolio.TradeEvent;

import java.util.List;
import java.util.Objects;

/**
 * Calculates the tax owed for a sequence of stock trades, following the rules
 * of the "Capital Gains" challenge.
 *
 * <p>The state logic (average price, position and accumulated loss) and the
 * taxation rules live in the {@link Portfolio}. This class only walks the list,
 * turns each {@link Trade} into a {@link TradeEvent} and collects the result.
 * It holds no state of its own: a single instance can be reused.</p>
 */
public class TaxCalculator {

    public List<TradeResult> calculate(List<Trade> trades) {
        Objects.requireNonNull(trades, "trades");

        Portfolio portfolio = new Portfolio();
        return trades.stream()
                .map(trade -> {
                    Objects.requireNonNull(trade, "trade");
                    Tax tax = portfolio.apply(TradeEvent.of(trade));
                    return new TradeResult(trade, tax);
                })
                .toList();
    }
}
