package com.example.capitalgains.domain.portfolio;

import com.example.capitalgains.domain.Trade;

/**
 * An event that triggers a transition in the {@link Portfolio}. Sealed: the
 * {@code Portfolio} handles every case in an exhaustive {@code switch}, with no
 * {@code default}.
 */
public sealed interface TradeEvent {

    Trade trade();

    record Buy(Trade trade) implements TradeEvent {}

    record Sell(Trade trade) implements TradeEvent {}

    static TradeEvent of(Trade trade) {
        return trade.isBuy() ? new Buy(trade) : new Sell(trade);
    }
}
